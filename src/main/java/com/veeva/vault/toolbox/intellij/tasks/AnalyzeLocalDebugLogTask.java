package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.sdk.SdkDebugLog;
import com.veeva.vault.toolbox.core.utils.FileIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.util.List;

/**
 * Analyzes a user-selected subset of local SDK debug log files and produces a CSV report.
 * The CSV is written to the standard {@code debug/{vaultId}/analysis/} folder, using the
 * supplied suffix to differentiate between single-session and bulk runs.
 */
public class AnalyzeLocalDebugLogTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeLocalDebugLogTask.class);
    private static final String LOCAL_VAULT_ID = "local";

    private final VirtualFile virtualFile;
    private final List<File> logFiles;
    private final String logIdSuffix;
    private File outputFile;

    /**
     * @param project     the IntelliJ project, may be {@code null}
     * @param logFiles    the specific log files to analyze
     * @param logIdSuffix either a single session id or a precomputed bulk-style suffix
     */
    public AnalyzeLocalDebugLogTask(@Nullable Project project, List<File> logFiles, String logIdSuffix) {
        super(project, "Analyzing Local SDK Debug Logs", true);
        this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
        this.logFiles = logFiles;
        this.logIdSuffix = logIdSuffix;
    }

    /**
     * Orchestrates the analysis of local SDK debug logs in a background thread.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            SdkDebugLog debugLog = new SdkDebugLog();

            String vaultIdStr = toolboxProject.getVaultId() != null ? String.valueOf(toolboxProject.getVaultId()) : LOCAL_VAULT_ID;
            File analysisDirectory = new File(virtualFile.getPath(), "/debug/" + vaultIdStr + "/analysis");
            FileIO.makeDirectories(analysisDirectory);

            outputFile = new File(analysisDirectory, "debug_analysis_" + this.logIdSuffix + ".csv");
            debugLog.analyze(logFiles, outputFile);

            openInDesktop(outputFile);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Triggered after the log analysis completes successfully. Selects the resulting
     * CSV file in the Project View and shows a success notification if desktop
     * auto-open is disabled.
     */
    @Override
    public void onSuccess() {
        super.onSuccess();
        if (outputFile != null) {
            VirtualFile vFile = VfsUtil.findFileByIoFile(outputFile, true);
            selectInProjectView(vFile);

            com.veeva.vault.toolbox.intellij.settings.AppSettings.AppState state = com.veeva.vault.toolbox.intellij.settings.AppSettings.getInstance().getState();
            if (state == null || !state.openLogsInDesktop) {
                com.veeva.vault.toolbox.intellij.ui.Message message = new com.veeva.vault.toolbox.intellij.ui.Message(toolboxProject);
                message.setTitle("Analyze Logs");
                message.append("SDK debug log analysis completed successfully.");
                message.showInformation();
            }
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
        com.veeva.vault.toolbox.intellij.settings.AppSettings.AppState state = com.veeva.vault.toolbox.intellij.settings.AppSettings.getInstance().getState();
        if (state == null || !state.openLogsInDesktop) {
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (Exception ex) {
            logger.error("Failed to open CSV file: " + ex.getMessage(), ex);
        }
    }
}
