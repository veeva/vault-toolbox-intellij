package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.vapil.api.model.response.ValidatePackageResponse;
import com.veeva.vault.vapil.api.request.ConfigurationMigrationRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateVpkTask extends ToolboxModalTask {
    private static final Logger logger = LoggerFactory.getLogger(ValidateVpkTask.class);
    private final VirtualFile virtualFile;
    private ValidatePackageResponse validateResponse;

    public ValidateVpkTask(@Nullable Project project, @NotNull VirtualFile virtualFile) {
        super(project, "Validating Package", true);
        this.virtualFile = virtualFile;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            super.run(indicator);

            if (!toolboxProject.isConnected()) {
                if (!toolboxProject.connectWithDialog()) {
                    return;
                }
            }

            indicator.setText("Uploading package for validation...");
            validateResponse = validatePackage(virtualFile.getPath());

            if (toolboxProject.handleSessionExpiration(validateResponse)) {
                return;
            }

        } catch (Exception e) {
            if (toolboxProject.handleSessionExpiration(e)) {
                return;
            }
            logger.error("Error validating package", e);
        }
    }

    /**
     * Validates a package using the VAPIL ConfigurationMigrationRequest.
     * Reference: https://developer.veevavault.com/api/26.1/#validate-package
     *
     * @param filePath The absolute path to the package file to validate
     * @return The ValidatePackageResponse from the Vault API
     */
    public ValidatePackageResponse validatePackage(String filePath) {
        return toolboxProject.getVaultClient()
                .newRequest(ConfigurationMigrationRequest.class)
                .setInputPath(filePath)
                .validatePackage();
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (toolboxProject != null && validateResponse != null) {
            Message message = toolboxProject.newMessage();

            if (validateResponse.isFailure()) {
                message.setTitle("Validation Error");
                String errorMsg = validateResponse.getResponseMessage();

                if (validateResponse.getResponseDetails() != null) {
                    String packageError = validateResponse.getResponseDetails().getPackageError();
                    if (packageError != null && !packageError.isEmpty()) {
                        errorMsg = packageError;
                    }
                }

                if ((errorMsg == null || errorMsg.isEmpty()) && validateResponse.getErrors() != null && !validateResponse.getErrors().isEmpty()) {
                    errorMsg = validateResponse.getErrors().get(0).getMessage();
                }

                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = validateResponse.getResponseStatus();
                }

                message.append("Vault rejected package validation: " + errorMsg);
                message.showError();
            } else {
                message.setTitle("Validation Successful");
                message.append("Package validated successfully.");
                if (validateResponse.getResponseDetails() != null) {
                    String pkgName = validateResponse.getResponseDetails().getPackageName();
                    String author = validateResponse.getResponseDetails().getAuthor();
                    String status = validateResponse.getResponseDetails().getPackageStatus();

                    if (pkgName != null && !pkgName.isEmpty()) {
                        message.append("\nPackage: " + pkgName);
                    }
                    if (author != null && !author.isEmpty()) {
                        message.append("\nAuthor: " + author);
                    }
                    if (status != null && !status.isEmpty()) {
                        message.append("\nStatus: " + status);
                    }
                }
                message.showInformation();
            }
        }
    }
}