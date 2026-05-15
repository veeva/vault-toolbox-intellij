package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.api.ApiUsageLog;
import com.veeva.vault.toolbox.core.utils.FileIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;

/**
 * Analyzes previously downloaded API usage logs and produces a CSV report.
 * The CSV is written to the {@code analysis} subdirectory of the API logs folder.
 */
public class AnalyzApiLogTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(AnalyzApiLogTask.class);
	private static final String LOCAL_VAULT_ID = "local";

	private final VirtualFile virtualFile;

	/**
	 * Creates a task that analyzes API logs in the project's default logs directory.
	 *
	 * @param project the IntelliJ project, may be {@code null}
	 */
	public AnalyzApiLogTask(@Nullable Project project) {
		super(project, "Analyzing API Usage Logs");
		this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
	}

	/**
	 * Creates a task that analyzes API logs in the given directory.
	 *
	 * @param project     the IntelliJ project, may be {@code null}
	 * @param virtualFile the logs directory containing API usage logs
	 */
	public AnalyzApiLogTask(@Nullable Project project, @NotNull VirtualFile virtualFile) {
		super(project, "Analyzing API Usage Logs");
		this.virtualFile = virtualFile;
	}

	/**
	 * Orchestrates the analysis of API usage logs in a background thread.
	 *
	 * @param indicator the progress indicator for the background task
	 */
	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			String vaultIdStr = toolboxProject.getVaultId() != null ? String.valueOf(toolboxProject.getVaultId()) : LOCAL_VAULT_ID;
			File apiLogDirectory = new File(virtualFile.getPath(), "/api/" + vaultIdStr);
			File analysisDirectory = new File(apiLogDirectory, "analysis");

			FileIO.makeDirectories(apiLogDirectory);
			FileIO.makeDirectories(analysisDirectory);

			ApiUsageLog apiUsageLog = new ApiUsageLog();
			File dbFile = new File(analysisDirectory, "toolbox.db");
			apiUsageLog.importLogFiles(dbFile, apiLogDirectory);

			String dateSuffix = LogFileNameUtils.getDateRangeSuffix(apiLogDirectory);
			File outputFile = new File(analysisDirectory, "api_usage_analysis_" + dateSuffix + ".csv");
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
