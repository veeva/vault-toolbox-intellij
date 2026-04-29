package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.core.config.VaultPackage;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class BuildSdkVpkTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(BuildSdkVpkTask.class);
    private final VirtualFile virtualFile;

    public BuildSdkVpkTask(@Nullable Project project,
                           @NotNull VirtualFile virtualFile) {
        super(project, "Building VPK");
        this.virtualFile = virtualFile;
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

            String packageName = virtualFile.getPath();
            packageName = packageName.substring(packageName.lastIndexOf("/src/main/java/com/veeva/vault/custom"));

            File procectVpkFolder = new File(toolboxProject.getVpkDirectory().getPath(), toolboxProject.getProject().getName());
            File buildDirectory = new File(procectVpkFolder.getPath(), "/build");
            if (buildDirectory.exists()) {
                try {
                    FileUtils.deleteDirectory(buildDirectory);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            VaultPackage vaultPackage = new VaultPackage(toolboxProject.getVaultClient());
            vaultPackage.setName(toolboxProject.getProject().getName().replace(" ", "-").toUpperCase());
            vaultPackage.setSource(new VaultPackage.Source());

            if (toolboxProject.getVaultUser() != null) {
                vaultPackage.getSource().setAuthor(toolboxProject.getVaultUser().getUserName());
                vaultPackage.getSource().setVault(toolboxProject.getVaultId());
            }

            vaultPackage.setJavaSdk(new VaultPackage.JavaSdk());
            vaultPackage.getJavaSdk().setDeploymentOption(VaultPackage.JavaSdk.DeploymentOption.INCREMENTAL);
            vaultPackage.setPackageType(VaultPackage.PackageType.MIGRATION);
            vaultPackage.setDescription("VPK Deployment SDK");
            vaultPackage.setSummary("VPK Deployment SDK");

            vaultPackage.createXmlFile(buildDirectory);

            File componentBuildDirectory = new File(buildDirectory, "/components");
            File deployComponentDirectory = new File(toolboxProject.getMdlDirectory().getPath(), "deploy");
            VirtualFile componentDirectory = VfsUtil.findFileByIoFile(deployComponentDirectory, true);
            copyToBuild(componentBuildDirectory, componentDirectory, "");

            File codeBuildDirectory = new File(buildDirectory, "/javasdk");
            copyToBuild(codeBuildDirectory, virtualFile, packageName);

            File vpkFile = new File(procectVpkFolder.getPath(), "/" + toolboxProject.getProject().getName() + ".vpk");
            vaultPackage.pack(buildDirectory, vpkFile);

            Message message = toolboxProject.newMessage();
            message.append("Created VPK package : " + vaultPackage.getName() + "\n");
            message.showInformation();

        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void copyToBuild(File vpkBuildDirectory, VirtualFile selectedFile, String parentName) {
        try {
            if (selectedFile.isDirectory()) {
                File directory = new File(vpkBuildDirectory, parentName);
                FileIO.makeDirectories(directory);
            }
            for (VirtualFile childFile : selectedFile.getChildren()) {
                if (childFile.isDirectory()) {
                    copyToBuild(vpkBuildDirectory, childFile, parentName + "/" + childFile.getName());
                } else {
                    File targetFile = new File(vpkBuildDirectory, parentName + "/" + childFile.getName());
                    try {
                        FileUtils.writeStringToFile(targetFile, new String(childFile.contentsToByteArray()), "UTF-8");
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
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
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}