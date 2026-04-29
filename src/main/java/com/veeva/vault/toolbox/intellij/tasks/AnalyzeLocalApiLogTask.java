package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.api.ApiUsageLog;
import com.veeva.vault.toolbox.core.utils.Date;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.List;

public class AnalyzeLocalApiLogTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeLocalApiLogTask.class);
    private final VirtualFile virtualFile;
    private final List<File> logFiles;

    public AnalyzeLocalApiLogTask(@Nullable Project project, List<File> logFiles) {
        super(project, "Analyzing Local API Usage Logs", true);
        this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
        this.logFiles = logFiles;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            super.run(indicator);

            String vaultIdStr = toolboxProject.getVaultId() != null ? String.valueOf(toolboxProject.getVaultId()) : "local";
            File analysisDirectory = new File(virtualFile.getPath(), "/api/" + vaultIdStr + "/analysis");

            com.veeva.vault.toolbox.core.utils.FileIO.makeDirectories(analysisDirectory);

            ApiUsageLog apiUsageLog = new ApiUsageLog();

            File dbFile = new File(analysisDirectory, "toolbox.db");
            apiUsageLog.importLogFiles(dbFile, logFiles);

            String dateSuffix = getDateRangeSuffix(logFiles);
            File outputFile = new File(analysisDirectory, "api_usage_analysis_" + dateSuffix + ".csv");
            apiUsageLog.analyze(dbFile, outputFile);

            if (Desktop.isDesktopSupported()) {
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

    private String getDateRangeSuffix(List<File> files) {
        java.util.List<java.time.LocalDate> dates = new java.util.ArrayList<>();
        java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

        for (File file : files) {
            java.util.regex.Matcher matcher = datePattern.matcher(file.getName());
            if (matcher.find()) {
                try {
                    dates.add(java.time.LocalDate.parse(matcher.group()));
                } catch (Exception ignored) {}
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
