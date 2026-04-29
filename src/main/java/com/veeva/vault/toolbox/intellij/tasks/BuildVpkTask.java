package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
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

public class BuildVpkTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(BuildVpkTask.class);
    private final VirtualFile virtualFile;
    private final boolean deploy;
    private final Runnable onComplete;

    public BuildVpkTask(@Nullable Project project,
                        @NotNull VirtualFile virtualFile, boolean deploy) {
        this(project, virtualFile, deploy, null);
    }

    public BuildVpkTask(@Nullable Project project,
                        @NotNull VirtualFile virtualFile, boolean deploy, Runnable onComplete) {
        super(project, "Building VPK");
        this.virtualFile = virtualFile;
        this.deploy = deploy;
        this.onComplete = onComplete;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            createVpk();
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void createVpk() {
        try {

            File manifestFile = new File(virtualFile.getPath());

            String username = AppSettings.getInstance().getState().username;
            Integer vaultId = 1;
            if (username == null) {
                if (toolboxProject.prepareRequest()) {
                    username = toolboxProject.getVaultUser().getUserName();
                    vaultId = toolboxProject.getVaultId();
                }
            }

            if (username != null && vaultId != null) {
                // 1. Get the Project Root (e.g., toolbox/vpk/VSDK-HELLOWORLD)
                // manifestFile is at: VSDK-HELLOWORLD/packages/VSDK-HELLOWORLD.json
                File packagesDir = manifestFile.getParentFile();
                File projectDir = packagesDir.getParentFile();

                // 2. Pass Project Root to the engine. It will natively put the VPK in projectDir/packages/
                // and the build files in projectDir/build/!
                VaultPackage vaultPackage = new VaultPackage(toolboxProject.getVaultClient());
                File finalVpkFile = vaultPackage.buildFromManifest(
                        manifestFile, projectDir,
                        new File(toolboxProject.getProject().getBasePath()),
                        username,
                        vaultId);

                // 3. Deploy using the natively placed VPK
                if (deploy && finalVpkFile != null && finalVpkFile.exists()) {
                    VirtualFile virtualVpk = VfsUtil.findFileByIoFile(finalVpkFile, true);
                    DeployVpkTask task = new DeployVpkTask(toolboxProject.getProject(), virtualVpk);
                    task.queue();
                }

                // 4. Refresh the Project Root so the VFS sees both the new /packages/ file and the /build/ folder
                com.intellij.openapi.vfs.VirtualFile vLogsDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                        .refreshAndFindFileByIoFile(projectDir);
                if (vLogsDir != null) {
                    vLogsDir.refresh(false, true);
                }
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

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
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}