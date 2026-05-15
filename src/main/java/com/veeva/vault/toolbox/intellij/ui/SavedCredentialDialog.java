package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.toolbox.intellij.credentials.BasicAuth;
import com.veeva.vault.toolbox.intellij.credentials.VaultCredentialManager;
import com.veeva.vault.toolbox.intellij.settings.SavedCredential;
import com.veeva.vault.toolbox.intellij.settings.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for adding or editing a saved credential.
 */
public class SavedCredentialDialog extends DialogWrapper {

    private final SavedCredential credential;
    private final boolean isNew;

    private final JBTextField labelField = new JBTextField(25);
    private final JBTextField dnsField = new JBTextField(25);
    private final JBRadioButton basicAuthButton = new JBRadioButton("Basic");
    private final JBRadioButton sessionAuthButton = new JBRadioButton("Session");
    private final JBTextField usernameField = new JBTextField(25);
    private final JPasswordField passwordField = new JPasswordField(25);
    private final JPasswordField sessionIdField = new JPasswordField(25);
    private final JBCheckBox defaultCheckbox = new JBCheckBox("Set as default credential");

    /**
     * Constructs a dialog for adding or editing a credential.
     *
     * @param parent             the parent component
     * @param existingCredential the existing credential to edit, or null to create a new one
     */
    public SavedCredentialDialog(@Nullable Component parent, @Nullable SavedCredential existingCredential) {
        super(parent, true);
        this.isNew = existingCredential == null;
        this.credential = isNew ? new SavedCredential() : existingCredential;
        setTitle(isNew ? "Add Credential" : "Edit Credential");
        setResizable(false);
        init();
        populateFields();
    }

