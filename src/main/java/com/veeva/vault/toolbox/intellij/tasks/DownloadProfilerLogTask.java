package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.sdk.SdkProfilerLog;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.vapil.api.model.common.SdkProfilingSession;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.AuthenticationRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class DownloadProfilerLogTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(DownloadProfilerLogTask.class);
    private final VirtualFile virtualFile;
    private final List<SdkProfilingSession> sessions;
    private final Runnable onComplete;

    public DownloadProfilerLogTask(@Nullable Project project, List<SdkProfilingSession> sessions) {
        this(project, sessions, null);
    }

    public DownloadProfilerLogTask(@Nullable Project project, List<SdkProfilingSession> sessions, Runnable onComplete) {
        super(project, "Downloading SDK Profiler Logs", true);
        this.virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
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

            File profilerLogDirectory = new File(virtualFile.getPath(), "/profiler/" + toolboxProject.getVaultId());
            SdkProfilerLog sdkProfilerLog = new SdkProfilerLog();

            for (SdkProfilingSession session : sessions) {
                String sessionFolderName = session.getName() + "." + session.getId();
                File sessionDir = new File(profilerLogDirectory, sessionFolderName);
                FileIO.makeDirectories(sessionDir);

                sdkProfilerLog.download(toolboxProject.getVaultClient(), session, sessionDir);
            }

            com.intellij.openapi.vfs.VirtualFile vLogsDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                    .refreshAndFindFileByIoFile(toolboxProject.getLogsDirectory());
            if (vLogsDir != null) {
                vLogsDir.refresh(false, true);
            }
            // ---------------------------------------------------

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
        if (onComplete != null) {
            onComplete.run();
        }
    }
}