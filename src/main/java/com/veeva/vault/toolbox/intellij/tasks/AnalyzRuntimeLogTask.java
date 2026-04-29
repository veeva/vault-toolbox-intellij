package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.core.logs.sdk.SdkRuntimeLog;
import com.veeva.vault.toolbox.core.utils.Date;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.time.ZonedDateTime;

public class AnalyzRuntimeLogTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzRuntimeLogTask.class);
    private final VirtualFile virtualFile;

    public AnalyzRuntimeLogTask(@Nullable Project project) {
        super(project, "Analyzing SDK Runtime Logs");
        this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
    }

    public AnalyzRuntimeLogTask(@Nullable Project project, @NotNull VirtualFile virtualFile) {
        super(project, "Analyzing SDK Runtime Logs");
        this.virtualFile = virtualFile;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            // 1. Define paths with Vault ID
            String vaultIdStr = toolboxProject.getVaultId() != null ? String.valueOf(toolboxProject.getVaultId()) : "local";
            File runtimeLogDirectory = new File(virtualFile.getPath(), "/runtime/" + vaultIdStr);
            File analysisDirectory = new File(runtimeLogDirectory, "analysis");

            FileIO.makeDirectories(runtimeLogDirectory);
            FileIO.makeDirectories(analysisDirectory);

            SdkRuntimeLog sdkRuntimeLog = new SdkRuntimeLog();

            // 2. Put the SQLite DB inside the analysis folder
            File dbFile = new File(analysisDirectory, "toolbox.db");
            sdkRuntimeLog.importLogFiles(dbFile, runtimeLogDirectory);

            // 3. Output the CSV report with dynamic date suffix
            String dateSuffix = getDateRangeSuffix(runtimeLogDirectory);
            File outputFile = new File(analysisDirectory, "runtime_log_analysis_" + dateSuffix + ".csv");
            sdkRuntimeLog.analyze(dbFile, outputFile);

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

    private String getDateRangeSuffix(File directory) {
        java.util.List<java.time.LocalDate> dates = new java.util.ArrayList<>();
        java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals("analysis")) continue;

                java.util.regex.Matcher matcher = datePattern.matcher(file.getName());
                if (matcher.find()) {
                    try {
                        dates.add(java.time.LocalDate.parse(matcher.group()));
                    } catch (Exception ignored) {}
                }
            }
        }

        if (dates.isEmpty()) {
            return Date.getDateTimeAsFileName(ZonedDateTime.now());
        }

        java.time.LocalDate min = java.util.Collections.min(dates);
        java.time.LocalDate max = java.util.Collections.max(dates);

        if (min.equals(max)) {
            return min.toString();
        } else {
            return min.toString() + "_to_" + max.toString();
        }
    }
}