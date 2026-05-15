package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.config.VaultPackage;
import com.veeva.vault.toolbox.core.results.DeploymentResult;
import com.veeva.vault.toolbox.core.results.ProgressResult;
import com.veeva.vault.toolbox.intellij.ui.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.function.Consumer;

/**
 * Deploys a Vault package (VPK) to the connected vault and surfaces the resulting
 * info, warning, and error messages in a UI dialog.
 */
public class DeployVpkTask extends ToolboxModalTask {
    private static final Logger logger = LoggerFactory.getLogger(DeployVpkTask.class);

    private final VirtualFile virtualFile;
    private DeploymentResult deploymentResult;

    /**
     * @param project     the IntelliJ project, may be {@code null}
     * @param virtualFile the VPK file to deploy
     */
    public DeployVpkTask(@Nullable Project project, @NotNull VirtualFile virtualFile) {
        super(project, "Deploying VPK", true);
        this.virtualFile = virtualFile;
    }

    /**
     * Deploys the VPK in a background thread and tracks progress.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            if (toolboxProject.isProductionVault()) {
                Message message = toolboxProject.newMessage();
                message.append("This tool cannot be run in a Production domain.");
                message.showError();
                return;
            }
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

    /**
     * Aggregates and displays the deployment result (info, warnings, errors) in a UI dialog.
     */
    @Override
    public void onSuccess() {
        super.onSuccess();
        try {
            if (toolboxProject == null || deploymentResult == null) {
                return;
            }
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
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
