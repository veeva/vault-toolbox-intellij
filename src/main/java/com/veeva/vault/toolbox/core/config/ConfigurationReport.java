package com.veeva.vault.toolbox.core.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.veeva.vault.toolbox.core.results.DeploymentResult;
import com.veeva.vault.toolbox.core.results.ProgressResult;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.response.JobCreateResponse;
import com.veeva.vault.vapil.api.model.response.JobStatusResponse;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.ConfigurationMigrationRequest;
import com.veeva.vault.vapil.api.request.JobRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Generates and downloads Vault configuration reports through the
 * Configuration Migration API.
 */
public class ConfigurationReport {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationReport.class);

    private static final int JOB_WAIT_SECONDS = 11;
    private static final int MAX_STATUS_RETRIES = 20;
    private static final String JOB_STATUS_SUCCESS = "SUCCESS";
    private static final Set<String> TERMINAL_JOB_STATUSES =
            Set.of(JOB_STATUS_SUCCESS, "FAILED", "ERRORS_ENCOUNTERED", "COMPLETED");

    private final VaultClient vaultClient;

    /**
     * Creates a new {@code ConfigurationReport} bound to the given Vault client.
     *
     * @param vaultClient the authenticated Vault API client used to issue requests
     */
    @JsonIgnore
    public ConfigurationReport(VaultClient vaultClient) {
        this.vaultClient = vaultClient;
    }

    /**
     * Requests a configuration report from Vault, polls the asynchronous job
     * until it reaches a terminal status, and downloads the resulting archive
     * to {@code outputFile}.
     *
     * @param outputFile       the destination file for the downloaded archive
     * @param progressConsumer callback that receives progress updates throughout
     *                         the request, polling, and download phases
     * @param unzip            when {@code true}, the downloaded archive is
     *                         extracted into the parent directory of
     * @param cancelledCheck   supplier to check if the task has been cancelled
     * @param options          additional options to configure the report request
     * @return a {@link DeploymentResult} describing the outcome and any errors
     */
     @JsonIgnore
     public DeploymentResult downloadConfigurationReport(File outputFile,
                                                        Consumer<ProgressResult> progressConsumer,
                                                        boolean unzip,
                                                        BooleanSupplier cancelledCheck,
                                                        Options options) {
        DeploymentResult deploymentResult = new DeploymentResult();
        try {
            progressConsumer.accept(new ProgressResult("Requesting Configuration Report"));

            ConfigurationMigrationRequest request = vaultClient.newRequest(ConfigurationMigrationRequest.class);
            if (options != null) {
                if (options.includeVaultSettings != null) request.setIncludeVaultSettings(options.includeVaultSettings);
                if (options.includeInactiveComponents != null) request.setIncludeInactiveComponents(options.includeInactiveComponents);
                if (options.includeComponentsModifiedSince != null) request.setIncludeComponentsModifiedSince(options.includeComponentsModifiedSince);
                if (options.includeDocBinderTemplates != null) request.setIncludeDocBinderTemplates(options.includeDocBinderTemplates);
                if (options.suppressEmptyResults != null) request.setSuppressEmptyResults(options.suppressEmptyResults);
                if (options.componentTypes != null && !options.componentTypes.isEmpty()) {
                    try {
                        for (java.lang.reflect.Method method : request.getClass().getMethods()) {
                            if (method.getName().toLowerCase().contains("componenttype")) {
                                Class<?>[] paramTypes = method.getParameterTypes();
                                if (paramTypes.length == 1) {
                                    if (paramTypes[0] == String.class) {
                                        method.invoke(request, String.join(",", options.componentTypes));
                                        break;
                                    } else if (java.util.List.class.isAssignableFrom(paramTypes[0])) {
                                        method.invoke(request, options.componentTypes);
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to set component types via reflection", e);
                    }
                }
                if (options.outputFormat != null) request.setOutputFormat(options.outputFormat);
            }

            JobCreateResponse jobCreateResponse = request.vaultConfigurationReport();

            if (jobCreateResponse.isFailure() || jobCreateResponse.getJobId() == null) {
                deploymentResult.addErrorMessage("Failed to request report: " + jobCreateResponse.getResponseMessage());
                return deploymentResult;
            }

            JobStatusResponse jobStatusResponse = pollJobUntilComplete(jobCreateResponse.getJobId(), progressConsumer, cancelledCheck);
            if (jobStatusResponse == null) {
                return deploymentResult;
            }

            String status = jobStatusResponse.getData().getStatus();
            if (!JOB_STATUS_SUCCESS.equals(status)) {
                deploymentResult.addErrorMessage("Report job finished with status: " + status);
                return deploymentResult;
            }

            if (cancelledCheck.getAsBoolean()) {
                return deploymentResult;
            }

            progressConsumer.accept(new ProgressResult("Downloading Config Report"));
            VaultResponse response = vaultClient.newRequest(ConfigurationMigrationRequest.class)
                    .setOutputPath(outputFile.getPath())
                    .retrieveConfigurationReportResults(jobCreateResponse.getJobId().toString());

            if (response == null) {
                return deploymentResult;
            }

            if (response.isFailure()) {
                deploymentResult.addErrorMessage("Vault API failed to download the report: " + response.getResponseMessage());
                return deploymentResult;
            }

            if (unzip && outputFile.exists()) {
                FileIO.unzipFiles(outputFile, outputFile.getParentFile());
            }
        } catch (Exception e) {
            logger.error("Failed to download configuration report", e);
            deploymentResult.addErrorMessage(e.getMessage());
        }
        return deploymentResult;
    }

    /**
     * Polls the status of an asynchronous Vault job until it reaches a terminal state.
     *
     * @param jobId            the ID of the job to poll
     * @param progressConsumer callback for progress updates
     * @param cancelledCheck   supplier to check if the task has been cancelled
     * @return the final {@link JobStatusResponse}, or {@code null} if cancelled or an error occurred
     */
    private JobStatusResponse pollJobUntilComplete(int jobId, Consumer<ProgressResult> progressConsumer, BooleanSupplier cancelledCheck) {
        JobStatusResponse jobStatusResponse = null;
        for (int tries = 0; tries < MAX_STATUS_RETRIES; tries++) {
            if (cancelledCheck.getAsBoolean()) {
                return null;
            }
            if (tries > 0 && !waitWithProgress(jobId, progressConsumer, cancelledCheck)) {
                return null;
            }

            progressConsumer.accept(new ProgressResult("Checking job status for Job ID = " + jobId));
            jobStatusResponse = vaultClient.newRequest(JobRequest.class).retrieveJobStatus(jobId);

            if (TERMINAL_JOB_STATUSES.contains(jobStatusResponse.getData().getStatus())) {
                return jobStatusResponse;
            }
        }
        return jobStatusResponse;
    }

    /**
     * Waits for a specified interval while providing progress updates and checking for cancellation.
     *
     * @param jobId            the ID of the job being waited on
     * @param progressConsumer callback for progress updates
     * @param cancelledCheck   supplier to check if the task has been cancelled
     * @return {@code true} if the wait completed; {@code false} if cancelled or interrupted
     */
    private boolean waitWithProgress(int jobId, Consumer<ProgressResult> progressConsumer, BooleanSupplier cancelledCheck) {
        try {
            for (int i = 0; i < JOB_WAIT_SECONDS; i++) {
                if (cancelledCheck.getAsBoolean()) {
                    return false;
                }
                progressConsumer.accept(new ProgressResult(
                        "Waiting " + (JOB_WAIT_SECONDS - i) + " seconds for Job ID = " + jobId));
                Thread.sleep(1000);
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Configuration options for the Vault configuration report request.
     */
    public static class Options {
        /** Includes Vault settings in the report. */
        public Boolean includeVaultSettings;
        /** Includes inactive components in the report. */
        public Boolean includeInactiveComponents;
        /** Filters the report to components modified since this date. */
        public java.time.ZonedDateTime includeComponentsModifiedSince;
        /** Includes document binder templates in the report. */
        public Boolean includeDocBinderTemplates;
        /** Suppresses empty results from the report. */
        public Boolean suppressEmptyResults;
        /** Filters the report to these specific component types. */
        public java.util.List<String> componentTypes;
        /** The format of the output report. */
        public ConfigurationMigrationRequest.OutputFormat outputFormat;
    }
}

