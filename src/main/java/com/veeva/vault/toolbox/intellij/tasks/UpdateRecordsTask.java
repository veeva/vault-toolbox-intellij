package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.response.ObjectRecordBulkResponse;
import com.veeva.vault.vapil.api.model.response.ObjectRecordResponse;
import com.veeva.vault.vapil.api.request.VaultRequest;
import com.veeva.vault.vapil.connector.HttpRequestConnector;
import com.veeva.vault.vapil.connector.HttpRequestConnector.HttpMethod;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Task to update Vault Object Records using the REST API.
 */
public class UpdateRecordsTask extends ToolboxModalTask {

    private final String objectName;
    private final List<Map<String, String>> recordsToUpdate;
    private final Consumer<String> onSuccess;
    private final Consumer<String> onError;
    private final Runnable onFinally;

    public UpdateRecordsTask(Project project, String objectName, List<Map<String, String>> recordsToUpdate, Consumer<String> onSuccess, Consumer<String> onError, Runnable onFinally) {
        super(project, "Updating Records", true);
        this.objectName = objectName;
        this.recordsToUpdate = recordsToUpdate;
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.onFinally = onFinally;
    }

    private String successMessage;
    private String errorMessage;
    private boolean sessionExpired = false;

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        if (toolboxProject.isProductionVault()) {
            errorMessage = "This tool cannot be run in a Production domain.";
            return;
        }

        indicator.setIndeterminate(true);
        VaultClient client = toolboxProject.getVaultClient();
        if (client == null) {
            errorMessage = "Not connected to a Vault.";
            return;
        }

        try {
            if (recordsToUpdate.isEmpty()) {
                successMessage = "No records to update.";
                return;
            }

            java.util.Set<String> allKeys = new java.util.LinkedHashSet<>();
            allKeys.add("id");
            for (Map<String, String> record : recordsToUpdate) {
                allKeys.addAll(record.keySet());
            }

            StringBuilder csv = new StringBuilder();

            boolean first = true;
            for (String key : allKeys) {
                if (!first) csv.append(",");
                csv.append(key);
                first = false;
            }
            csv.append("\n");


            for (Map<String, String> record : recordsToUpdate) {
                first = true;
                for (String key : allKeys) {
                    if (!first) csv.append(",");
                    String val = record.get(key);
                    csv.append("\"").append(val == null ? "" : val.replace("\"", "\"\"")).append("\"");
                    first = false;
                }
                csv.append("\n");
            }

            UpdateObjectRecordRequest request = client.newRequest(UpdateObjectRecordRequest.class);
            request.setObjectName(objectName);
            request.setCsvContent(csv.toString());
            request.setVaultDns(toolboxProject.getVaultDNS());
            ObjectRecordBulkResponse response = request.update();

            if (response != null && response.isFailure()) {
                if (toolboxProject.handleSessionExpiration(response)) {
                    sessionExpired = true;
                    return;
                }
            }

            if (response != null && !response.isFailure() && response.getData() != null) {
                int successCount = 0;
                int failureCount = 0;
                StringBuilder errors = new StringBuilder();

                for (ObjectRecordResponse item : response.getData()) {
                    if (item.isSuccessful()) {
                        successCount++;
                    } else {
                        failureCount++;
                        if (item.getErrors() != null && !item.getErrors().isEmpty()) {
                            if (errors.length() > 0) errors.append(", ");
                            errors.append(item.getErrors().get(0).getMessage());
                        }
                    }
                }

                if (failureCount == 0) {
                    successMessage = "Successfully updated " + successCount + " record(s).";
                } else if (successCount == 0) {
                    errorMessage = "Failed to update records: " + errors.toString().trim();
                } else {
                    errorMessage = "Partial success. " + successCount + " record(s) updated, " + failureCount + " failed: " + errors.toString().trim();
                }

            } else {
                String message = response != null ? response.getResponseMessage() : "Unknown error";
                if (response != null && response.getErrors() != null && !response.getErrors().isEmpty()) {
                    message += " - " + response.getErrors().get(0).getMessage();
                }
                errorMessage = message;
            }

        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
        }
    }

    @Override
    public void onSuccess() {
        if (sessionExpired) {
            return;
        }
        if (errorMessage != null) {
            onError.accept(errorMessage);
        } else {
            onSuccess.accept(successMessage);
        }
    }

    @Override
    public void onThrowable(@NotNull Throwable error) {
        if (error instanceof Exception && toolboxProject.handleSessionExpiration((Exception) error)) {
            return;
        }
        onError.accept(error.getMessage() != null ? error.getMessage() : error.toString());
    }

    @Override
    public void onFinished() {
        super.onFinished();
        if (onFinally != null) {
            onFinally.run();
        }
    }

    public static class UpdateObjectRecordRequest extends VaultRequest<UpdateObjectRecordRequest> {
        private String objectName;
        private String csvContent;
        private String vaultDns;

        /**
         * Sets the object name for the request.
         *
         * @param objectName the object name
         */
        public void setObjectName(String objectName) { this.objectName = objectName; }

        /**
         * Sets the CSV content for the request.
         *
         * @param csvContent the CSV content
         */
        public void setCsvContent(String csvContent) { this.csvContent = csvContent; }

        /**
         * Sets the Vault DNS for the request.
         *
         * @param vaultDns the Vault DNS
         */
        public void setVaultDns(String vaultDns) { this.vaultDns = vaultDns; }

        /**
         * Executes the update request.
         *
         * @return the bulk response from Vault
         */
        public ObjectRecordBulkResponse update() {
            String baseUrl = "https://" + vaultDns + "/api/v26.1";
            HttpRequestConnector request = new HttpRequestConnector(baseUrl + "/vobjects/" + objectName);
            request.addHeaderParam("Content-Type", "text/csv");
            request.addHeaderParam("Accept", "application/json");
            request.addRawString("text/csv", csvContent);
            return send(HttpMethod.PUT, request, ObjectRecordBulkResponse.class);
        }
    }
}