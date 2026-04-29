package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.toolbox.intellij.credentials.BasicAuth;
import com.veeva.vault.toolbox.intellij.credentials.VaultCredentialManager;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.settings.AppSettings;
import com.veeva.vault.toolbox.intellij.settings.Vault;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Objects;

import static com.veeva.vault.toolbox.intellij.settings.Vault.AuthenticationType.*;

public class LoginPanel extends JPanel {

    ToolboxProject toolboxProject;
    ToolboxProject.ConnectionResult connectionResult;

    JPanel vaultDnsPanel = new JPanel(new GridLayout(2, 1));
    JPanel basicAuthPanel = new JPanel(new GridLayout(5, 1));

    JPanel sessionAuthPanel = new JPanel(new GridLayout(5, 1));

    JPanel loginPanel = new JPanel(new BorderLayout(0, 10));
    JTextArea messageArea = new JTextArea();
    JBTabbedPane authTabs = new JBTabbedPane();
    ToolboxButton loginButton = new ToolboxButton(toolboxProject, "Login");
    boolean showLoginButton;

    JTextField vaultDnsField = new JTextField(30);
    JTextField usernameField = new JTextField(30);
    JPasswordField passwordField = new JPasswordField(30);
    JBCheckBox savePasswordCheckBox = new JBCheckBox("Save Password");
    JBCheckBox saveSessionIdCheckBox = new JBCheckBox("Save Session ID");
    JPasswordField sessionIdField = new JPasswordField(30);

    public LoginPanel(ToolboxProject toolboxProject, boolean showLoginButton) {
        super(true);
        this.toolboxProject = toolboxProject;
        this.showLoginButton = showLoginButton;
        init();
    }

    public void displayConnectionResults(ToolboxProject.ConnectionResult result) {
        this.connectionResult = result;
        if (connectionResult != null && connectionResult.isFailure()) {
            messageArea.setForeground(JBColor.RED);
            messageArea.setText(connectionResult.getErrorMessage());
        } else {
            resetConnectionResults();
        }
    }

    public void resetConnectionResults() {
        this.connectionResult = null;
        messageArea.setText("");
        messageArea.setForeground(null);
    }

    public static class LoginCredentials {
        public boolean isValid = false;
        public boolean isBasicAuth = true;
        public String vaultDns;
        public String username;
        public String password;
        public String sessionId;
        public boolean saveSecret;
    }

    protected LoginCredentials extractCredentials() {
        ValidationInfo validationInfo = validateForm();
        LoginCredentials creds = new LoginCredentials();

        if (validationInfo == null) {
            resetConnectionResults();
            creds.isValid = true;
            creds.vaultDns = vaultDnsField.getText();

            boolean isBasic = authTabs.getSelectedIndex() == 0;

            if (isBasic) {
                creds.isBasicAuth = true;
                creds.username = usernameField.getText();
                creds.password = String.copyValueOf(passwordField.getPassword());
                creds.saveSecret = savePasswordCheckBox.isSelected();
            } else {
                creds.isBasicAuth = false;
                creds.sessionId = String.copyValueOf(sessionIdField.getPassword());
                creds.saveSecret = saveSessionIdCheckBox.isSelected();
            }
        } else {
            messageArea.setForeground(JBColor.RED);
            messageArea.setText(validationInfo.message);

            if (validationInfo.component != null) {
                validationInfo.component.requestFocusInWindow();
            }
        }
        return creds;
    }

