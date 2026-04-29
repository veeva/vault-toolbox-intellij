package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTabbedPane;
import com.veeva.vault.toolbox.core.config.VaultPackage;
import com.veeva.vault.toolbox.core.models.VpkBuildManifest;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.services.Sdk;
import org.jdesktop.swingx.JXComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class DesignPackageDialog extends DialogWrapper {
    private static final Logger logger = LoggerFactory.getLogger(DesignPackageDialog.class);

    private static final String DEFAULT_JAVA_PATH = "/src/main/java/com/veeva/vault/custom";

    ToolboxProject toolboxProject;
    File manifestFile;
    VpkBuildManifest buildManifest = null;

    JPanel mainPanel = new JPanel(new BorderLayout());
    JPanel detailsTab = new JPanel(new GridLayout(12, 2));
    JPanel javaSdkTab = new JPanel(new GridLayout(12, 2));
    JPanel componentsTab = new JPanel(new BorderLayout());
    JPanel webSdkTab = new JPanel(new BorderLayout());

    JTextField nameField = new JTextField(15);
    JTextField authorField = new JTextField(15); // --- ADDED: Author Field ---
    JTextField summaryField = new JTextField(15);
    JTextField descriptionField = new JTextField(15);

    JCheckBox includeSdk = new JCheckBox();
    TextFieldWithBrowseButton sdkPath = new TextFieldWithBrowseButton();
    JXComboBox deploymentOptions = new JXComboBox();
    JBTabbedPane optionsTabbedPane = new JBTabbedPane();

    public DesignPackageDialog(ToolboxProject toolboxProject) {
        super(false);
        this.toolboxProject = toolboxProject;
        this.setModal(true);
        this.setUndecorated(true);
        this.setResizable(false);
        init();
    }

    public DesignPackageDialog(ToolboxProject toolboxProject, File manifestFile) {
        super(false);
        this.toolboxProject = toolboxProject;
        this.manifestFile = manifestFile;
        this.setModal(true);
        this.setUndecorated(true);
        this.setResizable(false);
        init();
    }

    public VaultPackage.JavaSdk.DeploymentOption getJavaSdkDeploymentOption() {
        VaultPackage.JavaSdk.DeploymentOption item = (VaultPackage.JavaSdk.DeploymentOption)deploymentOptions.getSelectedItem();
        return item;
    }


    @Override
    protected @Nullable ValidationInfo doValidate() {
        return super.doValidate();
    }

    @Override
    protected void doOKAction() {
        if (toolboxProject != null) {
            save();
            super.doOKAction();
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        this.mainPanel.setMinimumSize(new Dimension(500, 500));
        optionsTabbedPane.addTab("Details", detailsTab);
        optionsTabbedPane.addTab("Java SDK", javaSdkTab);
        optionsTabbedPane.addTab("Components & Data", componentsTab);
        optionsTabbedPane.addTab("Web SDK", webSdkTab);

        mainPanel.add(optionsTabbedPane);

        if (manifestFile != null && manifestFile.exists()) {
            buildManifest = VpkBuildManifest.load(this.manifestFile);
        }
        else {
            buildManifest = new VpkBuildManifest();
            buildManifest.setName(toolboxProject.getProject().getName());
            buildManifest.setSummary("Package Summary");
            buildManifest.setDescription("Package Description");
        }

        detailsTab.add(new JLabel("Name:"));
        nameField.setText(buildManifest.getName() != null ? buildManifest.getName().toUpperCase() : "");
        detailsTab.add(nameField);

        // --- ADDED: Author UI Logic ---
        detailsTab.add(new JLabel("Author:"));
        String currentAuthor = "";

        // 1. Try to load existing author from the JSON manifest first
        if (buildManifest.getAuthor() != null && !buildManifest.getAuthor().trim().isEmpty()) {
            currentAuthor = buildManifest.getAuthor();
        } else {
            // 2. Try the active project session (Most reliable if they are currently connected)
            if (toolboxProject.getVaultUser() != null && toolboxProject.getVaultUser().getUserName() != null) {
                currentAuthor = toolboxProject.getVaultUser().getUserName();
            }
            // 3. Fallback to global AppSettings if session isn't active yet
            else {
                String appUser = com.veeva.vault.toolbox.intellij.settings.AppSettings.getInstance().getState().username;
                if (appUser != null && !appUser.trim().isEmpty()) {
                    currentAuthor = appUser;
                }
            }
        }

        authorField.setText(currentAuthor);
        detailsTab.add(authorField);
        // ------------------------------

        detailsTab.add(new JLabel("Summary:"));
        summaryField.setText(buildManifest.getSummary());
        detailsTab.add(summaryField);
        detailsTab.add(new JLabel("Description:"));
        descriptionField.setText(buildManifest.getDescription());
        detailsTab.add(descriptionField);

        includeSdk.setText("Include Java SDK");
        javaSdkTab.add(includeSdk);

        javaSdkTab.add(new JLabel("SDK Path:"));
        javaSdkTab.add(sdkPath);
        javaSdkTab.add(new JLabel("Deployment Options:"));
        javaSdkTab.add(deploymentOptions);

        deploymentOptions.addItem(VaultPackage.JavaSdk.DeploymentOption.REPLACE_ALL);
        deploymentOptions.addItem(VaultPackage.JavaSdk.DeploymentOption.INCREMENTAL);
        deploymentOptions.addItem(VaultPackage.JavaSdk.DeploymentOption.DELETE_ALL);

        if (buildManifest.getJavaSdk() != null) {
            includeSdk.setSelected(true);
            sdkPath.setText(buildManifest.getJavaSdk().getPath());
            deploymentOptions.setSelectedItem(buildManifest.getJavaSdk().getDeploymentOption());
        }

        componentsTab.add(new ComponentTreePanel(toolboxProject, buildManifest), BorderLayout.CENTER);
        webSdkTab.add(new DistributionTreePanel(toolboxProject, buildManifest), BorderLayout.CENTER);

        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor();
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        VirtualFile projectFile = VfsUtil.findFileByIoFile(new File (toolboxProject.getProject().getBasePath()), true);
        fileChooserDescriptor.setRoots(getSdkFolders(projectFile));

        sdkPath.addBrowseFolderListener(toolboxProject.getProject(), fileChooserDescriptor);

        sdkPath.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updatePath(); }
            @Override
            public void removeUpdate(DocumentEvent e) { }
            @Override
            public void changedUpdate(DocumentEvent e) { }

            private void updatePath() {
                String path = sdkPath.getText();
                if (path != null && path.startsWith(toolboxProject.getProject().getBasePath())) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        String newPath = path.replace(toolboxProject.getProject().getBasePath(), "");
                        sdkPath.setText(newPath);
                    });
                }
            }
        });

        includeSdk.addActionListener(e -> toggleConteols());
        toggleConteols();

        return mainPanel;
    }

    private java.util.List<VirtualFile> getSdkFolders(VirtualFile parent) {
        return new Sdk(toolboxProject).getSdkFolders(parent);
    }

    private void toggleConteols() {
        sdkPath.setEnabled(includeSdk.isSelected());
        deploymentOptions.setEnabled(includeSdk.isSelected());
        sdkPath.setEditable(false);
        if (includeSdk.isSelected()) {
            if (buildManifest.getJavaSdk() == null) {
                buildManifest.setJavaSdk(new VpkBuildManifest.JavaSdk());
            }
            if (buildManifest.getJavaSdk().getPath() == null) {
                buildManifest.getJavaSdk().setPath(DEFAULT_JAVA_PATH);
                sdkPath.setText(buildManifest.getJavaSdk().getPath());
            }
            if (buildManifest.getJavaSdk().getDeploymentOption() == null) {
                buildManifest.getJavaSdk().setDeploymentOption(VaultPackage.JavaSdk.DeploymentOption.REPLACE_ALL);
                deploymentOptions.setSelectedItem(buildManifest.getJavaSdk().getDeploymentOption());
            }
        }
        else {
            buildManifest.setJavaSdk(null);
            sdkPath.setText("");
            deploymentOptions.setSelectedIndex(-1);
        }
    }

    public static class PackageItem {
        private final String path;
        private final String name;
        public PackageItem(String path, String name) {
            this.path = path;
            this.name = name;
        }
        public String getPath() { return path; }
        public String getName() { return name; }
        @Override
        public String toString() { return name; }
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();
        this.setOKButtonText("Save");
        return new Action[] { getOKAction(), getCancelAction() };
    }

    @NotNull
    protected Action[] createLeftSideActions() {
        return new Action[] {  };
    }

    private void save() {
        try {
            String name = (nameField.getText() != null) ? nameField.getText().toUpperCase() : null;
            if (manifestFile == null) {
                String packageName = name != null && !name.isEmpty() ? name : "NEW_PACKAGE";
                File projectDir = new File(toolboxProject.getVpkDirectory(), packageName);
                File packagesDir = new File(projectDir, "packages");
                packagesDir.mkdirs();

                manifestFile = new File(packagesDir, packageName + ".json");
            }
            buildManifest.setName(name);
            buildManifest.setAuthor(authorField.getText()); // --- ADDED: Save Author to model ---
            buildManifest.setSummary(summaryField.getText());
            buildManifest.setDescription(descriptionField.getText());
            buildManifest.setPackageType(VaultPackage.PackageType.MIGRATION);

            if (includeSdk.isSelected()) {
                if (buildManifest.getJavaSdk() == null) {
                    buildManifest.setJavaSdk(new VpkBuildManifest.JavaSdk());
                }
                buildManifest.getJavaSdk().setPath(sdkPath.getText());
                buildManifest.getJavaSdk().setDeploymentOption(getJavaSdkDeploymentOption());
            }
            else {
                buildManifest.setJavaSdk(null);
            }

            buildManifest.save(manifestFile);

            com.intellij.openapi.vfs.VirtualFile vLogsDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                    .refreshAndFindFileByIoFile(toolboxProject.getVpkDirectory());
            if (vLogsDir != null) {
                vLogsDir.refresh(false, true);
            }

            ApplicationManager.getApplication().invokeLater(() -> {

            });
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}