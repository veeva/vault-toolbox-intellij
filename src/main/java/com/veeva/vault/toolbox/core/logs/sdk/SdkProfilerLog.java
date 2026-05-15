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

/**
 * Manages the downloading of Vault SDK Profiler logs.
 * Profiler logs are downloaded as ZIP files, extracted, and renamed for clarity.
 * A companion JSON file is created to store session metadata.
 */
public class SdkProfilerLog {
    private static final Logger logger = LoggerFactory.getLogger(SdkProfilerLog.class);

    /**
     * Downloads the profiling session results for the given session and saves them to the output directory.
     * If the CSV and companion JSON files already exist, the download is skipped.
     * The downloaded ZIP is extracted and the inner {@code RequestProfiler.csv} is renamed to match
     * the session name and ID.
     *
     * @param vaultClient     authenticated Vault client
     * @param session         the profiling session to download
     * @param outputDirectory directory where the downloaded files will be saved
     */
    public void download(VaultClient vaultClient, SdkProfilingSession session, File outputDirectory) {
        try {
            String baseFilename = session.getName() + "_" + session.getId();
            File zipFile = new File(outputDirectory, baseFilename + ".zip");
            File csvFile = new File(outputDirectory, baseFilename + ".csv");
            File jsonFile = new File(outputDirectory, baseFilename + ".json");

            if (csvFile.exists() && jsonFile.exists()) {
                logger.info("Log file and companion JSON already exist for session {}. Skipping download.", session.getName());
                return;
            }

            FileIO.makeDirectories(zipFile.getParentFile());

            VaultResponse response = vaultClient.newRequest(LogRequest.class)
                    .setOutputPath(zipFile.getAbsolutePath())
                    .downloadProfilingSessionResults(session.getName());

            if (response != null && !response.isFailure()) {
                logger.info("Downloaded profiler log to {}", zipFile.getAbsolutePath());
                FileIO.unzipFiles(zipFile, zipFile.getParentFile());

                File extractedFile = new File(outputDirectory, "RequestProfiler.csv");
                if (extractedFile.exists()) {
                    Files.move(extractedFile.toPath(), csvFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                if (zipFile.exists()) {
                    zipFile.delete();
                }

                FileIO.writeFileContent(jsonFile, session.toJSONObject().toPrettyString());
                logger.info("Created companion JSON for session {}", session.getName());
            } else {
                String errorMessage = response != null ? response.getResponseMessage() : "Unknown error";
                logger.error("Failed to download profiler log: {}", errorMessage);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
