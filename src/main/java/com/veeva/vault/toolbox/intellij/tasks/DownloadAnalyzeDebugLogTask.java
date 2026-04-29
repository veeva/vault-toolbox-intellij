package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.core.logs.sdk.SdkDebugLog;
import com.veeva.vault.toolbox.core.utils.Date;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.time.ZonedDateTime;

public class DownloadAnalyzeDebugLogTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(DownloadAnalyzeDebugLogTask.class);
	private final VirtualFile virtualFile;
	private final String debugLogId;

	public DownloadAnalyzeDebugLogTask(@Nullable Project project,
									   String debugLogId) {
		super(project, "Downloading and Analyzing SDK Debug Logs", true);
		this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
		this.debugLogId = debugLogId;
	}

	public DownloadAnalyzeDebugLogTask(@Nullable Project project,
									   @NotNull VirtualFile virtualFile,
									   String debugLogId) {
		super(project, "Downloading and Analyzing SDK Debug Logs", true);
		this.virtualFile = virtualFile;
		this.debugLogId = debugLogId;
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			super.run(indicator);
			SdkDebugLog debugLog = new SdkDebugLog();
			File outputDirectory = new File(virtualFile.getPath(), "/debug");
			FileIO.makeDirectories(outputDirectory);
			File outputZipFile = new File(outputDirectory.getPath(), debugLogId + ".zip");
			debugLog.downloadSdkLogs(toolboxProject.getVaultClient(), debugLogId, outputZipFile, true);

			File inputDirectory = new File(virtualFile.getPath());
			File outputFile = new File(inputDirectory, "debug-" + Date.getDateTimeAsFileName(ZonedDateTime.now()) + ".csv");
			debugLog.analyze(inputDirectory, outputFile);

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
				//message.setTitle("SDK Debug Logs");
				//message.append("Download and Analysis Completed");
				//message.showInformation();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
