package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.veeva.vault.toolbox.intellij.credentials.VaultCredentialManager;
import com.veeva.vault.toolbox.intellij.settings.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists vault credentials to the IntelliJ {@link com.intellij.ide.passwordSafe.PasswordSafe}
 * via {@link VaultCredentialManager}. Supports either basic (username/password) or
 * session-id based authentication, selected by the constructor that is invoked.
 */
public class SaveCredentialsTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(SaveCredentialsTask.class);

    private final String vaultDNS;
    private final String username;
    private final String password;
    private final String sessionId;
    private final Vault.AuthenticationType authenticationType;

    /**
     * Creates a task that saves basic-auth credentials.
     *
     * @param project  the IntelliJ project, may be {@code null}
     * @param vaultDNS the vault DNS the credentials apply to
     * @param username the username
     * @param password the password; may be {@code null} to clear an existing entry
     */
    public SaveCredentialsTask(@Nullable Project project,
                               @NotNull String vaultDNS,
                               @NotNull String username,
                               @Nullable String password) {
        super(project, "Saving Vault Credentials");
        this.vaultDNS = vaultDNS;
        this.username = username;
        this.password = password;
        this.sessionId = null;
        this.authenticationType = Vault.AuthenticationType.BASIC;
    }

    /**
     * Creates a task that saves a pre-existing session id.
     *
     * @param project   the IntelliJ project, may be {@code null}
     * @param vaultDNS  the vault DNS the session applies to
     * @param sessionId the session id; may be {@code null} to clear an existing entry
     */
    public SaveCredentialsTask(@Nullable Project project,
                               @NotNull String vaultDNS,
                               @Nullable String sessionId) {
        super(project, "Saving Vault Credentials");
        this.vaultDNS = vaultDNS;
        this.username = null;
        this.password = null;
        this.sessionId = sessionId;
        this.authenticationType = Vault.AuthenticationType.SESSION_ID;
    }

    /**
     * Persists the vault credentials to the IntelliJ password safe.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            if (authenticationType == Vault.AuthenticationType.BASIC) {
                VaultCredentialManager.setUsernamePassword(vaultDNS, username, password);
            }
            else if (authenticationType == Vault.AuthenticationType.SESSION_ID) {
                VaultCredentialManager.setSessionId(vaultDNS, sessionId);
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
