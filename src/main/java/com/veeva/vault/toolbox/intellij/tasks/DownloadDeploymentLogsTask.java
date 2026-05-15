package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.deployment.DeploymentLogDownloader;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Downloads deployment logs for a previously deployed Vault package and writes them
 * to a {@code deployment/{packageName}.{packageId}} subdirectory of the project logs folder.
 */
public class DownloadDeploymentLogsTask extends ToolboxTask {
    private final String packageId;
    private final String packageName;
    private File outputDirectory;

    /**
     * @param project     the IntelliJ project, may be {@code null}
     * @param packageId   the id of the package whose deployment logs should be downloaded
     * @param packageName the human-readable package name, used in the log path and title
     */
    public DownloadDeploymentLogsTask(@Nullable Project project, String packageId, String packageName) {
        super(project, "Download Deployment Logs for " + packageName);
        this.packageId = packageId;
        this.packageName = packageName;
    }

    /**
     * Fetches deployment logs for a specific package and refreshes the VFS.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        ToolboxProject toolboxProject = ToolboxProject.getInstance(getProject());
        if (!toolboxProject.isConnected()) {
            indicator.setText("Not connected to Vault.");
            return;
        }

        outputDirectory = new File(toolboxProject.getLogsDirectory(), "deployment/" + packageName + "." + packageId);

        DeploymentLogDownloader downloader = new DeploymentLogDownloader();
        downloader.downloadLogs(toolboxProject.getVaultClient(), packageId, outputDirectory,
                result -> indicator.setText(result.getLabel()));

        refreshVfs(outputDirectory);
    }

    /**
     * Selects the output directory in the project view.
     */
    @Override
    public void onSuccess() {
        super.onSuccess();
        VirtualFile vDir = VfsUtil.findFileByIoFile(outputDirectory, true);
        selectInProjectView(vDir);
    }

    /**
     * Refreshes the IntelliJ virtual file system so the newly downloaded logs are visible.
     * Falls back to refreshing the parent directory if the target folder is not yet known
     * to the VFS.
     *
     * @param outputDirectory the directory to refresh
     */
    private static void refreshVfs(File outputDirectory) {
        VirtualFile vf = VfsUtil.findFileByIoFile(outputDirectory, true);
        if (vf != null) {
            vf.refresh(false, true);
            return;
        }
        VirtualFile parentVf = VfsUtil.findFileByIoFile(outputDirectory.getParentFile(), true);
        if (parentVf != null) {
            parentVf.refresh(false, true);
        }
    }
}
