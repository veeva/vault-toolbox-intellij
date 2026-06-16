package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.veeva.vault.toolbox.intellij.credentials.BasicAuth;
import com.veeva.vault.toolbox.intellij.credentials.VaultCredentialManager;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.settings.AppSettings;
import com.veeva.vault.toolbox.intellij.settings.SavedCredential;
import com.veeva.vault.toolbox.intellij.settings.Vault;
import com.veeva.vault.vapil.api.client.VaultClient;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Dialog for Vault authentication.
 */
public class VaultAuthDialog extends DialogWrapper {

    private final Project project;
    private final ToolboxProject toolboxProject;
    private final String dns;

    private LoginPanel loginPanel;
    private VaultClient authenticatedClient;

    /**
     * Authenticates against a known vault DNS.
     *
     * @param project        the IDE project
     * @param toolboxProject the current toolbox project
     * @param dns            the Vault DNS
     */
    public VaultAuthDialog(Project project, ToolboxProject toolboxProject, String dns) {
        super(project, false);
        this.project = project;
        this.toolboxProject = toolboxProject;
        this.dns = dns;
        setTitle("Authenticate — " + dns);
        setOKButtonText("Sign In");
        init();
    }

    /**
     * Prompts for sign-in to a new environment, requiring the DNS to be entered.
     *
     * @param project        the IDE project
     * @param toolboxProject the current toolbox project
     */
    public VaultAuthDialog(Project project, ToolboxProject toolboxProject) {
        super(project, false);
        this.project = project;
        this.toolboxProject = toolboxProject;
        this.dns = null;
        setTitle("Sign In to New Environment");
        setOKButtonText("Sign In");
        init();
    }

    /**
     * Creates the center panel for the dialog.
     *
     * @return the center panel
     */
    @Override
    protected @Nullable JComponent createCenterPanel() {
        loginPanel = new LoginPanel(toolboxProject, false);
        if (dns != null) {
            loginPanel.vaultDnsField.setText(dns);
            loginPanel.vaultDnsField.setEditable(false);
        }
        return loginPanel;
    }

    /**
     * Gets the preferred focused component.
     *
     * @return the focused component
     */
    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        if (loginPanel == null) return null;
        return dns != null ? loginPanel.usernameField : loginPanel.vaultDnsField;
    }

    /**
     * Handles the OK action, validating and authenticating.
     */
    @Override
    protected void doOKAction() {
        setErrorText(null);

        var validation = loginPanel.validateForm();
        if (validation != null) {
            setErrorText(validation.message, validation.component);
            return;
        }

        LoginPanel.LoginCredentials creds = loginPanel.extractCredentials();
        if (!creds.isValid) return;

        String effectiveDns = dns != null ? dns : creds.vaultDns.trim();

        final VaultClient[] result = {null};
        final String[] errorMsg = {null};

        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                result[0] = buildVaultClient(effectiveDns, creds.isBasicAuth,
                        creds.username  != null ? creds.username  : "",
                        creds.password  != null ? creds.password  : "",
                        creds.sessionId != null ? creds.sessionId : "");
            } catch (Exception e) {
                errorMsg[0] = e.getMessage();
            }
        }, "Authenticating " + effectiveDns + "…", false, project);

        if (errorMsg[0] != null) { setErrorText("Connection failed: " + errorMsg[0]); return; }
        if (result[0] == null || !result[0].hasSessionId()) {
            setErrorText("Authentication failed. Check your credentials."); return;
        }

        LoginPanel.PendingCredentialSave pending = LoginPanel.checkCredentialForSave(
                effectiveDns, creds.isBasicAuth,
                creds.username != null ? creds.username : "",
                creds.password != null ? creds.password : "",
                creds.sessionId != null ? creds.sessionId : "",
                null // we don't have loadedCredential from VaultAuthDialog easily, but it's fine, checkCredentialForSave handles null
        );
        if (pending != null) {
            toolboxProject.promptToSaveCredential(pending);
        }

        authenticatedClient = result[0];
        super.doOKAction();
    }

    /**
     * Gets the authenticated client.
     *
     * @return the authenticated client
     */
    public @Nullable VaultClient getAuthenticatedClient() { return authenticatedClient; }

    /**
     * Gets the authenticated DNS.
     *
     * @return the authenticated DNS
     */
    public @Nullable String getAuthenticatedDns() {
        if (dns != null) return dns;
        return loginPanel != null ? loginPanel.vaultDnsField.getText().trim() : null;
    }

    /**
     * Tries to auto-login based on saved credentials.
     *
     * @param dns the Vault DNS
     * @return a valid VaultClient if successful, otherwise null
     */
    static @Nullable VaultClient tryAutoLogin(String dns) {
        SavedCredential cred = AppSettings.findCredentialByDns(dns);
        if (cred == null) return null;
        if (cred.authenticationType == Vault.AuthenticationType.BASIC) {
            BasicAuth saved = VaultCredentialManager.getUsernamePasswordById(cred.id);
            if (saved != null && saved.getUsername() != null) {
                VaultClient c = buildVaultClient(dns, true,
                        saved.getUsername(), saved.getPassword() != null ? saved.getPassword() : "", "");
                if (c != null && c.hasSessionId()) return c;
            }
        } else {
            String session = VaultCredentialManager.getSessionIdById(cred.id);
            if (session != null && !session.isBlank()) {
                VaultClient c = buildVaultClient(dns, false, "", "", session);
                if (c != null && c.hasSessionId()) return c;
            }
        }
        return null;
    }

    /**
     * Builds and authenticates a VaultClient.
     *
     * @param dns       the Vault DNS
     * @param isBasic   true for basic auth, false for session ID
     * @param username  the username
     * @param password  the password
     * @param sessionId the session ID
     * @return the authenticated VaultClient
     */
    static @Nullable VaultClient buildVaultClient(String dns, boolean isBasic,
            String username, String password, String sessionId) {
        try {
            AppSettings.AppState state = Objects.requireNonNull(AppSettings.getInstance().getState());
            boolean allowAllCerts = state.allowAllCertificates;
            int timeout = state.connectionTimeout > 0 ? state.connectionTimeout : 15;

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<VaultClient> future = executor.submit(() -> {
                if (isBasic) {
                    var builder = VaultClient.newClientBuilder(VaultClient.AuthenticationType.BASIC)
                            .withVaultClientId(ToolboxProject.CLIENT_ID)
                            .withVaultDNS(dns.trim())
                            .withVaultUsername(username)
                            .withVaultPassword(password);
                    if (allowAllCerts) builder.withAllowAllCertificates(true);
                    return builder.build();
                } else {
                    var builder = VaultClient.newClientBuilder(VaultClient.AuthenticationType.SESSION_ID)
                            .withVaultClientId(ToolboxProject.CLIENT_ID)
                            .withVaultDNS(dns.trim())
                            .withVaultSessionId(sessionId);
                    if (allowAllCerts) builder.withAllowAllCertificates(true);
                    return builder.build();
                }
            });
            try {
                return future.get(timeout, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                return null;
            } catch (ExecutionException e) {
                return null;
            } finally {
                executor.shutdownNow();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
