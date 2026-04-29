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
import java.time.LocalDate;
import java.time.ZonedDateTime;

public class DownloadAnalyzeApiLogTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(DownloadAnalyzeApiLogTask.class);
	private final VirtualFile virtualFile;
	private final LocalDate startDate;
	private final LocalDate endDate;

	public DownloadAnalyzeApiLogTask(@Nullable Project project,
									 LocalDate startDate,
									 LocalDate endDate) {
		super(project, "Downloading and Analyzing API Usage Logs", true);
		this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public DownloadAnalyzeApiLogTask(@Nullable Project project,
									 @NotNull VirtualFile virtualFile,
									 LocalDate startDate,
									 LocalDate endDate) {
		super(project, "Downloading and Analyzing API Usage Logs", true);
		this.virtualFile = virtualFile;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			super.run(indicator);
			File apiLogDirectory = new File(virtualFile.getPath(), "/api");
			FileIO.makeDirectories(apiLogDirectory);
			ApiUsageLog apiUsageLog = new ApiUsageLog();
			apiUsageLog.download(toolboxProject.getVaultClient(),
					startDate, endDate, apiLogDirectory,
					true);

			File dbFile = new File(virtualFile.getPath(), "toolbox.db");
			apiUsageLog.importLogFiles(dbFile, apiLogDirectory);

			File outputFile = new File(virtualFile.getPath(), "api-" + Date.getDateTimeAsFileName(ZonedDateTime.now()) + ".csv");
			apiUsageLog.analyze(dbFile, outputFile);

			if (Desktop.isDesktopSupported()) {
				try {
					Desktop desktop = Desktop.getDesktop();
					desktop.open(outputFile);
				} catch (Exception ex) {
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
				//message.setTitle("Download");
				//message.append("Download and Analysis Completed");
				//message.showInformation();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
