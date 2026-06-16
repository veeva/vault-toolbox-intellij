package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.toolbox.intellij.credentials.BasicAuth;
import com.veeva.vault.toolbox.intellij.credentials.VaultCredentialManager;
import com.veeva.vault.toolbox.intellij.settings.AppSettings;
import com.veeva.vault.toolbox.intellij.settings.SavedCredential;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.settings.Vault;
import com.veeva.vault.toolbox.intellij.settings.VaultSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dialog for comparing two Vault environments.
 */
public class CompareEnvironmentsDialog extends DialogWrapper {

    /**
     * Types of comparisons available.
     */
    public enum ComparisonType {
        MDL("MDL (Component Definitions)"),
        SDK("SDK (Source Code)");

        private final String label;

        /**
         * Constructs a ComparisonType.
         *
         * @param label the display label
         */
        ComparisonType(String label) {
            this.label = label;
        }

        /**
         * Returns the display label.
         *
         * @return the display label
         */
        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Narrows MDL results by Vault naming convention.
     * Custom components end in {@code __c}; standard components end in {@code __v};
     * system components end in {@code __sys}.
     */
    public enum MdlFilter {
        ALL("All"),
        CUSTOM("Custom (__c)"),
        STANDARD("Standard (__v)"),
        SYSTEM("System (__sys)"),
        OTHER("Other (__vs, __rim, …)");

        private final String label;

        /**
         * Constructs an MdlFilter.
         *
         * @param label the display label
         */
        MdlFilter(String label) {
            this.label = label;
        }

        /**
         * Returns the display label.
         *
         * @return the display label
         */
        @Override
        public String toString() {
            return label;
        }

        /**
         * Tests whether the given component name matches this filter.
         *
         * @param componentName the name to test
         * @return true if accepted, false otherwise
         */
        public boolean accepts(String componentName) {
            if (componentName == null) return this == ALL;
            return switch (this) {
                case CUSTOM -> componentName.endsWith("__c");
                case STANDARD -> componentName.endsWith("__v");
                case SYSTEM -> componentName.endsWith("__sys");
                case OTHER -> !componentName.endsWith("__c")
                        && !componentName.endsWith("__v")
                        && !componentName.endsWith("__sys");
                case ALL -> true;
            };
        }
    }

    private static final int AUTH_TAB_BASIC = 0;
    private static final int AUTH_TAB_SESSION = 1;

    private final ToolboxProject toolboxProject;

    private JCheckBox mdlCheckBox;
    private JCheckBox sdkCheckBox;

    private JComboBox<String> targetDnsCombo;
    private JBTabbedPane authTabs;
    private JBTextField usernameField;
    private JBPasswordField passwordField;
    private JBPasswordField sessionIdField;
    private JBLabel credStatusLabel;

    /**
     * Constructs the dialog.
     *
     * @param toolboxProject the current toolbox project
     */
    public CompareEnvironmentsDialog(ToolboxProject toolboxProject) {
        super(toolboxProject.getProject(), false);
        this.toolboxProject = toolboxProject;
        setTitle("Compare Vault Environments");
        setModal(true);
        init();
    }

    /**
     * Creates the center panel of the dialog.
     *
     * @return the center panel component
     */
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        VaultSettings vaultSettings = VaultSettings.getInstance(toolboxProject.getProject());
        String activeDns = toolboxProject.getVaultDNS();

        mdlCheckBox = new JCheckBox("MDL (Component Definitions)", true);
        sdkCheckBox = new JCheckBox("SDK (Source Code)", false);
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        typePanel.add(mdlCheckBox);
        typePanel.add(sdkCheckBox);

        List<String> otherVaults = vaultSettings.getVaults().values().stream()
                .map(Vault::getVaultDNS)
                .filter(dns -> dns != null && !dns.equalsIgnoreCase(activeDns))
                .sorted()
                .collect(Collectors.toList());

        targetDnsCombo = new JComboBox<>();
        targetDnsCombo.setEditable(true);
        for (String dns : otherVaults) {
            targetDnsCombo.addItem(dns);
        }

        credStatusLabel = new JBLabel("");
        credStatusLabel.setForeground(JBColor.namedColor("Label.infoForeground", JBColor.GRAY));

        usernameField = new JBTextField(28);
        passwordField = new JBPasswordField();
        sessionIdField = new JBPasswordField();

        JPanel basicPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Username:", usernameField)
                .addLabeledComponent("Password:", passwordField)
                .getPanel();

        JPanel sessionPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Session ID:", sessionIdField)
                .getPanel();

        authTabs = new JBTabbedPane();
        authTabs.addTab("Basic Auth", basicPanel);
        authTabs.addTab("Session ID", sessionPanel);

