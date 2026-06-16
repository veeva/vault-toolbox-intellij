package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.LogArchiveImporter;
import com.veeva.vault.toolbox.intellij.ui.DeveloperLogsDialog;
import com.veeva.vault.toolbox.intellij.ui.Message;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Background task to import a zip archive of developer logs into the workspace.
 */
public class ImportLogArchiveTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(ImportLogArchiveTask.class);

    private final File archiveFile;
    private final String vaultId;
    private final DeveloperLogsDialog.LogType logType;
    private final Runnable onComplete;
    private boolean isSuccess = false;
    private File targetDirectory;

    public ImportLogArchiveTask(Project project, File archiveFile, String vaultId, DeveloperLogsDialog.LogType logType, Runnable onComplete) {
        super(project, "Importing Log Archive", true);
        this.archiveFile = archiveFile;
        this.vaultId = vaultId;
        this.logType = logType;
        this.onComplete = onComplete;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            indicator.setIndeterminate(true);
            indicator.setText("Extracting and organizing logs...");

            String basePath = "";
            switch (logType) {
                case API_USAGE:
                    basePath = "/api/";
                    break;
                case SDK_DEBUG:
                    basePath = "/debug/";
                    break;
                case SDK_PROFILER:
                    basePath = "/profiler/";
                    break;
                case SDK_RUNTIME:
                    basePath = "/runtime/";
                    break;
            }

            targetDirectory = new File(toolboxProject.getLogsDirectory().getPath(), basePath + vaultId);

            LogArchiveImporter importer = new LogArchiveImporter();
            isSuccess = importer.importArchive(archiveFile, vaultId, logType.name(), targetDirectory);

            if (isSuccess) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    VirtualFile vDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetDirectory);
                    if (vDir != null) {
                        vDir.refresh(false, true);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Failed to import log archive", e);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isSuccess) {
            try {
                if (toolboxProject != null) {
                    Message message = toolboxProject.newMessage();
                    message.setTitle("Import Logs");
                    message.append("Log archive imported successfully.");
                    message.showInformation();
                }
                if (onComplete != null) {
                    onComplete.run();
                }
                VirtualFile vDir = VfsUtil.findFileByIoFile(targetDirectory, true);
                selectInProjectView(vDir);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            if (toolboxProject != null) {
                Message message = toolboxProject.newMessage();
                message.setTitle("Import Logs");
                message.append("Failed to import log archive. See logs for details.");
                message.showError();
            }
        }
    }
}
