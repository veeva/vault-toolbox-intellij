package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.veeva.vault.toolbox.intellij.credentials.VaultCredentialManager;
import com.veeva.vault.toolbox.intellij.settings.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveCredentialsTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(SaveCredentialsTask.class);
    private String vaultDNS = null;
    private String username = null;
    private String password = null;
    private String sessionId = null;
    private final Vault.AuthenticationType authenticationType;

    public SaveCredentialsTask(@Nullable Project project,
                               @NotNull String vaultDNS,
                               @NotNull String username,
                               @Nullable String password) { // Changed to @Nullable
        super(project, "Saving Vault Credentials");
        this.vaultDNS = vaultDNS;
        this.username = username;
        this.password = password;
        this.authenticationType = Vault.AuthenticationType.BASIC;
    }

    public SaveCredentialsTask(@Nullable Project project,
                               @NotNull String vaultDNS,
                               @Nullable String sessionId) { // Changed to @Nullable
        super(project, "Saving Vault Credentials");
        this.vaultDNS = vaultDNS;
        this.sessionId = sessionId;
        this.authenticationType = Vault.AuthenticationType.SESSION_ID;
    }

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

    @Override
    public void onSuccess() {
        super.onSuccess();
        try {
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}