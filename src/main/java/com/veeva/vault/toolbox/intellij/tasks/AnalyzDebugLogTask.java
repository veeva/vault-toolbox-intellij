package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.sdk.SdkDebugLog;
import com.veeva.vault.toolbox.core.utils.Date;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.time.ZonedDateTime;

public class AnalyzDebugLogTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzDebugLogTask.class);
    private final VirtualFile virtualFile;

    public AnalyzDebugLogTask(@Nullable Project project) {
        super(project, "Analyzing SDK Debug Logs");
        this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
    }

    public AnalyzDebugLogTask(@Nullable Project project,
                              @NotNull VirtualFile virtualFile) {
        super(project, "Analyzing SDK Debug Logs");
        this.virtualFile = virtualFile;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            String vaultIdStr = toolboxProject.getVaultId() != null ? String.valueOf(toolboxProject.getVaultId()) : "local";
            File debugLogDirectory = new File(virtualFile.getPath(), "/debug/" + vaultIdStr);
            File analysisDirectory = new File(debugLogDirectory, "analysis");

            com.veeva.vault.toolbox.core.utils.FileIO.makeDirectories(debugLogDirectory);
            com.veeva.vault.toolbox.core.utils.FileIO.makeDirectories(analysisDirectory);

            SdkDebugLog debugLog = new SdkDebugLog();

            String fileName = getBulkLogName(debugLogDirectory);
            File outputFile = new File(analysisDirectory, fileName + ".csv");

            debugLog.analyze(debugLogDirectory, outputFile);

            if (Desktop.isDesktopSupported() && outputFile.exists()) {
                try {
                    Desktop desktop = Desktop.getDesktop();
                    desktop.open(outputFile);
                } catch (Exception ex) {
                    logger.error("Failed to open CSV file: " + ex.getMessage(), ex);
                }
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private String getBulkLogName(File directory) {
        File[] sessionFolders = directory.listFiles((dir, name) ->
                !name.equals("analysis") && !name.equals("toolbox.db") && new File(dir, name).isDirectory()
        );

        int count = (sessionFolders != null) ? sessionFolders.length : 0;

        if (count == 1) {
            String folderName = sessionFolders[0].getName();
            int dotIndex = folderName.lastIndexOf('.');
            String id = (dotIndex == -1) ? folderName : folderName.substring(dotIndex + 1);
            return "debug_" + id;
        } else if (count > 1) {
            return "debug_analysis_bulk_" + count + "_sessions_" + Date.getDateTimeAsFileName(ZonedDateTime.now());
        } else {
            return "debug_analysis_empty_" + Date.getDateTimeAsFileName(ZonedDateTime.now());
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        try {
            if (toolboxProject != null) {
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}