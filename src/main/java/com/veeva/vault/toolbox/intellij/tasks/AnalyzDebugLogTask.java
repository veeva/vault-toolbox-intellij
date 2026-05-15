package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.sdk.SdkDebugLog;
import com.veeva.vault.toolbox.core.utils.Date;
import com.veeva.vault.toolbox.core.utils.FileIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.time.ZonedDateTime;

/**
 * Analyzes previously downloaded SDK debug logs and produces a CSV report.
 * The CSV is written to the {@code analysis} subdirectory of the debug logs folder.
 */
public class AnalyzDebugLogTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzDebugLogTask.class);
    private static final String LOCAL_VAULT_ID = "local";
    private static final String ANALYSIS_FOLDER = "analysis";
    private static final String DB_FILE_NAME = "toolbox.db";

    private final VirtualFile virtualFile;

    /**
     * Creates a task that analyzes debug logs in the project's default logs directory.
     *
     * @param project the IntelliJ project, may be {@code null}
     */
    public AnalyzDebugLogTask(@Nullable Project project) {
        super(project, "Analyzing SDK Debug Logs");
        this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
    }

    /**
     * Creates a task that analyzes debug logs in the given directory.
     *
     * @param project     the IntelliJ project, may be {@code null}
     * @param virtualFile the logs directory containing SDK debug logs
     */
    public AnalyzDebugLogTask(@Nullable Project project, @NotNull VirtualFile virtualFile) {
        super(project, "Analyzing SDK Debug Logs");
        this.virtualFile = virtualFile;
    }

    /**
     * Orchestrates the analysis of SDK debug logs in a background thread.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            String vaultIdStr = toolboxProject.getVaultId() != null ? String.valueOf(toolboxProject.getVaultId()) : LOCAL_VAULT_ID;
            File debugLogDirectory = new File(virtualFile.getPath(), "/debug/" + vaultIdStr);
            File analysisDirectory = new File(debugLogDirectory, ANALYSIS_FOLDER);

            FileIO.makeDirectories(debugLogDirectory);
            FileIO.makeDirectories(analysisDirectory);

            SdkDebugLog debugLog = new SdkDebugLog();
            String fileName = getBulkLogName(debugLogDirectory);
            File outputFile = new File(analysisDirectory, fileName + ".csv");
            debugLog.analyze(debugLogDirectory, outputFile);

            openInDesktop(outputFile);
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Builds an output filename based on the number of debug log session folders found
     * in the directory. A single session uses its session id; multiple sessions produce
     * a bulk-style filename with a timestamp; an empty directory falls back to a timestamp.
     *
     * @param directory the debug logs directory to scan
     * @return the base name (without extension) for the analysis output file
     */
    private String getBulkLogName(File directory) {
        File[] sessionFolders = directory.listFiles((dir, name) ->
                !ANALYSIS_FOLDER.equals(name) && !DB_FILE_NAME.equals(name) && new File(dir, name).isDirectory()
        );

        int count = (sessionFolders != null) ? sessionFolders.length : 0;
        String timestamp = Date.getDateTimeAsFileName(ZonedDateTime.now());

        if (count == 1) {
            String folderName = sessionFolders[0].getName();
            int dotIndex = folderName.lastIndexOf('.');
            String id = (dotIndex == -1) ? folderName : folderName.substring(dotIndex + 1);
            return "debug_" + id;
        }
        if (count > 1) {
            return "debug_analysis_bulk_" + count + "_sessions_" + timestamp;
        }
        return "debug_analysis_empty_" + timestamp;
    }

    /**
     * Opens the specified file using the system's default desktop application for CSV files.
     *
     * @param file the file to open
     */
    private static void openInDesktop(File file) {
        if (!Desktop.isDesktopSupported() || !file.exists()) {
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (Exception ex) {
            logger.error("Failed to open CSV file: " + ex.getMessage(), ex);
        }
    }
}
