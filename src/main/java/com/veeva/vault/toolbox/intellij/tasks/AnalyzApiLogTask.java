package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.core.logs.api.ApiUsageLog;
import com.veeva.vault.toolbox.core.utils.Date;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.time.ZonedDateTime;

public class AnalyzApiLogTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(AnalyzApiLogTask.class);
	private final VirtualFile virtualFile;


	public AnalyzApiLogTask(@Nullable Project project) {
		super(project, "Analyzing API Usage Logs");
		this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
	}

	public AnalyzApiLogTask(@Nullable Project project,
							@NotNull VirtualFile virtualFile) {
		super(project, "Analyzing API Usage Logs");
		this.virtualFile = virtualFile;
	}

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            String vaultIdStr = toolboxProject.getVaultId() != null ? String.valueOf(toolboxProject.getVaultId()) : "local";

            File apiLogDirectory = new File(virtualFile.getPath(), "/api/" + vaultIdStr);

            File analysisDirectory = new File(apiLogDirectory, "analysis");

            FileIO.makeDirectories(apiLogDirectory);
            FileIO.makeDirectories(analysisDirectory);

            ApiUsageLog apiUsageLog = new ApiUsageLog();

            File dbFile = new File(analysisDirectory, "toolbox.db");
            apiUsageLog.importLogFiles(dbFile, apiLogDirectory);

            String dateSuffix = getDateRangeSuffix(apiLogDirectory);
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
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

	@Override
	public void onSuccess() {
		super.onSuccess();
		try {
			if (toolboxProject != null) {
				//Message message = toolboxProject.newMessage();
				//message.setTitle("Analyze");
				//message.append("Analyze Completed");
				//message.showInformation();
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
