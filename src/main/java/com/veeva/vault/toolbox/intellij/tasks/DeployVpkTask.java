package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.toolbox.core.results.DeploymentResult;
import com.veeva.vault.toolbox.core.results.ProgressResult;
import com.veeva.vault.toolbox.core.config.VaultPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.function.Consumer;

public class DeployVpkTask extends ToolboxModalTask {
    private static final Logger logger = LoggerFactory.getLogger(DeployVpkTask.class);
    private final VirtualFile virtualFile;
    DeploymentResult deploymentResult;

    public DeployVpkTask(@Nullable Project project, @NotNull VirtualFile virtualFile) {
        super(project, "Deploying VPK", true);
        this.virtualFile = virtualFile;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            Consumer<ProgressResult> changeProgress = progressMessage -> {
                if (indicator.isCanceled()) {
                    throw new RuntimeException("Deployment tracking cancelled by user.");
                }
                indicator.setText(progressMessage.getLabel());
            };

            VaultPackage vaultPackage = new VaultPackage(toolboxProject.getVaultClient());
            deploymentResult = vaultPackage.deployPackage(new File(virtualFile.getPath()), changeProgress);
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        try {
            if (toolboxProject != null && deploymentResult != null) {

                Message message = toolboxProject.newMessage();

                for (String error : deploymentResult.getErrorMessages()) {
                    message.append(error, true);
                }

                for (String warn : deploymentResult.getWarnMessages()) {
                    message.append(warn, true);
                }

                for (String info : deploymentResult.getInfoMessages()) {
                    message.append(info, true);
                }

                if (deploymentResult.isError() || !deploymentResult.getErrorMessages().isEmpty()) {
                    message.setTitle("Deployment Error");
                    message.showError();
                } else if (deploymentResult.isWarning() || !deploymentResult.getWarnMessages().isEmpty()) {
                    message.setTitle("Deployment Warning");
                    message.showWarning();
                } else {
                    message.setTitle("Deployment Complete");
                    message.showInformation();
                }
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}