package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.core.logs.api.ApiUsageLog;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.AuthenticationRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadApiLogTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(DownloadApiLogTask.class);
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Runnable onComplete;
    private boolean isSuccess = false;

    public DownloadApiLogTask(@Nullable Project project,
                              LocalDate startDate,
                              LocalDate endDate) {
        this(project, startDate, endDate, null);
    }

    public DownloadApiLogTask(@Nullable Project project,
                              LocalDate startDate,
                              LocalDate endDate,
                              Runnable onComplete) {
        super(project, "Downloading API Usage Logs");
        this.startDate = startDate;
        this.endDate = endDate;
        this.onComplete = onComplete;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            VaultResponse authCheck = toolboxProject.getVaultClient()
                    .newRequest(AuthenticationRequest.class)
                    .sessionKeepAlive();

            if (toolboxProject.handleSessionExpiration(authCheck)) {
                return;
            }

            File outputDirectory = new File(toolboxProject.getLogsDirectory().getPath(),  "/api/" + toolboxProject.getVaultId());
            FileIO.makeDirectories(outputDirectory);

            ApiUsageLog apiUsageLog = new ApiUsageLog();

            apiUsageLog.download(toolboxProject.getVaultClient(),
                    startDate, endDate, outputDirectory,
                    true);

            File[] downloadedFiles = outputDirectory.listFiles();
            if (downloadedFiles != null) {
                Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
                for (File file : downloadedFiles) {
                    if (file.isFile()) {
                        Matcher matcher = datePattern.matcher(file.getName());
                        if (matcher.find()) {
                            String dateStr = matcher.group();
                            File dateFolder = new File(outputDirectory, dateStr);

                            if (!dateFolder.exists()) {
                                FileIO.makeDirectories(dateFolder);
                            }

                            Files.move(file.toPath(), new File(dateFolder, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
            isSuccess = true;
        }
        catch (Exception e) {
            if (toolboxProject.handleSessionExpiration(e)) {
                return;
            }
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();

        if (!isSuccess) {
            return;
        }

        try {
            if (toolboxProject != null) {
                Message message = toolboxProject.newMessage();
                message.setTitle("Download");
                message.append("Download Completed");
                message.showInformation();
            }
            if (onComplete != null) {
                onComplete.run();
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}