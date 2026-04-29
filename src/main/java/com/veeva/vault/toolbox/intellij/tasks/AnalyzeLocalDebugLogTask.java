package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.sdk.SdkDebugLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.List;

public class AnalyzeLocalDebugLogTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeLocalDebugLogTask.class);
    private final VirtualFile virtualFile;
    private final List<File> logFiles;
    private final String logIdSuffix;

    // Notice we removed outputDir from the constructor!
    public AnalyzeLocalDebugLogTask(@Nullable Project project, List<File> logFiles, String logIdSuffix) {
        super(project, "Analyzing Local SDK Debug Logs", true);
        this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
        this.logFiles = logFiles;
        this.logIdSuffix = logIdSuffix; // This is either the single ID, or the bulk name
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            super.run(indicator);
            SdkDebugLog debugLog = new SdkDebugLog();

            // 1. Build the standardized path directly from the root logs directory
            String vaultIdStr = toolboxProject.getVaultId() != null ? String.valueOf(toolboxProject.getVaultId()) : "local";
            File analysisDirectory = new File(virtualFile.getPath(), "/debug/" + vaultIdStr + "/analysis");

            com.veeva.vault.toolbox.core.utils.FileIO.makeDirectories(analysisDirectory);

            // 2. Name the file based on the suffix we calculated in the panel
            File outputFile = new File(analysisDirectory, "debug_analysis_" + this.logIdSuffix + ".csv");

            // 3. Analyze the master list of all selected files at once!
            debugLog.analyze(logFiles, outputFile);

            if (Desktop.isDesktopSupported() && outputFile.exists()) {
                try {
                    Desktop desktop = Desktop.getDesktop();
                    desktop.open(outputFile);
                } catch (Exception ex) {
                    logger.error("Failed to open CSV file: " + ex.getMessage(), ex);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}