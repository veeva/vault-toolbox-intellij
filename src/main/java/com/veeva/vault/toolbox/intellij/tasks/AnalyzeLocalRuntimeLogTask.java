package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.sdk.SdkRuntimeLog;
import com.veeva.vault.toolbox.core.utils.FileIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.util.List;

/**
 * Analyzes a user-selected subset of local SDK runtime log files and produces a CSV report.
 * The CSV is written to the standard {@code runtime/{vaultId}/analysis/} folder.
 */
public class AnalyzeLocalRuntimeLogTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeLocalRuntimeLogTask.class);
    private static final String LOCAL_VAULT_ID = "local";

    private final VirtualFile virtualFile;
    private final List<File> logFiles;

    /**
     * @param project  the IntelliJ project, may be {@code null}
     * @param logFiles the specific log files to analyze
     */
    public AnalyzeLocalRuntimeLogTask(@Nullable Project project, List<File> logFiles) {
        super(project, "Analyzing Local SDK Runtime Logs", true);
        this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
        this.logFiles = logFiles;
    }

    /**
     * Orchestrates the analysis of local SDK runtime logs in a background thread.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            String vaultIdStr = toolboxProject.getVaultId() != null ? String.valueOf(toolboxProject.getVaultId()) : LOCAL_VAULT_ID;
            File analysisDirectory = new File(virtualFile.getPath(), "/runtime/" + vaultIdStr + "/analysis");
            FileIO.makeDirectories(analysisDirectory);

            SdkRuntimeLog sdkRuntimeLog = new SdkRuntimeLog();
            File dbFile = new File(analysisDirectory, "toolbox.db");
            sdkRuntimeLog.importLogFiles(dbFile, logFiles);

            String dateSuffix = LogFileNameUtils.getDateRangeSuffix(logFiles);
            File outputFile = new File(analysisDirectory, "runtime_log_analysis_" + dateSuffix + ".csv");
            sdkRuntimeLog.analyze(dbFile, outputFile);

            openInDesktop(outputFile);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
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
