package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.config.VaultPackage;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.intellij.ui.Message;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Builds a Java SDK VPK from the Java sources rooted at the supplied virtual file.
 * The package is assembled with both the SDK code and any associated MDL components
 * staged in {@code mdl/deploy/}.
 */
public class BuildSdkVpkTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(BuildSdkVpkTask.class);
    private static final String JAVA_SOURCE_ROOT = "/src/main/java/com/veeva/vault/custom";

    private final VirtualFile virtualFile;
    private File vpkOutputFolder;

    /**
     * @param project     the IntelliJ project, may be {@code null}
     * @param virtualFile the root directory containing the Java SDK sources to package
     */
    public BuildSdkVpkTask(@Nullable Project project, @NotNull VirtualFile virtualFile) {
        super(project, "Building VPK");
        this.virtualFile = virtualFile;
    }

    /**
     * Orchestrates the VPK creation process in a background thread.
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
     * Handles the core logic of assembling the VPK, including generating the manifest,
     * staging MDL components, and copying Java SDK source code.
     */
    private void createVpk() {
        try {
            String packageName = virtualFile.getPath();
            packageName = packageName.substring(packageName.lastIndexOf(JAVA_SOURCE_ROOT));

            File projectVpkFolder = new File(toolboxProject.getVpkDirectory().getPath(), toolboxProject.getProject().getName());
            vpkOutputFolder = projectVpkFolder;
            File buildDirectory = new File(projectVpkFolder.getPath(), "/build");
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

            File vpkFile = new File(projectVpkFolder.getPath(), "/" + toolboxProject.getProject().getName() + ".vpk");
            vaultPackage.pack(buildDirectory, vpkFile);

            VirtualFile vProjectVpkFolder = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projectVpkFolder);
            if (vProjectVpkFolder != null) {
                vProjectVpkFolder.refresh(false, true);
            }

            Message message = toolboxProject.newMessage();
            message.append("Created VPK package : " + vaultPackage.getName() + "\n");
            message.showInformation();
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Recursively copies a virtual file tree into the VPK build directory, mirroring
     * its directory structure under {@code parentName}.
     *
     * @param vpkBuildDirectory the destination root within the VPK build folder
     * @param selectedFile      the source file or directory in the IntelliJ VFS
     * @param parentName        the relative path within the destination root
     */
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

    /**
     * Refreshes the project view and shows a success notification when the task completes successfully.
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

                VirtualFile vDir = VfsUtil.findFileByIoFile(vpkOutputFolder, true);
                selectInProjectView(vDir);
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
