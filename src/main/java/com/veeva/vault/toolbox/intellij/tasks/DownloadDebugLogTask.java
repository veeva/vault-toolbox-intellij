package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.veeva.vault.toolbox.core.logs.sdk.SdkDebugLog;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.vapil.api.model.common.SdkDebugSession;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.AuthenticationRequest;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Downloads SDK debug log archives for one or more sessions, expands each archive into
 * its own session directory, and writes a sidecar JSON describing the session.
 */
public class DownloadDebugLogTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(DownloadDebugLogTask.class);

    private final List<SdkDebugSession> sessions;
    private final Runnable onComplete;
    private boolean isSuccess = false;
    private File debugLogDirectory;

    /**
     * @param project  the IntelliJ project, may be {@code null}
     * @param sessions the debug sessions to download
     */
    public DownloadDebugLogTask(@Nullable Project project, List<SdkDebugSession> sessions) {
        this(project, sessions, null);
    }

    /**
     * @param project    the IntelliJ project, may be {@code null}
     * @param sessions   the debug sessions to download
     * @param onComplete optional callback invoked after a successful download
     */
    public DownloadDebugLogTask(@Nullable Project project, List<SdkDebugSession> sessions, Runnable onComplete) {
        super(project, "Downloading SDK Debug Logs", true);
        this.sessions = sessions;
        this.onComplete = onComplete;
    }

    /**
     * Downloads and processes SDK debug log sessions.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            VaultResponse authCheck = toolboxProject.getVaultClient()
                    .newRequest(AuthenticationRequest.class)
                    .sessionKeepAlive();

            if (toolboxProject.handleSessionExpiration(authCheck)) {
                return;
            }

            File baseDebugLogDirectory = new File(toolboxProject.getLogsDirectory(), "/debug/" + toolboxProject.getVaultId());
            FileIO.makeDirectories(baseDebugLogDirectory);

            SdkDebugLog sdkDebugLog = new SdkDebugLog();
            for (SdkDebugSession session : sessions) {
                debugLogDirectory = downloadSession(sdkDebugLog, session, baseDebugLogDirectory);
            }

            ApplicationManager.getApplication().invokeLater(() ->
                    VirtualFileManager.getInstance().syncRefresh());
            isSuccess = true;
        } catch (Exception e) {
            if (toolboxProject.handleSessionExpiration(e)) {
                return;
            }
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Downloads a single debug session, replacing any pre-existing session directory,
     * and extracts the resulting zip alongside a JSON description of the session.
     *
     * @param sdkDebugLog       the debug-log helper used to download from Vault
     * @param session           the session being downloaded
     * @param debugLogDirectory the parent directory holding all session folders
     * @return the created session directory
     */
    private File downloadSession(SdkDebugLog sdkDebugLog, SdkDebugSession session, File debugLogDirectory) {
        String baseFilename = session.getName() + "." + session.getId();
        File sessionDirectory = new File(debugLogDirectory, baseFilename);

        if (sessionDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(sessionDirectory);
            } catch (Exception ex) {
                logger.error("Failed to clean up existing session directory: " + sessionDirectory.getName(), ex);
            }
        }
        FileIO.makeDirectories(sessionDirectory);

        File outputFile = new File(sessionDirectory, baseFilename + ".zip");
        sdkDebugLog.downloadSdkLogs(toolboxProject.getVaultClient(), session.getId(), outputFile, false);

        if (!outputFile.exists()) {
            return sessionDirectory;
        }

        File jsonFile = new File(sessionDirectory, baseFilename + ".json");
        FileIO.writeFileContent(jsonFile, session.toJSONObject().toPrettyString());

        try {
            FileIO.unzipFiles(outputFile, sessionDirectory);
        } catch (Exception ex) {
            logger.error("Failed to automatically extract debug log zip file for session: " + session.getId(), ex);
        }
        
        return sessionDirectory;
    }

    /**
     * Executes the completion callback and reveals the debug log directory.
     */
    @Override
    public void onSuccess() {
        super.onSuccess();
        if (!isSuccess) {
            return;
        }
        if (onComplete != null) {
            onComplete.run();
        }
        VirtualFile vDir = VfsUtil.findFileByIoFile(debugLogDirectory, true);
        selectInProjectView(vDir);
    }
}
