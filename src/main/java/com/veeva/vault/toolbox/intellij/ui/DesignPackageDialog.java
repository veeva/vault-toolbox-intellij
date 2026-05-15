package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTabbedPane;
import com.veeva.vault.toolbox.core.config.VaultPackage;
import com.veeva.vault.toolbox.core.models.VpkBuildManifest;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.services.Sdk;
import com.veeva.vault.toolbox.intellij.settings.AppSettings;
import org.jdesktop.swingx.JXComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Dialog for designing and configuring a Vault VPK package.
 * Allows managing package details, Java SDK inclusion, MDL components, and Web SDK distributions.
 */
public class DesignPackageDialog extends DialogWrapper {
    private static final Logger logger = LoggerFactory.getLogger(DesignPackageDialog.class);
    private static final String DEFAULT_JAVA_PATH = "/src/main/java/com/veeva/vault/custom";

    private final ToolboxProject toolboxProject;
    private File manifestFile;
    private VpkBuildManifest buildManifest = null;

    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private final JPanel detailsTab = new JPanel(new GridLayout(12, 2));
    private final JPanel javaSdkTab = new JPanel(new GridLayout(12, 2));
    private final JPanel componentsTab = new JPanel(new BorderLayout());
    private final JPanel webSdkTab = new JPanel(new BorderLayout());

    private final JTextField nameField = new JTextField(15);
    private final JTextField authorField = new JTextField(15);
    private final JTextField summaryField = new JTextField(15);
    private final JTextField descriptionField = new JTextField(15);

    private final JCheckBox includeSdk = new JCheckBox();
    private final TextFieldWithBrowseButton sdkPath = new TextFieldWithBrowseButton();
    private final JXComboBox deploymentOptions = new JXComboBox();
    private final JBTabbedPane optionsTabbedPane = new JBTabbedPane();

    /**
     * Creates a new dialog for designing a new package.
     *
     * @param toolboxProject The toolbox project context.
     */
    public DesignPackageDialog(ToolboxProject toolboxProject) {
        this(toolboxProject, null);
    }

    /**
     * Creates a new dialog for editing an existing package manifest.
     *
     * @param toolboxProject The toolbox project context.
     * @param manifestFile   The existing manifest file.
     */
    public DesignPackageDialog(ToolboxProject toolboxProject, File manifestFile) {
        super(toolboxProject.getProject(), false);
        this.toolboxProject = toolboxProject;
        this.manifestFile = manifestFile;
        this.setModal(true);
        this.setUndecorated(true);
        this.setResizable(false);
        init();
    }

    /**
     * @return The selected Java SDK deployment option.
     */
    public VaultPackage.JavaSdk.DeploymentOption getJavaSdkDeploymentOption() {
        return (VaultPackage.JavaSdk.DeploymentOption) deploymentOptions.getSelectedItem();
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
        } else {
            buildManifest = new VpkBuildManifest();
            buildManifest.setName(toolboxProject.getProject().getName());
            buildManifest.setSummary("Package Summary");
            buildManifest.setDescription("Package Description");
        }

        detailsTab.add(new JLabel("Name:"));
        nameField.setText(buildManifest.getName() != null ? buildManifest.getName().toUpperCase() : "");
        detailsTab.add(nameField);

        detailsTab.add(new JLabel("Author:"));
        String currentAuthor = "";

        if (buildManifest.getAuthor() != null && !buildManifest.getAuthor().trim().isEmpty()) {
            currentAuthor = buildManifest.getAuthor();
        } else {
            if (toolboxProject.getVaultUser() != null && toolboxProject.getVaultUser().getUserName() != null) {
                currentAuthor = toolboxProject.getVaultUser().getUserName();
            } else {
                String appUser = AppSettings.getInstance().getState().username;
                if (appUser != null && !appUser.trim().isEmpty()) {
                    currentAuthor = appUser;
                }
            }
        }

        authorField.setText(currentAuthor);
        detailsTab.add(authorField);

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
        VirtualFile projectFile = VfsUtil.findFileByIoFile(new File(toolboxProject.getProject().getBasePath()), true);
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

        includeSdk.addActionListener(e -> toggleControls());
        toggleControls();

        return mainPanel;
    }

    private List<VirtualFile> getSdkFolders(VirtualFile parent) {
        return Sdk.getSdkFolders(toolboxProject, parent);
    }

    /**
     * Toggles the availability of SDK-related controls based on whether the "Include Java SDK" checkbox is selected.
     */
    private void toggleControls() {
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
        } else {
            buildManifest.setJavaSdk(null);
            sdkPath.setText("");
            deploymentOptions.setSelectedIndex(-1);
        }
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        this.setOKButtonText("Save");
        return new Action[] { getOKAction(), getCancelAction() };
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[] {  };
    }

    /**
     * Persists the current dialog configuration to the VPK manifest file.
     */
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
            buildManifest.setAuthor(authorField.getText());
            buildManifest.setSummary(summaryField.getText());
            buildManifest.setDescription(descriptionField.getText());
            buildManifest.setPackageType(VaultPackage.PackageType.MIGRATION);

            if (includeSdk.isSelected()) {
                if (buildManifest.getJavaSdk() == null) {
                    buildManifest.setJavaSdk(new VpkBuildManifest.JavaSdk());
                }
                buildManifest.getJavaSdk().setPath(sdkPath.getText());
                buildManifest.getJavaSdk().setDeploymentOption(getJavaSdkDeploymentOption());
            } else {
                buildManifest.setJavaSdk(null);
            }

            buildManifest.save(manifestFile);

            VirtualFile vLogsDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(toolboxProject.getVpkDirectory());
            if (vLogsDir != null) {
                vLogsDir.refresh(false, true);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
