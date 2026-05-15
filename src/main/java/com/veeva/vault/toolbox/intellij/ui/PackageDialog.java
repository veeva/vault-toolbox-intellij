package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.BuildVpkTask;
import com.veeva.vault.toolbox.intellij.tasks.DeployVpkTask;
import org.jdesktop.swingx.JXComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Dialog for selecting and performing actions on Vault VPK packages.
 * Supports designing new packages, building VPK files, and deploying them to Vault.
 */
public class PackageDialog extends DialogWrapper {
    public enum ActionType {
        DESIGN,
        BUILD,
        BUILD_DEPLOY,
        DEPLOY
    }
    private static final Logger logger = LoggerFactory.getLogger(PackageDialog.class);

    private final ToolboxProject toolboxProject;
    private ActionType actionType;

    private final JPanel mainPanel = new JPanel(new GridLayout(3, 1));
    private final JXComboBox packageFiles = new JXComboBox();

    /**
     * Initializes a package dialog with default settings.
     *
     * @param toolboxProject The toolbox project context.
     */
    public PackageDialog(ToolboxProject toolboxProject) {
        super(toolboxProject.getProject(), false);
        this.toolboxProject = toolboxProject;
        this.setModal(true);
        this.setUndecorated(true);
        this.setResizable(false);
        init();
    }

    /**
     * Initializes a package dialog for a specific action type.
     *
     * @param toolboxProject The toolbox project context.
     * @param actionType     The type of package action to perform.
     */
    public PackageDialog(ToolboxProject toolboxProject, ActionType actionType) {
        super(toolboxProject.getProject(), false);
        this.toolboxProject = toolboxProject;
        this.actionType = actionType;
        this.setModal(true);
        this.setUndecorated(true);
        this.setResizable(false);
        init();
    }

    /**
     * Data class for entries in the package selection dropdown.
     */
    public static class PackageItem {
        private final String path;
        private final String name;
        public PackageItem(String path, String name) {
            this.path = path;
            this.name = name;
        }
        public String getPath() {
            return path;
        }
        public String getName() {
            return name;
        }
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * @return The absolute path of the currently selected package file.
     */
    public String getSelectedPath() {
        if (packageFiles.getSelectedItem() != null) {
            PackageItem item = (PackageItem)packageFiles.getSelectedItem();
            if (item != null) {
                return item.getPath();
            }
        }
        return null;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        return super.doValidate();
    }

    /**
     * Performs the selected package action upon dialog confirmation.
     */
    @Override
    protected void doOKAction() {
        if (toolboxProject != null && packageFiles.getSelectedItem() != null) {
            switch (actionType) {
                case DESIGN: {
                    String path = getSelectedPath();
                    if (path != null) {
                        DesignPackageDialog designPackageDialog = new DesignPackageDialog(toolboxProject, new File(getSelectedPath()));
                        designPackageDialog.show();
                    } else {
                        DesignPackageDialog designPackageDialog = new DesignPackageDialog(toolboxProject, null);
                        designPackageDialog.show();
                    }
                    break;
                }
                case BUILD: {
                    buildPackage(false);
                    break;
                }
                case BUILD_DEPLOY: {
                    buildPackage(true);
                    break;
                }
                case DEPLOY: {
                    deployPackage();
                    break;
                }
            }
            super.doOKAction();
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        File packageManifestDirectory = new File(toolboxProject.getVpkDirectory(), "packages");
        if (packageManifestDirectory.exists() && packageManifestDirectory.isDirectory()) {
            File[] files = packageManifestDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (actionType.equals(ActionType.DEPLOY)) {
                        if (file.getName().endsWith(".vpk")) {
                            PackageItem packageManifestItem = new PackageItem(file.getPath(), file.getName());
                            packageFiles.addItem(packageManifestItem);
                        }
                    } else {
                        if (file.getName().endsWith(".json")) {
                            PackageItem packageManifestItem = new PackageItem(file.getPath(), file.getName());
                            packageFiles.addItem(packageManifestItem);
                        }
                    }
                }
            }
        }

        if (actionType.equals(ActionType.DESIGN)) {
            PackageItem packageManifestItem = new PackageItem(null, "+ new package");
            packageFiles.addItem(packageManifestItem);

            if (packageFiles.getItemCount() > 0) {
                packageFiles.addActionListener(e -> toggleEditButton());
            }
        }

        if (packageFiles.getItemCount() > 0) {
            mainPanel.add(new JLabel("  Select Package:"));
            mainPanel.add(packageFiles);
        } else {
            if (!actionType.equals(ActionType.DESIGN)) {
                mainPanel.add(new JLabel("No packages found"));
            }
        }

        return mainPanel;
    }

    /**
     * Updates the OK button text based on whether a new package is being created or an existing one edited.
     */
    private void toggleEditButton() {
        if (getSelectedPath() == null) {
            setOKButtonText("Create");
        } else {
            setOKButtonText("Edit");
        }
    }

    /**
     * Configures the dialog actions based on the current action type and package availability.
     *
     * @return The array of actions for the dialog.
     */
    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();

        if (packageFiles.getItemCount() > 0) {
            switch (actionType) {
                case DESIGN: {
                    toggleEditButton();
                    break;
                }
                case BUILD: {
                    this.setOKButtonText("Build");
                    break;
                }
                case BUILD_DEPLOY: {
                    this.setOKButtonText("Build and Deploy");
                    break;
                }
                case DEPLOY: {
                    this.setOKButtonText("Deploy");
                    break;
                }
            }
            return new Action[] { getOKAction(), getCancelAction() };
        } else {
            return new Action[] { getCancelAction() };
        }
    }

    /**
     * Returns an empty set of actions for the left side of the dialog footer.
     *
     * @return An empty array of Actions.
     */
    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[] {  };
    }

    /**
     * Initiates a build task for the selected package.
     *
     * @param deploy true to automatically deploy after building.
     */
    private void buildPackage(boolean deploy) {
        try {
            ApplicationManager.getApplication().invokeLater(() -> {
                VirtualFile virtualFile = VfsUtil.findFileByIoFile(new File(getSelectedPath()), true);
                BuildVpkTask task = new BuildVpkTask(toolboxProject.getProject(), virtualFile, deploy);
                task.queue();
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Initiates a deployment task for the selected package.
     */
    private void deployPackage() {
        try {
            ApplicationManager.getApplication().invokeLater(() -> {
                VirtualFile virtualFile = VfsUtil.findFileByIoFile(new File(getSelectedPath()), true);
                DeployVpkTask task = new DeployVpkTask(toolboxProject.getProject(), virtualFile);
                task.queue();
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
