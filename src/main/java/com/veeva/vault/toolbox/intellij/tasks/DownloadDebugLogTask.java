package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.sdk.SdkDebugLog;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.vapil.api.model.common.SdkDebugSession;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.AuthenticationRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class DownloadDebugLogTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(DownloadDebugLogTask.class);
    private final List<SdkDebugSession> sessions;
    private final Runnable onComplete;
    private boolean isSuccess = false;

    public DownloadDebugLogTask(@Nullable Project project, List<SdkDebugSession> sessions) {
        this(project, sessions, null);
    }

    public DownloadDebugLogTask(@Nullable Project project, List<SdkDebugSession> sessions, Runnable onComplete) {
        super(project, "Downloading SDK Debug Logs", true);
        this.sessions = sessions;
        this.onComplete = onComplete;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            super.run(indicator);

            VaultResponse authCheck = toolboxProject.getVaultClient()
                    .newRequest(AuthenticationRequest.class)
                    .sessionKeepAlive();

            if (toolboxProject.handleSessionExpiration(authCheck)) {
                return;
            }

            File debugLogDirectory = new File(toolboxProject.getLogsDirectory(), "/debug/" + toolboxProject.getVaultId());
            FileIO.makeDirectories(debugLogDirectory);
            SdkDebugLog sdkDebugLog = new SdkDebugLog();

            for (SdkDebugSession session : sessions) {
                String baseFilename = session.getName() + "." + session.getId();
                File sessionDirectory = new File(debugLogDirectory, baseFilename);

                if (sessionDirectory.exists()) {
                    try {
                        org.apache.commons.io.FileUtils.deleteDirectory(sessionDirectory);
                    } catch (Exception ex) {
                        logger.error("Failed to clean up existing session directory: " + sessionDirectory.getName(), ex);
                    }
                }

                FileIO.makeDirectories(sessionDirectory);

                File outputFile = new File(sessionDirectory, baseFilename + ".zip");
                sdkDebugLog.downloadSdkLogs(toolboxProject.getVaultClient(), session.getId(), outputFile, false);

                if (outputFile.exists()) {
                    File jsonFile = new File(sessionDirectory, baseFilename + ".json");
                    FileIO.writeFileContent(jsonFile, session.toJSONObject().toPrettyString());

                    try {
                        FileIO.unzipFiles(outputFile, sessionDirectory);
                    } catch (Exception ex) {
                        logger.error("Failed to automatically extract debug log zip file for session: " + session.getId(), ex);
                    }
                }
            }

            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                com.intellij.openapi.vfs.VirtualFileManager.getInstance().syncRefresh();
            });
            isSuccess = true;

        } catch (Exception e) {
            if (toolboxProject.handleSessionExpiration(e)) {
                return;
            }
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (!isSuccess) return;
        if (onComplete != null) {
            onComplete.run();
        }
    }
}