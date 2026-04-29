package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.deployment.DeploymentLogDownloader;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class DownloadDeploymentLogsTask extends ToolboxTask {
    private final String packageId;
    private final String packageName;

    public DownloadDeploymentLogsTask(@Nullable Project project, String packageId, String packageName) {
        super(project, "Download Deployment Logs for " + packageName);
        this.packageId = packageId;
        this.packageName = packageName;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        ToolboxProject toolboxProject = ToolboxProject.getInstance(getProject());
        if (!toolboxProject.isConnected()) {
            indicator.setText("Not connected to Vault.");
            return;
        }

        File outputDirectory = new File(toolboxProject.getLogsDirectory(), "deployment/" + packageName + "." + packageId);
        
        DeploymentLogDownloader downloader = new DeploymentLogDownloader();
        downloader.downloadLogs(toolboxProject.getVaultClient(), packageId, outputDirectory, result -> {
            indicator.setText(result.getLabel());
        });

        VirtualFile vf = VfsUtil.findFileByIoFile(outputDirectory, true);
        if (vf != null) {
            vf.refresh(false, true);
        } else {
            VirtualFile parentVf = VfsUtil.findFileByIoFile(outputDirectory.getParentFile(), true);
            if (parentVf != null) {
                parentVf.refresh(false, true);
            }
        }
    }
}