    public void doAsyncLogin(Runnable onSuccess, Runnable onFailure) {
        ValidationInfo validationInfo = validateForm();
        if (validationInfo != null) {
            messageArea.setForeground(JBColor.RED);
            messageArea.setText(validationInfo.message);
            if (validationInfo.component != null) {
                validationInfo.component.requestFocusInWindow();
            }
            if (onFailure != null) onFailure.run();
            return;
        }

        resetConnectionResults();

        String vaultDns = vaultDnsField.getText();
        boolean isBasic = authTabs.getSelectedIndex() == 0;
        String username = usernameField.getText();
        String password = String.copyValueOf(passwordField.getPassword());
        boolean savePass = savePasswordCheckBox.isSelected();
        String sessionId = String.copyValueOf(sessionIdField.getPassword());
        boolean saveSession = saveSessionIdCheckBox.isSelected();

        if (showLoginButton && loginButton != null) {
            loginButton.setEnabled(false);
            loginButton.setText("Connecting...");
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ToolboxProject.ConnectionResult result = null;
            try {
                if (isBasic) {
                    result = toolboxProject.connectWithBasic(vaultDns, username, password, savePass);
                } else {
                    result = toolboxProject.connectWithSession(vaultDns, sessionId, saveSession);
                }
            } catch (Exception e) {
                result = new ToolboxProject.ConnectionResult("Unexpected system error: " + e.getMessage());
            } finally {
                final ToolboxProject.ConnectionResult finalResult = result;

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (showLoginButton && loginButton != null) {
                        loginButton.setEnabled(true);
                        loginButton.setText("Login");
                    }

                    if (finalResult != null && finalResult.isConnected()) {
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        displayConnectionResults(finalResult != null ? finalResult : new ToolboxProject.ConnectionResult("Unknown error."));
                        if (onFailure != null) onFailure.run();
                    }
                }, com.intellij.openapi.application.ModalityState.any());
            }
        });
    }

    @Deprecated
    protected boolean login() {
        return false;
    }

    protected @Nullable ValidationInfo validateForm() {
        if (vaultDnsField.getText().isEmpty()) {
            return new ValidationInfo("Vault DNS is required", vaultDnsField);
        }

        if (authTabs.getSelectedIndex() == 0) {
            String username = usernameField.getText();
            String password = String.copyValueOf(passwordField.getPassword());

            if (username.isEmpty()) {
                return new ValidationInfo("Username is required", usernameField);
            } else if (password.isEmpty()) {
                return new ValidationInfo("Password is required", passwordField);
            }
        } else {
            String sessionId = String.copyValueOf(sessionIdField.getPassword());
            if (sessionId.isEmpty()) {
                return new ValidationInfo("Session ID is required", sessionIdField);
            }
        }
        return null;
    }

    @FunctionalInterface
    public interface FieldListener extends DocumentListener {
        void update(DocumentEvent e);
        @Override default void insertUpdate(DocumentEvent e) { update(e); }
        @Override default void removeUpdate(DocumentEvent e) { update(e); }
        @Override default void changedUpdate(DocumentEvent e) { update(e); }
    }

    protected void init() {
        this.setLayout(new BorderLayout());
        this.setSize(400, 400);
        this.setMaximumSize(new Dimension(400, 400));
        this.setBorder(JBUI.Borders.empty(20));

        this.setPreferredSize(new Dimension(400, 400));
        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        vaultDnsField.getDocument().addDocumentListener((FieldListener) e -> resetConnectionResults());
        usernameField.getDocument().addDocumentListener((FieldListener) e -> resetConnectionResults());
        passwordField.getDocument().addDocumentListener((FieldListener) e -> resetConnectionResults());
        sessionIdField.getDocument().addDocumentListener((FieldListener) e -> resetConnectionResults());

        authTabs.addTab("Basic", basicAuthPanel);
        authTabs.addTab("Session", sessionAuthPanel);

        addVisibilityToggle(passwordField);
        addVisibilityToggle(sessionIdField);

        basicAuthPanel.add(new JLabel("Username:"));
        basicAuthPanel.add(usernameField);
        basicAuthPanel.add(new JLabel("Password:"));
        basicAuthPanel.add(passwordField);
        basicAuthPanel.add(savePasswordCheckBox);

        sessionAuthPanel.add(new JLabel("Session ID:"));
        sessionAuthPanel.add(sessionIdField);

        sessionAuthPanel.add(new JLabel());
        sessionAuthPanel.add(new JLabel());
        sessionAuthPanel.add(saveSessionIdCheckBox);

        vaultDnsPanel.add(new JLabel("Vault DNS:"));
        vaultDnsPanel.add(vaultDnsField);

        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(UIManager.getFont("Label.font"));

        loginPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        if (showLoginButton) {
            loginButton.addActionListener(e -> {
                this.doAsyncLogin(null, null);
            });
            loginPanel.add(loginButton, BorderLayout.NORTH);
        }
        loginPanel.add(messageArea, BorderLayout.CENTER);

        this.add(vaultDnsPanel, BorderLayout.NORTH);
        this.add(authTabs, BorderLayout.CENTER);
        this.add(loginPanel, BorderLayout.SOUTH);

        AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());

        if (toolboxProject.isToolboxEnabled()) {
            Vault currentVault = toolboxProject.getActiveVault();
            if (currentVault != null && currentVault.getVaultDNS() != null) {
                setFieldValues(currentVault.getAuthenticationType(), currentVault.getVaultDNS(), currentVault.getSaveSecret());
            } else {
                setFieldValues(appState.authenticationType, appState.vaultDNS, appState.saveSecret);
            }
        } else {
            setFieldValues(appState.authenticationType, appState.vaultDNS, appState.saveSecret);
        }
    }

    private void addVisibilityToggle(JPasswordField field) {
        char defaultEchoChar = field.getEchoChar();

        JLabel toggleIcon = new JLabel(AllIcons.Actions.Show) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                if (field.getEchoChar() != 0) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                }
                super.paintComponent(g2);
                g2.dispose();
            }
        };

        toggleIcon.setToolTipText("Show Password");
        toggleIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));

        toggleIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (field.getEchoChar() == 0) {
                    field.setEchoChar(defaultEchoChar);
                    toggleIcon.setToolTipText("Show Password");
                } else {
                    field.setEchoChar((char) 0);
                    toggleIcon.setToolTipText("Hide Password");
                }
                toggleIcon.repaint();
            }
        });

        Border currentBorder = field.getBorder();
        field.setBorder(BorderFactory.createCompoundBorder(currentBorder, JBUI.Borders.emptyRight(28)));

        field.setLayout(null);
        field.add(toggleIcon);

        field.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int height = field.getHeight();
                int width = field.getWidth();

                Dimension prefSize = toggleIcon.getPreferredSize();
                int iconW = prefSize.width > 0 ? prefSize.width : 16;
                int iconH = prefSize.height > 0 ? prefSize.height : 16;

                toggleIcon.setBounds(width - iconW - 10, (height - iconH) / 2, iconW, iconH);
            }
        });
    }

    private void setFieldValues(
            Vault.AuthenticationType authenticationType,
            String vaultDNS, Boolean saveSecret) {

        vaultDnsField.setText(vaultDNS);

        boolean shouldSave = BooleanUtils.isTrue(saveSecret);
        savePasswordCheckBox.setSelected(shouldSave);
        saveSessionIdCheckBox.setSelected(shouldSave);

        if (BASIC.equals(authenticationType)) {
            authTabs.setSelectedIndex(0);
        } else if (SESSION_ID.equals(authenticationType)) {
            authTabs.setSelectedIndex(1);
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            BasicAuth basicAuth = VaultCredentialManager.getUsernamePassword(vaultDNS);
            String savedSession = VaultCredentialManager.getSessionId(vaultDNS);

            ApplicationManager.getApplication().invokeLater(() -> {

                if (basicAuth != null && basicAuth.getUsername() != null && !basicAuth.getUsername().isEmpty()) {
                    usernameField.setText(basicAuth.getUsername());
                    passwordField.setText(basicAuth.getPassword());
                }

                if (savedSession != null && !savedSession.isEmpty()) {
                    sessionIdField.setText(savedSession);
                }

            }, com.intellij.openapi.application.ModalityState.any());
        });
    }
}