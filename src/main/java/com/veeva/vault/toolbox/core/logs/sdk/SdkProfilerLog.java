package com.veeva.vault.toolbox.core.logs.sdk;

import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.common.SdkProfilingSession;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.LogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SdkProfilerLog {
    private static final Logger logger = LoggerFactory.getLogger(SdkProfilerLog.class);

    public SdkProfilerLog() {
    }

    public void download(VaultClient vaultClient, SdkProfilingSession session, File outputDirectory) {
        try {
            String baseFilename = session.getName() + "_" + session.getId();
            String zipFilename = baseFilename + ".zip";
            String csvFilename = baseFilename + ".csv";
            String jsonFilename = baseFilename + ".json";
            
            File zipFile = new File(outputDirectory, zipFilename);
            File csvFile = new File(outputDirectory, csvFilename);
            File jsonFile = new File(outputDirectory, jsonFilename);
            
            if (csvFile.exists() && jsonFile.exists()) {
                logger.info("Log file and companion JSON already exist for session " + session.getName() + ". Skipping download.");
                return;
            }

            FileIO.makeDirectories(zipFile.getParentFile());

            VaultResponse response = vaultClient.newRequest(LogRequest.class)
                    .setOutputPath(zipFile.getAbsolutePath())
                    .downloadProfilingSessionResults(session.getName());

            if (response != null && !response.isFailure()) {
                logger.info("Downloaded profiler log to " + zipFile.getAbsolutePath());
                FileIO.unzipFiles(zipFile, zipFile.getParentFile());
                
                // Rename the extracted file if it doesn't match the desired format
                // The zip contains a file named RequestProfiler.csv
                File extractedFile = new File(outputDirectory, "RequestProfiler.csv");
                
                if (extractedFile.exists()) {
                    Files.move(extractedFile.toPath(), csvFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                // Delete the original zip file
                if (zipFile.exists()) {
                    zipFile.delete();
                }

                // Create companion JSON file
                FileIO.writeFileContent(jsonFile, session.toJSONObject().toPrettyString());
                logger.info("Created companion JSON for session " + session.getName());

            } else {
                String errorMessage = response != null ? response.getResponseMessage() : "Unknown error";
                logger.error("Failed to download profiler log: " + errorMessage);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
