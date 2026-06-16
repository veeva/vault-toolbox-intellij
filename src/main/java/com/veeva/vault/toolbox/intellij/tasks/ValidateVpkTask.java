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
                validateResponse = null;
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

            if (validateResponse.getResponseDetails().getPackageSteps() != null) {
                StringBuilder stepsError = new StringBuilder();
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    for (var step : validateResponse.getResponseDetails().getPackageSteps()) {
                        String stepJson = mapper.writeValueAsString(step);
                        com.fasterxml.jackson.databind.JsonNode stepNode = mapper.readTree(stepJson);

                        String stepName = stepNode.path("step_name").asText("Step");
                        String stepErrorStr = stepNode.path("step_error").asText();

                        java.util.List<String> innerErrors = new java.util.ArrayList<>();
                        findErrorMessages(stepNode.path("step_error_details"), innerErrors);

                        if ((stepErrorStr != null && !stepErrorStr.isEmpty() && !"null".equals(stepErrorStr)) || !innerErrors.isEmpty()) {
                            if (stepsError.length() > 0) {
                                stepsError.append("\n");
                            }
                            stepsError.append(stepName).append(":");
                            if (stepErrorStr != null && !stepErrorStr.isEmpty() && !"null".equals(stepErrorStr)) {
                                stepsError.append(" ").append(stepErrorStr);
                            }
                            for (String err : innerErrors) {
                                if (err != null && !err.isEmpty() && !err.equals(stepErrorStr) && !"null".equals(err)) {
                                    stepsError.append("\n  - ").append(err);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error processing package steps JSON", e);
                }
                if (stepsError.length() > 0) {
                    errorMsg = errorMsg + "\n\nDetails:\n" + stepsError.toString();
                }
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
     * Recursively traverses a JSON node to extract relevant error messages.
     *
     * @param node the JSON node to traverse
     * @param errors the list to populate with extracted error messages
     */
    private void findErrorMessages(com.fasterxml.jackson.databind.JsonNode node, java.util.List<String> errors) {
        if (node.isObject()) {
            java.util.Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                com.fasterxml.jackson.databind.JsonNode value = node.get(key);
                
                if (key.equals("error_message") || key.equals("message") || key.equals("validation_message") || key.equals("error")) {
                    errors.add(value.asText());
                } else if (key.equals("validation_errors") || key.equals("package_errors")) {
                    if (value.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode child : value) {
                            if (child.isTextual()) {
                                errors.add(child.asText());
                            } else {
                                findErrorMessages(child, errors);
                            }
                        }
                    } else if (value.isTextual()) {
                        errors.add(value.asText());
                    } else {
                        findErrorMessages(value, errors);
                    }
                } else if (!key.equals("step_error")) {
                    findErrorMessages(value, errors);
                }
            }
        } else if (node.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode child : node) {
                findErrorMessages(child, errors);
            }
        }
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
