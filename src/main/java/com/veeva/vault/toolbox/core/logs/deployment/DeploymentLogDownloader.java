package com.veeva.vault.toolbox.core.logs.deployment;

import com.veeva.vault.toolbox.core.results.ProgressResult;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.common.PackageLog;
import com.veeva.vault.vapil.api.model.response.PackageDeploymentResultsResponse;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.ConfigurationMigrationRequest;
import com.veeva.vault.vapil.api.request.VaultRequest;
import com.veeva.vault.vapil.connector.HttpRequestConnector.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DeploymentLogDownloader {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentLogDownloader.class);

    public void downloadLogs(VaultClient vaultClient, String packageId, File outputDirectory, Consumer<ProgressResult> progressConsumer) {
        progressConsumer.accept(new ProgressResult("Retrieving package deploy results..."));
        
        PackageDeploymentResultsResponse response = vaultClient.newRequest(ConfigurationMigrationRequest.class).retrievePackageDeployResults(packageId);
        
        if (response != null && !response.isFailure() && response.getResponseDetails() != null) {
            PackageDeploymentResultsResponse.ResponseDetails details = response.getResponseDetails();
            
            // Handle deployment_log
            List<PackageLog> deploymentLogs = details.getDeploymentLog();
            if (deploymentLogs != null) {
                for (PackageLog log : deploymentLogs) {
                    downloadFile(vaultClient, log.getUrl(), log.getFilename(), outputDirectory, progressConsumer);
                }
            }

            // Handle data_deployment_log (dynamically since it might not be explicitly typed as List<PackageLog>)
            Object dataLogsObj = details.get("data_deployment_log");
            if (dataLogsObj instanceof List) {
                List<?> dataLogsList = (List<?>) dataLogsObj;
                for (Object item : dataLogsList) {
                    if (item instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) item;
                        String url = (String) map.get("url");
                        String filename = (String) map.get("filename");
                        if (url != null && filename != null) {
                            downloadFile(vaultClient, url, filename, outputDirectory, progressConsumer);
                        }
                    } else if (item instanceof PackageLog) {
                        PackageLog log = (PackageLog) item;
                        if (log.getUrl() != null && log.getFilename() != null) {
                            downloadFile(vaultClient, log.getUrl(), log.getFilename(), outputDirectory, progressConsumer);
                        }
                    }
                }
            }
        } else {
            progressConsumer.accept(new ProgressResult("Failed to retrieve package deploy results: " + (response != null ? response.getResponseMessage() : "Unknown error")));
        }
    }

    private void downloadFile(VaultClient vaultClient, String url, String filename, File outputDirectory, Consumer<ProgressResult> progressConsumer) {
        progressConsumer.accept(new ProgressResult("Downloading " + filename + "..."));

        LogDownloadRequest request = vaultClient.newRequest(LogDownloadRequest.class);
        request.setLogUrl(url);
        
        VaultResponse response = request.download();
        
        if (response != null && !response.isFailure() && response.getBinaryContent() != null) {
            File logFile = new File(outputDirectory, filename);
            FileIO.makeDirectories(logFile.getParentFile());
            FileIO.writeFileContent(logFile, response.getBinaryContent());
            
            if (filename.toLowerCase().endsWith(".zip")) {
                progressConsumer.accept(new ProgressResult("Unzipping " + filename + "..."));
                FileIO.unzipFiles(logFile, logFile.getParentFile());
            }
        } else {
            progressConsumer.accept(new ProgressResult("Failed to download " + filename + ": " + (response != null ? response.getResponseMessage() : "Unknown error")));
        }
    }

    // Inner class to download binary from an arbitrary VAPIL URL
    public static class LogDownloadRequest extends VaultRequest<LogDownloadRequest> {
        private String logUrl;

        public LogDownloadRequest() {}

        public void setLogUrl(String logUrl) {
            this.logUrl = logUrl;
        }

        public VaultResponse download() {
            com.veeva.vault.vapil.connector.HttpRequestConnector request = new com.veeva.vault.vapil.connector.HttpRequestConnector(logUrl);
            return sendReturnBinary(HttpMethod.GET, request, VaultResponse.class);
        }
    }
}