package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.api.ApiUsageLog;
import com.veeva.vault.toolbox.core.utils.Date;
import com.veeva.vault.toolbox.core.utils.FileIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Downloads API usage logs for the given date range and immediately analyzes them,
 * producing a CSV report that is opened on the desktop when complete.
 */
public class DownloadAnalyzeApiLogTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(DownloadAnalyzeApiLogTask.class);

	private final VirtualFile virtualFile;
	private final LocalDate startDate;
	private final LocalDate endDate;

	/**
	 * Downloads and analyzes logs into the project's default logs directory.
	 *
	 * @param project   the IntelliJ project, may be {@code null}
	 * @param startDate inclusive start of the date range to download
	 * @param endDate   inclusive end of the date range to download
	 */
	public DownloadAnalyzeApiLogTask(@Nullable Project project, LocalDate startDate, LocalDate endDate) {
		super(project, "Downloading and Analyzing API Usage Logs", true);
		this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
		this.startDate = startDate;
		this.endDate = endDate;
	}

	/**
	 * Downloads and analyzes logs into the supplied directory.
	 *
	 * @param project     the IntelliJ project, may be {@code null}
	 * @param virtualFile the destination logs directory
	 * @param startDate   inclusive start of the date range to download
	 * @param endDate     inclusive end of the date range to download
	 */
	public DownloadAnalyzeApiLogTask(@Nullable Project project,
									 @NotNull VirtualFile virtualFile,
									 LocalDate startDate,
									 LocalDate endDate) {
		super(project, "Downloading and Analyzing API Usage Logs", true);
		this.virtualFile = virtualFile;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	/**
	 * Downloads and analyzes API logs in a background thread.
	 *
	 * @param indicator the progress indicator for the background task
	 */
	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			File apiLogDirectory = new File(virtualFile.getPath(), "/api");
			FileIO.makeDirectories(apiLogDirectory);

			ApiUsageLog apiUsageLog = new ApiUsageLog();
			apiUsageLog.download(toolboxProject.getVaultClient(), startDate, endDate, apiLogDirectory, true);

			File dbFile = new File(virtualFile.getPath(), "toolbox.db");
			apiUsageLog.importLogFiles(dbFile, apiLogDirectory);

			File outputFile = new File(virtualFile.getPath(), "api-" + Date.getDateTimeAsFileName(ZonedDateTime.now()) + ".csv");
			apiUsageLog.analyze(dbFile, outputFile);

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
