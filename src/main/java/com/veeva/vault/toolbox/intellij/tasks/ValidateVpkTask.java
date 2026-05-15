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

/**
 * Uploads a VPK to the connected vault for validation and reports the result in a
 * UI dialog. The vault is contacted only after a connection is established (or the
 * user cancels the connection dialog).
 */
public class ValidateVpkTask extends ToolboxModalTask {
    private static final Logger logger = LoggerFactory.getLogger(ValidateVpkTask.class);

    private final VirtualFile virtualFile;
    private ValidatePackageResponse validateResponse;

    /**
     * @param project     the IntelliJ project, may be {@code null}
     * @param virtualFile the VPK file to validate
     */
    public ValidateVpkTask(@Nullable Project project, @NotNull VirtualFile virtualFile) {
        super(project, "Validating Package", true);
        this.virtualFile = virtualFile;
    }

    /**
     * Uploads the VPK for validation in a background thread.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            if (!toolboxProject.isConnected() && !toolboxProject.connectWithDialog()) {
                return;
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
     * Calls the Vault Configuration Migration API to validate the package at
     * {@code filePath}.
     * <p>
     * See <a href="https://developer.veevavault.com/api/26.1/#validate-package">
     * Validate Package</a>.
     *
     * @param filePath absolute path to the package file to validate
     * @return the API response describing the validation outcome
     */
    public ValidatePackageResponse validatePackage(String filePath) {
        return toolboxProject.getVaultClient()
                .newRequest(ConfigurationMigrationRequest.class)
                .setInputPath(filePath)
                .validatePackage();
    }

    /**
     * Displays the validation results (success or error with details) in a UI dialog on the EDT.
     */
    @Override
    public void onSuccess() {
        super.onSuccess();
        if (toolboxProject == null || validateResponse == null) {
            return;
        }
        Message message = toolboxProject.newMessage();
        if (validateResponse.isFailure()) {
            message.setTitle("Validation Error");
            message.append("Vault rejected package validation: " + resolveErrorMessage());
            message.showError();
            return;
        }
        message.setTitle("Validation Successful");
        message.append("Package validated successfully.");
        appendDetails(message);
        message.showInformation();
    }

    /**
     * Extracts the most relevant error message from the validation response.
     *
     * @return the resolved error message
     */
    private String resolveErrorMessage() {
        String errorMsg = validateResponse.getResponseMessage();

        if (validateResponse.getResponseDetails() != null) {
            String packageError = validateResponse.getResponseDetails().getPackageError();
            if (packageError != null && !packageError.isEmpty()) {
                errorMsg = packageError;
            }
        }

        if ((errorMsg == null || errorMsg.isEmpty())
                && validateResponse.getErrors() != null
                && !validateResponse.getErrors().isEmpty()) {
            errorMsg = validateResponse.getErrors().get(0).getMessage();
        }

        if (errorMsg == null || errorMsg.isEmpty()) {
            errorMsg = validateResponse.getResponseStatus();
        }
        return errorMsg;
    }

    /**
     * Appends package details (name, author, status) to the given UI message.
     *
     * @param message the message to append details to
     */
    private void appendDetails(Message message) {
        if (validateResponse.getResponseDetails() == null) {
            return;
        }
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
}
