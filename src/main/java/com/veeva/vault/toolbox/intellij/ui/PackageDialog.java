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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class PackageDialog extends DialogWrapper {
    public enum ActionType {
        DESIGN,
        BUILD,
        BUILD_DEPLOY,
        DEPLOY,
    }
    private static final Logger logger = LoggerFactory.getLogger(PackageDialog.class);

    ToolboxProject toolboxProject;
    ActionType actionType;

    JPanel mainPanel = new JPanel(new GridLayout(3, 1));
    JXComboBox packageFiles = new JXComboBox();


    public PackageDialog(ToolboxProject toolboxProject) {
        super(false);
        this.toolboxProject = toolboxProject;
        this.setModal(true);
        this.setUndecorated(true);
        this.setResizable(false);
        init();
    }

    public PackageDialog(ToolboxProject toolboxProject, ActionType actionType) {
        super(false);
        this.toolboxProject = toolboxProject;
        this.actionType = actionType;
        this.setModal(true);
        this.setUndecorated(true);
        this.setResizable(false);
        init();
    }

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

    @Override
    protected void doOKAction() {
        if (toolboxProject != null && packageFiles.getSelectedItem() != null) {
            switch (actionType) {
                case DESIGN: {
                    String path = getSelectedPath();
                    if (path != null) {
                        DesignPackageDialog designPackageDialog = new DesignPackageDialog(toolboxProject, new File(getSelectedPath()));
                        designPackageDialog.show();
                    }
                    else {
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
                    }
                    else {
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
            //mainPanel.add(new JButton("Design new package"));

            if (packageFiles.getItemCount() > 0) {
                packageFiles.addActionListener (new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        toggleEditButton();
                    }
                });

            }
        }



        if (packageFiles.getItemCount() > 0) {
            mainPanel.add(new JLabel("  Select Package:"));
            mainPanel.add(packageFiles);
        }
        else {
            if (!actionType.equals(ActionType.DESIGN)) {
                mainPanel.add(new JLabel("No packages found"));
            }
        }

        return mainPanel;
    }

    private void toggleEditButton() {
        if (getSelectedPath() == null) {
            setOKButtonText("Create");
        }
        else {
            setOKButtonText("Edit");
        }
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();

        if (packageFiles.getItemCount() > 0) {
            // return right hand side action buttons
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
        }
        else {
            return new Action[] { getCancelAction() };
        }
    }

    @NotNull
    protected Action[] createLeftSideActions() {
        // return left hand side action buttons
        return new Action[] {  };
    }
    private void buildPackage(boolean deploy) {
        try {

            ApplicationManager.getApplication().invokeLater(() -> {
                VirtualFile virtualFile = VfsUtil.findFileByIoFile(new File(getSelectedPath()), true);
                BuildVpkTask task = new BuildVpkTask(toolboxProject.getProject(), virtualFile, deploy);
                task.queue();
            });
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void deployPackage() {
        try {

            ApplicationManager.getApplication().invokeLater(() -> {
                VirtualFile virtualFile = VfsUtil.findFileByIoFile(new File(getSelectedPath()), true);
                DeployVpkTask task = new DeployVpkTask(toolboxProject.getProject(), virtualFile);
                task.queue();
            });
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}