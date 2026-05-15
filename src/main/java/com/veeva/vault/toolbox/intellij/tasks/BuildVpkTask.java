package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.config.VaultPackage;
import com.veeva.vault.toolbox.intellij.settings.AppSettings;
import com.veeva.vault.toolbox.intellij.ui.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Builds a VPK from a manifest file and, optionally, immediately deploys the resulting
 * package to the connected vault. The build output is placed in the manifest project's
 * own {@code packages/} and {@code build/} folders.
 */
public class BuildVpkTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(BuildVpkTask.class);
    private static final Integer DEFAULT_VAULT_ID = 1;

    private final VirtualFile virtualFile;
    private final boolean deploy;
    private final Runnable onComplete;

    /**
     * @param project     the IntelliJ project, may be {@code null}
     * @param virtualFile the package manifest JSON file
     * @param deploy      {@code true} to deploy the resulting VPK after building
     */
    public BuildVpkTask(@Nullable Project project, @NotNull VirtualFile virtualFile, boolean deploy) {
        this(project, virtualFile, deploy, null);
    }

    /**
     * @param project     the IntelliJ project, may be {@code null}
     * @param virtualFile the package manifest JSON file
     * @param deploy      {@code true} to deploy the resulting VPK after building
     * @param onComplete  optional callback invoked after a successful build
     */
    public BuildVpkTask(@Nullable Project project, @NotNull VirtualFile virtualFile, boolean deploy, Runnable onComplete) {
        super(project, "Building VPK");
        this.virtualFile = virtualFile;
        this.deploy = deploy;
        this.onComplete = onComplete;
    }

    /**
     * Executes the VPK build process.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            createVpk();
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Builds a VPK from a manifest file and optionally triggers its deployment.
     */
    private void createVpk() {
        try {
            File manifestFile = new File(virtualFile.getPath());

            String username = AppSettings.getInstance().getState().username;
            Integer vaultId = DEFAULT_VAULT_ID;
            if (username == null) {
                if (toolboxProject.prepareRequest()) {
                    username = toolboxProject.getVaultUser().getUserName();
                    vaultId = toolboxProject.getVaultId();
                }
            }

            if (username == null || vaultId == null) {
                return;
            }

            File packagesDir = manifestFile.getParentFile();
            File projectDir = packagesDir.getParentFile();

            VaultPackage vaultPackage = new VaultPackage(toolboxProject.getVaultClient());
            File finalVpkFile = vaultPackage.buildFromManifest(
                    manifestFile,
                    projectDir,
                    new File(toolboxProject.getProject().getBasePath()),
                    username,
                    vaultId);

            if (deploy && finalVpkFile != null && finalVpkFile.exists()) {
                VirtualFile virtualVpk = VfsUtil.findFileByIoFile(finalVpkFile, true);
                DeployVpkTask task = new DeployVpkTask(toolboxProject.getProject(), virtualVpk);
                task.queue();
            }

            VirtualFile vProjectDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectDir);
            if (vProjectDir != null) {
                vProjectDir.refresh(false, true);
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Displays success message and refreshes project view on successful build.
     */
    @Override
    public void onSuccess() {
        super.onSuccess();
        try {
            if (toolboxProject != null) {
                Message message = toolboxProject.newMessage();
                message.setTitle("VPK");
                message.append("VPK Created");
                message.showInformation();

                if (onComplete != null) {
                    onComplete.run();
                }
                selectInProjectView(virtualFile.getParent());
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
