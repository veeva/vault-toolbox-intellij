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
 * Downloads SDK debug logs for a given debug session id and immediately analyzes them,
 * producing a CSV report that is opened on the desktop when complete.
 */
public class DownloadAnalyzeDebugLogTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(DownloadAnalyzeDebugLogTask.class);

	private final VirtualFile virtualFile;
	private final String debugLogId;

	/**
	 * Downloads and analyzes the debug session into the project's default logs directory.
	 *
	 * @param project    the IntelliJ project, may be {@code null}
	 * @param debugLogId the debug session id to download
	 */
	public DownloadAnalyzeDebugLogTask(@Nullable Project project, String debugLogId) {
		super(project, "Downloading and Analyzing SDK Debug Logs", true);
		this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
		this.debugLogId = debugLogId;
	}

	/**
	 * Downloads and analyzes the debug session into the supplied directory.
	 *
	 * @param project     the IntelliJ project, may be {@code null}
	 * @param virtualFile the destination logs directory
	 * @param debugLogId  the debug session id to download
	 */
	public DownloadAnalyzeDebugLogTask(@Nullable Project project,
									   @NotNull VirtualFile virtualFile,
									   String debugLogId) {
		super(project, "Downloading and Analyzing SDK Debug Logs", true);
		this.virtualFile = virtualFile;
		this.debugLogId = debugLogId;
	}

	/**
	 * Downloads and analyzes SDK debug logs in a background thread.
	 *
	 * @param indicator the progress indicator for the background task
	 */
	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			SdkDebugLog debugLog = new SdkDebugLog();
			File outputDirectory = new File(virtualFile.getPath(), "/debug");
			FileIO.makeDirectories(outputDirectory);

			File outputZipFile = new File(outputDirectory.getPath(), debugLogId + ".zip");
			debugLog.downloadSdkLogs(toolboxProject.getVaultClient(), debugLogId, outputZipFile, true);

			File inputDirectory = new File(virtualFile.getPath());
			File outputFile = new File(inputDirectory, "debug-" + Date.getDateTimeAsFileName(ZonedDateTime.now()) + ".csv");
			debugLog.analyze(inputDirectory, outputFile);

			openInDesktop(outputFile);
		}
		catch (Exception e) {
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