    /**
     * Populates the dialog fields with the existing credential's data.
     */
    private void populateFields() {
        if (!isNew) {
            labelField.setText(credential.label);
            dnsField.setText(credential.vaultDNS);
            usernameField.setText(credential.username);
            defaultCheckbox.setSelected(credential.isDefault);
            if (credential.authenticationType == Vault.AuthenticationType.SESSION_ID) {
                sessionAuthButton.setSelected(true);
            } else {
                basicAuthButton.setSelected(true);
            }
            updateAuthFields();

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                String password = null;
                String sessionId = null;
                if (credential.authenticationType == Vault.AuthenticationType.BASIC) {
                    BasicAuth auth = VaultCredentialManager.getUsernamePasswordById(credential.id);
                    if (auth != null) password = auth.getPassword();
                } else {
                    sessionId = VaultCredentialManager.getSessionIdById(credential.id);
                }
                final String finalPassword = password;
                final String finalSessionId = sessionId;
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (finalPassword != null) passwordField.setText(finalPassword);
                    if (finalSessionId != null) sessionIdField.setText(finalSessionId);
                }, com.intellij.openapi.application.ModalityState.any());
            });
        } else {
            basicAuthButton.setSelected(true);
            updateAuthFields();
        }
    }

    /**
     * Creates the center panel of the dialog containing the form fields.
     *
     * @return the center panel component
     */
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        ButtonGroup authGroup = new ButtonGroup();
        authGroup.add(basicAuthButton);
        authGroup.add(sessionAuthButton);
        basicAuthButton.addActionListener(e -> updateAuthFields());
        sessionAuthButton.addActionListener(e -> updateAuthFields());

        addVisibilityToggle(passwordField);
        addVisibilityToggle(sessionIdField);

        JPanel authTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        authTypePanel.add(basicAuthButton);
        authTypePanel.add(Box.createHorizontalStrut(8));
        authTypePanel.add(sessionAuthButton);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10, 10, 0, 10));

        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = JBUI.insets(4, 0, 4, 8);
        labelGbc.gridx = 0;

        GridBagConstraints fieldGbc = new GridBagConstraints();
        fieldGbc.fill = GridBagConstraints.HORIZONTAL;
        fieldGbc.weightx = 1.0;
        fieldGbc.insets = JBUI.insets(4, 0, 4, 0);
        fieldGbc.gridx = 1;

        int row = 0;
        addFormRow(panel, labelGbc, fieldGbc, row++, "Label:", labelField);
        addFormRow(panel, labelGbc, fieldGbc, row++, "Vault DNS:", dnsField);
        addFormRow(panel, labelGbc, fieldGbc, row++, "Auth Type:", authTypePanel);
        addFormRow(panel, labelGbc, fieldGbc, row++, "Username:", usernameField);
        addFormRow(panel, labelGbc, fieldGbc, row++, "Password:", passwordField);
        addFormRow(panel, labelGbc, fieldGbc, row++, "Session ID:", sessionIdField);

        GridBagConstraints checkGbc = new GridBagConstraints();
        checkGbc.gridx = 0;
        checkGbc.gridy = row;
        checkGbc.gridwidth = 2;
        checkGbc.anchor = GridBagConstraints.WEST;
        checkGbc.insets = JBUI.insets(8, 0, 4, 0);
        panel.add(defaultCheckbox, checkGbc);

        return panel;
    }

    /**
     * Adds a form row with a label and a field to the specified panel.
     *
     * @param panel    the panel to add the row to
     * @param labelGbc the grid bag constraints for the label
     * @param fieldGbc the grid bag constraints for the field
     * @param row      the row index
     * @param text     the text for the label
     * @param field    the field component
     */
    private void addFormRow(JPanel panel, GridBagConstraints labelGbc, GridBagConstraints fieldGbc,
                            int row, String text, JComponent field) {
        labelGbc.gridy = row;
        panel.add(new JLabel(text), labelGbc);
        fieldGbc.gridy = row;
        panel.add(field, fieldGbc);
    }

    /**
     * Updates the visibility of authentication fields based on the selected authentication type.
     */
    private void updateAuthFields() {
        boolean isBasic = basicAuthButton.isSelected();
        usernameField.setVisible(isBasic);
        passwordField.setVisible(isBasic);
        sessionIdField.setVisible(!isBasic);
        JPanel content = (JPanel) getContentPane();
        if (content != null) {
            content.revalidate();
            content.repaint();
        }
    }

    /**
     * Adds a visibility toggle to the given password field.
     *
     * @param field the password field to add the toggle to
     */
    private void addVisibilityToggle(JPasswordField field) {
        char defaultEchoChar = field.getEchoChar();
        JLabel toggleIcon = new JLabel(AllIcons.Actions.Show);
        toggleIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                field.setEchoChar(field.getEchoChar() == 0 ? defaultEchoChar : (char) 0);
                toggleIcon.repaint();
            }
        });

        field.setBorder(BorderFactory.createCompoundBorder(field.getBorder(), JBUI.Borders.emptyRight(28)));
        field.setLayout(null);
        field.add(toggleIcon);
        field.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                Dimension ps = toggleIcon.getPreferredSize();
                int w = ps.width > 0 ? ps.width : 16;
                int h = ps.height > 0 ? ps.height : 16;
                toggleIcon.setBounds(field.getWidth() - w - 10, (field.getHeight() - h) / 2, w, h);
            }
        });
    }

    /**
     * Validates the form fields.
     *
     * @return a ValidationInfo object if there is a validation error, null otherwise
     */
    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (labelField.getText().trim().isEmpty()) {
            return new ValidationInfo("Label is required", labelField);
        }
        if (dnsField.getText().trim().isEmpty()) {
            return new ValidationInfo("Vault DNS is required", dnsField);
        }
        if (basicAuthButton.isSelected() && usernameField.getText().trim().isEmpty()) {
            return new ValidationInfo("Username is required", usernameField);
        }
        return null;
    }

    /**
     * Performs the OK action, saving the credential data and closing the dialog.
     */
    @Override
    protected void doOKAction() {
        credential.label = labelField.getText().trim();
        credential.vaultDNS = dnsField.getText().trim();
        credential.username = usernameField.getText().trim();
        credential.authenticationType = basicAuthButton.isSelected()
                ? Vault.AuthenticationType.BASIC
                : Vault.AuthenticationType.SESSION_ID;
        credential.isDefault = defaultCheckbox.isSelected();

        String password = String.copyValueOf(passwordField.getPassword());
        String sessionId = String.copyValueOf(sessionIdField.getPassword());

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (credential.authenticationType == Vault.AuthenticationType.BASIC) {
                if (!password.isEmpty()) {
                    VaultCredentialManager.setUsernamePasswordById(credential.id, credential.username, password);
                }
            } else {
                if (!sessionId.isEmpty()) {
                    VaultCredentialManager.setSessionIdById(credential.id, sessionId);
                }
            }
        });

        super.doOKAction();
    }

    /**
     * Creates the actions for the dialog buttons.
     *
     * @return an array of actions
     */
    @NotNull
    @Override
    protected Action[] createActions() {
        setOKButtonText(isNew ? "Add" : "Save");
        return new Action[]{getOKAction(), getCancelAction()};
    }

    /**
     * Creates the actions for the left side of the dialog button panel.
     *
     * @return an array of actions
     */
    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[]{};
    }

    /**
     * Gets the current credential object with the form data.
     *
     * @return the credential object
     */
    public SavedCredential getCredential() {
        return credential;
    }

    /**
     * Gets the plaintext password from the password field.
     *
     * @return the plaintext password
     */
    public String getPlaintextPassword() {
        return String.copyValueOf(passwordField.getPassword());
    }

    /**
     * Gets the plaintext session ID from the session ID field.
     *
     * @return the plaintext session ID
     */
    public String getPlaintextSessionId() {
        return String.copyValueOf(sessionIdField.getPassword());
    }
}
