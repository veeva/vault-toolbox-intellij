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

import java.io.File;
import java.util.function.Consumer;

public class ConfigurationReport {
    private final static int JOB_WAIT_SECONDS = 11;

    public final static String VAULTPACKAGE_FILENAME = "vaultpackage.xml";

    private VaultClient vaultClient;

    @JsonIgnore
    public ConfigurationReport(VaultClient vaultClient) {
        this.vaultClient = vaultClient;
    }

    private JobStatusResponse getJobStatusWithRetry(int jobId, Consumer<ProgressResult> progressConsumer) {
        JobStatusResponse jobStatusResponse = null;
        int tries = 0;
        boolean completed = false;
        while (!completed) {
            if (tries > 0) {
                try {
                    int numSeconds = JOB_WAIT_SECONDS;
                    for (int i = 0; i < numSeconds; i++) {
                        progressConsumer.accept(new ProgressResult("Waiting " + (numSeconds - i) + " seconds for Job ID = " + jobId));
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }

            progressConsumer.accept(new ProgressResult("Checking job status for Job ID = " + jobId));
            jobStatusResponse = vaultClient.newRequest(JobRequest.class)
                    .retrieveJobStatus(jobId);
            tries++;

            String status = jobStatusResponse.getData().getStatus();
            if (status.equals("SUCCESS") || status.equals("FAILED") || status.equals("ERRORS_ENCOUNTERED") || status.equals("COMPLETED")) {
                completed = true;
            }
            else if (tries == 20) {
                completed = true;
            }
        }

        return jobStatusResponse;
    }

    @JsonIgnore
    public DeploymentResult downloadConfigurationReport(File outputFile, Consumer<ProgressResult> progressConsumer, boolean unZip) {
        DeploymentResult deploymentResult = new DeploymentResult();
        try {
            progressConsumer.accept(new ProgressResult("Requesting Configuration Report"));

            JobCreateResponse importJobResponse = vaultClient.newRequest(ConfigurationMigrationRequest.class)
                    .vaultConfigurationReport();

            if (importJobResponse.isFailure() || importJobResponse.getJobId() == null) {
                deploymentResult.addErrorMessage("Failed to request report: " + importJobResponse.getResponseMessage());
                return deploymentResult;
            }

            JobStatusResponse jobStatusResponse = getJobStatusWithRetry(importJobResponse.getJobId(), progressConsumer);

            if (jobStatusResponse != null && jobStatusResponse.getData().getStatus().equals("SUCCESS")) {
                progressConsumer.accept(new ProgressResult("Downloading Config Report"));

                VaultResponse response = vaultClient.newRequest(ConfigurationMigrationRequest.class)
                        .setOutputPath(outputFile.getPath())
                        .retrieveConfigurationReportResults(importJobResponse.getJobId().toString());

                if (response != null && !response.isFailure() && outputFile.exists() && unZip) {
                    FileIO.unzipFiles(outputFile, outputFile.getParentFile());
                } else if (response != null && response.isFailure()) {
                    deploymentResult.addErrorMessage("Vault API failed to download the report: " + response.getResponseMessage());
                }
            } else if (jobStatusResponse != null) {
                deploymentResult.addErrorMessage("Report job finished with status: " + jobStatusResponse.getData().getStatus());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            deploymentResult.addErrorMessage(e.getMessage());
        }
        return deploymentResult;
    }
}