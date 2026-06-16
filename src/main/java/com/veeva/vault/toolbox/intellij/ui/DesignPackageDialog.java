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
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
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

    /** The associated toolbox project. */
    private final ToolboxProject toolboxProject;
    
    /** The manifest file for the package. */
    private File manifestFile;
    
    /** The build manifest model. */
    private VpkBuildManifest buildManifest = null;

    /** The main panel of the dialog. */
    private final JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
    
    /** The panel for the details tab. */
    private JPanel detailsTab;
    
    /** The panel for the Java SDK tab. */
    private JPanel javaSdkTab;
    
    /** The panel for the components tab. */
    private final JBPanel<?> componentsTab = new JBPanel<>(new BorderLayout());
    
    /** The panel for the Web SDK tab. */
    private final JBPanel<?> webSdkTab = new JBPanel<>(new BorderLayout());

    /** Text field for the package name. */
    private final JBTextField nameField = new JBTextField();
    
    /** Text field for the package author. */
    private final JBTextField authorField = new JBTextField();
    
    /** Text field for the package summary. */
    private final JBTextField summaryField = new JBTextField();
    
    /** Text field for the package description. */
    private final JBTextField descriptionField = new JBTextField();

    /** Checkbox indicating if the Java SDK should be included. */
    private final JCheckBox includeSdk = new JCheckBox();
    
    /** Field for selecting the Java SDK path. */
    private final TextFieldWithBrowseButton sdkPath = new TextFieldWithBrowseButton();
    
    /** Dropdown for selecting deployment options. */
    private final JXComboBox deploymentOptions = new JXComboBox();
    
    /** The tabbed pane containing configuration options. */
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

    /**
     * Validates the dialog inputs.
     *
     * @return ValidationInfo if validation fails, null otherwise.
     */
    @Override
    protected @Nullable ValidationInfo doValidate() {
        return super.doValidate();
    }

    /**
     * Handles the OK action, persisting changes.
     */
    @Override
    protected void doOKAction() {
        if (toolboxProject != null) {
            save();
            super.doOKAction();
        }
    }

    /**
     * Creates the center panel of the dialog containing the tabs.
     *
     * @return The created center panel.
     */
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        this.mainPanel.setMinimumSize(new Dimension(500, 500));

        if (manifestFile != null && manifestFile.exists()) {
            buildManifest = VpkBuildManifest.load(this.manifestFile);
        } else {
            buildManifest = new VpkBuildManifest();
            buildManifest.setName(toolboxProject.getProject().getName());
            buildManifest.setSummary("Package Summary");
            buildManifest.setDescription("Package Description");
        }

        nameField.setText(buildManifest.getName() != null ? buildManifest.getName().toUpperCase() : "");

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

        summaryField.setText(buildManifest.getSummary());
        descriptionField.setText(buildManifest.getDescription());

        detailsTab = FormBuilder.createFormBuilder()
                .addLabeledComponent("Name:", nameField, 5, true)
                .addLabeledComponent("Author:", authorField, 5, true)
                .addLabeledComponent("Summary:", summaryField, 5, true)
                .addLabeledComponent("Description:", descriptionField, 5, true)
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();
        detailsTab.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        includeSdk.setText("Include Java SDK");
        deploymentOptions.addItem(VaultPackage.JavaSdk.DeploymentOption.REPLACE_ALL);
        deploymentOptions.addItem(VaultPackage.JavaSdk.DeploymentOption.INCREMENTAL);
        deploymentOptions.addItem(VaultPackage.JavaSdk.DeploymentOption.DELETE_ALL);

        javaSdkTab = FormBuilder.createFormBuilder()
                .addComponent(includeSdk, 5)
                .addLabeledComponent("SDK Path:", sdkPath, 5, true)
                .addLabeledComponent("Deployment Options:", deploymentOptions, 5, true)
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();
        javaSdkTab.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        if (buildManifest.getJavaSdk() != null) {
            includeSdk.setSelected(true);
            sdkPath.setText(buildManifest.getJavaSdk().getPath());
            deploymentOptions.setSelectedItem(buildManifest.getJavaSdk().getDeploymentOption());
        }

        componentsTab.add(new ComponentTreePanel(toolboxProject, buildManifest), BorderLayout.CENTER);
        webSdkTab.add(new DistributionTreePanel(toolboxProject, buildManifest), BorderLayout.CENTER);

        optionsTabbedPane.addTab("Details", detailsTab);
        optionsTabbedPane.addTab("Java SDK", javaSdkTab);
        optionsTabbedPane.addTab("Components & Data", componentsTab);
        optionsTabbedPane.addTab("Web SDK", webSdkTab);

        mainPanel.add(optionsTabbedPane);

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

    /**
     * Retrieves the SDK folders for the project.
     *
     * @param parent The parent virtual file.
     * @return A list of virtual files representing the SDK folders.
     */
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

    /**
     * Creates the dialog actions.
     *
     * @return an array of actions
     */
    @NotNull
    @Override
    protected Action[] createActions() {
        this.setOKButtonText("Save");
        return new Action[] { getOKAction(), getCancelAction() };
    }

    /**
     * Creates the left-side dialog actions.
     *
     * @return an array of actions
     */
    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[0];
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