        targetDnsCombo.addActionListener(e -> loadSavedCredentials());
        Component editorComp = targetDnsCombo.getEditor().getEditorComponent();
        if (editorComp instanceof JTextField textField) {
            textField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { loadSavedCredentials(); }
                @Override public void removeUpdate(DocumentEvent e) { loadSavedCredentials(); }
                @Override public void changedUpdate(DocumentEvent e) { loadSavedCredentials(); }
            });
        }

        JPanel mainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Compare:", typePanel)
                .addSeparator(8)
                .addLabeledComponent("Source Vault:", new JBLabel(activeDns != null ? activeDns : "(none)"))
                .addLabeledComponent("Target Vault:", targetDnsCombo)
                .addComponent(credStatusLabel)
                .addSeparator(4)
                .addComponent(authTabs)
                .addVerticalGap(4)
                .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(8));

        if (targetDnsCombo.getItemCount() > 0) {
            loadSavedCredentials();
        }

        return mainPanel;
    }

    /**
     * Loads saved credentials for the target vault if available.
     */
    private void loadSavedCredentials() {
        String dns = getTargetVaultDns();
        if (dns == null || dns.isBlank()) {
            credStatusLabel.setText("");
            return;
        }

        SavedCredential cred = AppSettings.findCredentialByDns(dns);
        if (cred == null) {
            credStatusLabel.setText("");
            return;
        }
        if (cred.authenticationType == Vault.AuthenticationType.BASIC) {
            BasicAuth saved = VaultCredentialManager.getUsernamePasswordById(cred.id);
            if (saved != null && saved.getUsername() != null) {
                usernameField.setText(saved.getUsername());
                if (saved.getPassword() != null) passwordField.setText(saved.getPassword());
                authTabs.setSelectedIndex(AUTH_TAB_BASIC);
                credStatusLabel.setText("Saved credentials loaded");
                return;
            }
        } else {
            String sessionId = VaultCredentialManager.getSessionIdById(cred.id);
            if (sessionId != null && !sessionId.isBlank()) {
                sessionIdField.setText(sessionId);
                authTabs.setSelectedIndex(AUTH_TAB_SESSION);
                credStatusLabel.setText("Saved session ID loaded");
                return;
            }
        }
        credStatusLabel.setText("");
    }

    /**
     * Creates the dialog actions.
     *
     * @return an array of actions
     */
    @NotNull
    @Override
    protected Action[] createActions() {
        setOKButtonText("Compare");
        return new Action[]{getOKAction(), getCancelAction()};
    }

    /**
     * Creates the left-side dialog actions.
     *
     * @return an array of actions
     */
    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[]{};
    }

    /**
     * Validates the dialog form inputs.
     *
     * @return a ValidationInfo object if invalid, null if valid
     */
    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (!mdlCheckBox.isSelected() && !sdkCheckBox.isSelected()) {
            return new ValidationInfo("Please select at least one comparison type.", mdlCheckBox);
        }
        String targetDns = getTargetVaultDns();
        if (targetDns == null || targetDns.isBlank()) {
            return new ValidationInfo("Please enter a target Vault DNS.", targetDnsCombo);
        }
        if (targetDns.equalsIgnoreCase(toolboxProject.getVaultDNS())) {
            return new ValidationInfo("Target vault must differ from the source vault.", targetDnsCombo);
        }
        if (authTabs.getSelectedIndex() == AUTH_TAB_BASIC) {
            if (usernameField.getText().isBlank()) {
                return new ValidationInfo("Username is required for Basic Auth.", usernameField);
            }
            if (passwordField.getPassword().length == 0) {
                return new ValidationInfo("Password is required for Basic Auth.", passwordField);
            }
        } else {
            if (sessionIdField.getPassword().length == 0) {
                return new ValidationInfo("Session ID is required.", sessionIdField);
            }
        }
        return null;
    }

    /**
     * Gets the selected comparison types.
     *
     * @return the set of comparison types
     */
    public Set<ComparisonType> getComparisonTypes() {
        Set<ComparisonType> types = EnumSet.noneOf(ComparisonType.class);
        if (mdlCheckBox.isSelected()) types.add(ComparisonType.MDL);
        if (sdkCheckBox.isSelected()) types.add(ComparisonType.SDK);
        return types;
    }

    /**
     * Gets the target Vault DNS.
     *
     * @return the target DNS string
     */
    public String getTargetVaultDns() {
        Object item = targetDnsCombo.getEditor().getItem();
        return item instanceof String s ? s.trim() : null;
    }

    /**
     * Gets the selected authentication type.
     *
     * @return the authentication type
     */
    public Vault.AuthenticationType getAuthType() {
        return authTabs.getSelectedIndex() == AUTH_TAB_BASIC
                ? Vault.AuthenticationType.BASIC
                : Vault.AuthenticationType.SESSION_ID;
    }

    /**
     * Gets the target username.
     *
     * @return the target username
     */
    public String getTargetUsername() {
        return usernameField.getText();
    }

    /**
     * Gets the target password.
     *
     * @return the target password
     */
    public String getTargetPassword() {
        return new String(passwordField.getPassword());
    }

    /**
     * Gets the target session ID.
     *
     * @return the target session ID
     */
    public String getTargetSessionId() {
        return new String(sessionIdField.getPassword());
    }
}
