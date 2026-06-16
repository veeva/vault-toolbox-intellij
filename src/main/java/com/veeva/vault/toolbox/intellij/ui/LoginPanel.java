package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.toolbox.intellij.credentials.BasicAuth;
import com.veeva.vault.toolbox.intellij.credentials.VaultCredentialManager;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.settings.AppSettings;
import com.veeva.vault.toolbox.intellij.settings.SavedCredential;
import com.veeva.vault.toolbox.intellij.settings.Vault;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.veeva.vault.toolbox.intellij.settings.Vault.AuthenticationType.*;

/**
 * Provides a login interface for Veeva Vault, supporting both Basic and Session-based authentication.
 */
public class LoginPanel extends JBPanel<LoginPanel> {

    ToolboxProject toolboxProject;
    ToolboxProject.ConnectionResult connectionResult;

    JPanel vaultDnsPanel = new JBPanel<>();
    JPanel basicAuthPanel = new JBPanel<>();
    JPanel sessionAuthPanel = new JBPanel<>();

    JPanel loginPanel = new JBPanel<>(new BorderLayout(0, 8));
    JTextArea messageArea = new JTextArea();
    JBTabbedPane authTabs = new JBTabbedPane();
    ToolboxButton loginButton = new ToolboxButton(toolboxProject, "Login");
    boolean showLoginButton;

    JBTextField vaultDnsField = new JBTextField(30);
    JBTextField usernameField = new JBTextField(30);
    JBPasswordField passwordField = new JBPasswordField();
    JBPasswordField sessionIdField = new JBPasswordField();

    SavedCredential loadedCredential = null;
    private Consumer<PendingCredentialSave> credentialSaveHandler;

    /**
     * Carries the context needed to prompt the user to save or update a credential
     * after a successful login. Passed to {@link VaultInfoPanel} for inline display.
     */
    public static class PendingCredentialSave {
        public final String vaultDns;
        public final boolean isBasic;
        public final String username;
        public final String password;
        public final String sessionId;
        /** Non-null when an existing saved credential matched the login — prompt to update. */
        public final SavedCredential matchedCred;
        /** Previously stored secret for the matched credential; used to detect password changes. */
        public final String storedSecret;

        /**
         * Constructs a new PendingCredentialSave.
         *
         * @param vaultDns     The Vault DNS.
         * @param isBasic      True if basic auth, false otherwise.
         * @param username     The username.
         * @param password     The password.
         * @param sessionId    The session ID.
         * @param matchedCred  The matching saved credential, if any.
         * @param storedSecret The stored secret for the matching credential, if any.
         */
        PendingCredentialSave(String vaultDns, boolean isBasic, String username,
                              String password, String sessionId,
                              SavedCredential matchedCred, String storedSecret) {
            this.vaultDns = vaultDns;
            this.isBasic = isBasic;
            this.username = username;
            this.password = password;
            this.sessionId = sessionId;
            this.matchedCred = matchedCred;
            this.storedSecret = storedSecret;
        }
    }

    /**
     * Prompts the user to save or update the credential using standard dialogs.
     * @param parent The parent component for the dialogs.
     * @param pending The pending credential context.
     */
    public static void promptToSaveCredential(Component parent, PendingCredentialSave pending) {
        if (pending == null) return;

        if (pending.matchedCred != null) {
            String label = (pending.matchedCred.label != null && !pending.matchedCred.label.isEmpty())
                    ? pending.matchedCred.label : pending.matchedCred.vaultDNS;
            boolean dnsChanged = !pending.vaultDns.equalsIgnoreCase(pending.matchedCred.vaultDNS);
            boolean usernameChanged = pending.isBasic && !pending.username.equals(pending.matchedCred.username);
            
            String message;
            if (dnsChanged || usernameChanged) {
                message = "Update saved credential \"" + label + "\" with these changes?";
            } else {
                message = "Update saved " + (pending.isBasic ? "password" : "session ID") + " for \"" + label + "\"?";
            }

            int confirm = javax.swing.JOptionPane.showConfirmDialog(parent, message, "Update Credential", javax.swing.JOptionPane.YES_NO_OPTION);
            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                pending.matchedCred.vaultDNS = pending.vaultDns;
                if (pending.isBasic) pending.matchedCred.username = pending.username;
                com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    if (pending.isBasic) {
                        VaultCredentialManager.setUsernamePasswordById(pending.matchedCred.id, pending.username, pending.password);
                    } else {
                        VaultCredentialManager.setSessionIdById(pending.matchedCred.id, pending.sessionId);
                    }
                });
                com.intellij.openapi.application.ApplicationManager.getApplication().saveSettings();
            }
        } else {
            int confirm = javax.swing.JOptionPane.showConfirmDialog(parent, "Save these credentials?", "Save Credential", javax.swing.JOptionPane.YES_NO_OPTION);
            if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                String name = javax.swing.JOptionPane.showInputDialog(parent, "Credential label:", pending.vaultDns);
                if (name != null) {
                    name = name.trim();
                    if (name.isEmpty()) name = pending.vaultDns;

                    AppSettings.AppState appState = java.util.Objects.requireNonNull(AppSettings.getInstance().getState());
                    final String finalName = name;
                    SavedCredential duplicate = appState.savedCredentials.stream()
                            .filter(c -> finalName.equalsIgnoreCase(c.label))
                            .findFirst().orElse(null);

                    if (duplicate != null) {
                        int overwrite = javax.swing.JOptionPane.showConfirmDialog(
                                parent,
                                "A credential with label \"" + name + "\" already exists. Overwrite it?",
                                "Duplicate Label",
                                javax.swing.JOptionPane.YES_NO_OPTION,
                                javax.swing.JOptionPane.WARNING_MESSAGE);
                        if (overwrite == javax.swing.JOptionPane.YES_OPTION) {
                            duplicate.vaultDNS = pending.vaultDns;
                            duplicate.authenticationType = pending.isBasic
                                    ? Vault.AuthenticationType.BASIC
                                    : Vault.AuthenticationType.SESSION_ID;
                            if (pending.isBasic) duplicate.username = pending.username;
                            final SavedCredential finalDuplicate = duplicate;
                            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                                if (pending.isBasic) {
                                    VaultCredentialManager.setUsernamePasswordById(finalDuplicate.id, pending.username, pending.password);
                                } else {
                                    VaultCredentialManager.setSessionIdById(finalDuplicate.id, pending.sessionId);
                                }
                            });
                            com.intellij.openapi.application.ApplicationManager.getApplication().saveSettings();
                        }
                    } else {
                        SavedCredential newCred = new SavedCredential();
                        newCred.label = name;
                        newCred.vaultDNS = pending.vaultDns;
                        newCred.authenticationType = pending.isBasic
                                ? Vault.AuthenticationType.BASIC
                                : Vault.AuthenticationType.SESSION_ID;
                        if (pending.isBasic) newCred.username = pending.username;
                        appState.savedCredentials.add(newCred);
                        com.intellij.openapi.application.ApplicationManager.getApplication().saveSettings();

                        final SavedCredential finalNewCred = newCred;
                        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                            if (pending.isBasic) {
                                VaultCredentialManager.setUsernamePasswordById(finalNewCred.id, pending.username, pending.password);
                            } else {
                                VaultCredentialManager.setSessionIdById(finalNewCred.id, pending.sessionId);
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * Registers a handler that is called after a successful login when a credential
     * save or update prompt should be shown. Called on the EDT.
     */
    public void setCredentialSaveHandler(Consumer<PendingCredentialSave> handler) {
        this.credentialSaveHandler = handler;
    }

    /**
     * Initializes the login panel.
     *
     * @param toolboxProject  The project context.
     * @param showLoginButton Whether to display the integrated login button.
     */
    public LoginPanel(ToolboxProject toolboxProject, boolean showLoginButton) {
        super(true);
        this.toolboxProject = toolboxProject;
        this.showLoginButton = showLoginButton;
        init();
    }

    /**
     * Updates the UI to show connection errors if the login attempt fails.
     *
     * @param result The result of the connection attempt.
     */
    public void displayConnectionResults(ToolboxProject.ConnectionResult result) {
        this.connectionResult = result;
        if (connectionResult != null && connectionResult.isFailure()) {
            messageArea.setForeground(JBColor.RED);
            messageArea.setText(connectionResult.getErrorMessage());
            if (!messageArea.isVisible()) {
                messageArea.setVisible(true);
                loginPanel.setVisible(true);
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JDialog) {
                    window.pack();
                }
            }
        } else {
            resetConnectionResults();
        }
    }

    /**
     * Clears the current connection results and error messages from the UI.
     */
    public void resetConnectionResults() {
        this.connectionResult = null;
        messageArea.setText("");
        messageArea.setForeground(null);
        if (messageArea.isVisible()) {
            messageArea.setVisible(false);
            if (!showLoginButton) {
                loginPanel.setVisible(false);
            }
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JDialog) {
                window.pack();
            }
        }
    }

    /**
     * Data class holding extracted login credentials from the UI.
     */
    public static class LoginCredentials {
        public boolean isValid = false;
        public boolean isBasicAuth = true;
        public String vaultDns;
        public String username;
        public String password;
        public String sessionId;
        public boolean saveSecret;
    }

    /**
     * Extracts and validates the current form data into a credentials object.
     *
     * @return A LoginCredentials object containing the current field values.
     */
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
                creds.saveSecret = true;
            } else {
                creds.isBasicAuth = false;
                creds.sessionId = String.copyValueOf(sessionIdField.getPassword());
                creds.saveSecret = true;
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

    public static PendingCredentialSave checkCredentialForSave(String vaultDns, boolean isBasic, String username, String password, String sessionId, SavedCredential credentialFromPicker) {
        AppSettings.AppState appState = java.util.Objects.requireNonNull(AppSettings.getInstance().getState());
        SavedCredential matchedCred = credentialFromPicker;
        if (matchedCred == null) {
            for (SavedCredential c : appState.savedCredentials) {
                boolean typeMatch = isBasic
                        ? c.authenticationType == Vault.AuthenticationType.BASIC
                        : c.authenticationType == Vault.AuthenticationType.SESSION_ID;
                boolean dnsMatch = vaultDns.equalsIgnoreCase(c.vaultDNS);
                boolean userMatch = !isBasic || username.equals(c.username);
                if (typeMatch && dnsMatch && userMatch) {
                    matchedCred = c;
                    break;
                }
            }
        }

        String storedSecret = null;
        if (matchedCred != null) {
            if (isBasic) {
                BasicAuth stored = VaultCredentialManager.getUsernamePasswordById(matchedCred.id);
                storedSecret = stored != null ? stored.getPassword() : null;
            } else {
                storedSecret = VaultCredentialManager.getSessionIdById(matchedCred.id);
            }
            String currentSecret = isBasic ? password : sessionId;
            boolean secretChanged = !java.util.Objects.equals(storedSecret, currentSecret);
            boolean dnsChanged = !vaultDns.equalsIgnoreCase(matchedCred.vaultDNS);
            boolean usernameChanged = isBasic && !username.equals(matchedCred.username);
            if (dnsChanged) {
                return new PendingCredentialSave(vaultDns, isBasic, username, password, sessionId, null, null);
            } else if (secretChanged || usernameChanged) {
                return new PendingCredentialSave(vaultDns, isBasic, username, password, sessionId, matchedCred, storedSecret);
            }
        } else {
            return new PendingCredentialSave(vaultDns, isBasic, username, password, sessionId, null, null);
        }
        return null;
    }

    /**
     * Performs an asynchronous login operation on a background thread.
     *
     * @param onSuccess Callback executed on the EDT after a successful login.
     * @param onFailure Callback executed on the EDT after a failed login.
     */
    public void doAsyncLogin(Runnable onSuccess, Runnable onFailure) {
        ValidationInfo validationInfo = validateForm();
        if (validationInfo != null) {
            messageArea.setForeground(JBColor.RED);
            messageArea.setText(validationInfo.message);
            if (!messageArea.isVisible()) {
                messageArea.setVisible(true);
                loginPanel.setVisible(true);
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JDialog) {
                    window.pack();
                }
            }
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
        String sessionId = String.copyValueOf(sessionIdField.getPassword());
        SavedCredential credentialFromPicker = loadedCredential;

        if (showLoginButton && loginButton != null) {
            loginButton.setEnabled(false);
            loginButton.setText("Connecting...");
        }

        AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ToolboxProject.ConnectionResult result = null;
            PendingCredentialSave pending = null;

            try {
                if (isBasic) {
                    result = toolboxProject.connectWithBasic(vaultDns, username, password, true);
                } else {
                    result = toolboxProject.connectWithSession(vaultDns, sessionId, true);
                }

                if (result != null && result.isConnected()) {
                    pending = checkCredentialForSave(vaultDns, isBasic, username, password, sessionId, credentialFromPicker);
                }
            } catch (Exception e) {
                result = new ToolboxProject.ConnectionResult("Unexpected system error: " + e.getMessage());
            }

            final ToolboxProject.ConnectionResult finalResult = result;
            final PendingCredentialSave finalPending = pending;

            ApplicationManager.getApplication().invokeLater(() -> {
                if (showLoginButton && loginButton != null) {
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                }
                if (finalResult != null && finalResult.isConnected()) {
                    if (finalPending != null && credentialSaveHandler != null) {
                        credentialSaveHandler.accept(finalPending);
                    }
                    if (onSuccess != null) onSuccess.run();
                } else {
                    displayConnectionResults(finalResult != null
                            ? finalResult : new ToolboxProject.ConnectionResult("Unknown error."));
                    if (onFailure != null) onFailure.run();
                }
            }, com.intellij.openapi.application.ModalityState.any());
        });
    }

    @Deprecated
    protected boolean login() {
        return false;
    }

    /**
     * Validates the input fields based on the selected authentication type.
     *
     * @return ValidationInfo if an error is found, otherwise null.
     */
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

    /**
     * Helper interface to simplify document listener implementation for UI updates.
     */
    @FunctionalInterface
    public interface FieldListener extends DocumentListener {
        void update(DocumentEvent e);
        @Override default void insertUpdate(DocumentEvent e) { update(e); }
        @Override default void removeUpdate(DocumentEvent e) { update(e); }
        @Override default void changedUpdate(DocumentEvent e) { update(e); }
    }

    /**
     * Configures the layout, components, and default values of the login panel.
     */
    protected void init() {
        this.setLayout(new BorderLayout());
        if (showLoginButton) {
            this.setBorder(JBUI.Borders.empty(10, 20));
        } else {
            this.setBorder(JBUI.Borders.empty(0, 0));
        }

        vaultDnsField.getDocument().addDocumentListener((FieldListener) e -> resetConnectionResults());
        usernameField.getDocument().addDocumentListener((FieldListener) e -> resetConnectionResults());
        passwordField.getDocument().addDocumentListener((FieldListener) e -> resetConnectionResults());
        sessionIdField.getDocument().addDocumentListener((FieldListener) e -> resetConnectionResults());

        addVisibilityToggle(passwordField);
        addVisibilityToggle(sessionIdField);

        JBLabel usernameLabel = new JBLabel("Username:");
        usernameLabel.setIcon(AllIcons.General.ContextHelp);
        usernameLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        usernameLabel.setToolTipText("Vault DNS and Username are case sensitive");
        
        JBPanel<?> userRow = new JBPanel<>(new BorderLayout());
        userRow.add(usernameField, BorderLayout.CENTER);

        JBPanel<?> passRow = new JBPanel<>(new BorderLayout());
        passRow.add(passwordField, BorderLayout.CENTER);

        JBPanel<?> sessRow = new JBPanel<>(new BorderLayout());
        sessRow.add(sessionIdField, BorderLayout.CENTER);

        basicAuthPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(usernameLabel, userRow, 1, true)
                .addLabeledComponent(new JBLabel("Password:"), passRow, 1, true)
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();
        basicAuthPanel.setBorder(JBUI.Borders.empty(8, 0, 0, 0));

        sessionAuthPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Session ID:"), sessRow, 1, true)
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();
        sessionAuthPanel.setBorder(JBUI.Borders.empty(8, 0, 0, 0));

        authTabs.addTab("Basic", basicAuthPanel);
        authTabs.addTab("Session", sessionAuthPanel);

        AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());

        JBLabel vaultDnsLabel = new JBLabel("Vault DNS:");
        vaultDnsLabel.setIcon(AllIcons.General.ContextHelp);
        vaultDnsLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        vaultDnsLabel.setToolTipText("Navigate to Vault Toolbox settings and check for Allow All Certificates checkbox if you want to authenticate to PVM");
        
        JBPanel<?> dnsRow = new JBPanel<>(new BorderLayout(4, 0));
        dnsRow.add(vaultDnsField, BorderLayout.CENTER);
        dnsRow.add(buildCredentialPickerIcon(), BorderLayout.EAST);
        
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(UIManager.getFont("Label.font"));
        messageArea.setVisible(false);

        loginPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        loginPanel.setBorder(JBUI.Borders.emptyTop(10));

        if (showLoginButton) {
            java.awt.event.ActionListener enterListener = e -> {
                if (loginButton != null && loginButton.isEnabled()) {
                    doAsyncLogin(null, null);
                }
            };
            vaultDnsField.addActionListener(enterListener);
            usernameField.addActionListener(enterListener);
            passwordField.addActionListener(enterListener);
            sessionIdField.addActionListener(enterListener);

            loginButton.addActionListener(e -> this.doAsyncLogin(null, null));
            JBPanel<?> buttonRow = new JBPanel<>(new BorderLayout());
            buttonRow.setBorder(JBUI.Borders.empty(0, -2, 0, 10));
            buttonRow.add(loginButton, BorderLayout.CENTER);
            
            loginPanel.add(buttonRow, BorderLayout.NORTH);
            loginPanel.add(messageArea, BorderLayout.CENTER);
        } else {
            loginPanel.add(messageArea, BorderLayout.CENTER);
            loginPanel.setVisible(false);
        }

        JPanel formPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(vaultDnsLabel, dnsRow, 1, true)
                .addComponent(authTabs)
                .addComponentToRightColumn(loginPanel)
                .addComponentFillVertically(new JBPanel<>(), 0)
                .getPanel();

        this.add(formPanel, BorderLayout.CENTER);

        SavedCredential defaultCredential = appState.savedCredentials.stream()
                .filter(c -> c.isDefault).findFirst().orElse(null);

        if (defaultCredential != null) {
            loadSavedCredential(defaultCredential);
        } else if (toolboxProject.isToolboxEnabled()) {
            Vault currentVault = toolboxProject.getActiveVault();
            if (currentVault != null && currentVault.getVaultDNS() != null) {
                setFieldValues(currentVault.getAuthenticationType(), currentVault.getVaultDNS());
            } else {
                setFieldValues(appState.authenticationType, appState.vaultDNS);
            }
        } else {
            setFieldValues(appState.authenticationType, appState.vaultDNS);
        }
    }

    /**
     * Injects a visibility toggle icon into a password field.
     *
     * @param field The password field to decorate.
     */
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

    /**
     * Sets the UI field values based on stored settings or vault configuration.
     *
     * @param authenticationType The authentication type.
     * @param vaultDNS           The Vault DNS.
     */
    private void setFieldValues(Vault.AuthenticationType authenticationType, String vaultDNS) {
        SavedCredential cred = AppSettings.findCredentialByDns(vaultDNS);
        if (cred != null) {
            loadSavedCredential(cred);
            return;
        }
        vaultDnsField.setText(vaultDNS);
        if (BASIC.equals(authenticationType)) {
            authTabs.setSelectedIndex(0);
        } else if (SESSION_ID.equals(authenticationType)) {
            authTabs.setSelectedIndex(1);
        }
    }

    /**
     * Builds the credential picker icon for selecting saved credentials.
     *
     * @return The credential picker icon.
     */
    private JLabel buildCredentialPickerIcon() {
        JLabel icon = new JLabel(AllIcons.General.ArrowDown) {
            boolean hovered = false;
            boolean pressed = false;

            {
                JLabel self = this;
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e)  { hovered = false; pressed = false; repaint(); }
                    @Override public void mousePressed(java.awt.event.MouseEvent e) { pressed = true; repaint(); }
                    @Override public void mouseReleased(java.awt.event.MouseEvent e){ pressed = false; repaint(); }
                    @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                        AppSettings.AppState state = AppSettings.getInstance().getState();
                        showCredentialPopup(self, state != null ? state.savedCredentials : java.util.Collections.emptyList());
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                if (hovered || pressed) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    String key = pressed ? "ActionButton.pressedBackground" : "ActionButton.hoverBackground";
                    Color bg = UIManager.getColor(key);
                    if (bg == null) {
                        bg = new JBColor(new Color(0, 0, 0, pressed ? 50 : 25),
                                         new Color(255, 255, 255, pressed ? 50 : 25));
                    }
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        icon.setBorder(JBUI.Borders.empty(2, 4, 2, 4));
        icon.setCursor(new Cursor(Cursor.HAND_CURSOR));
        icon.setToolTipText("Load saved credential");
        return icon;
    }

    /**
     * Shows a popup menu for selecting a saved credential.
     *
     * @param invoker          The component that invoked the popup.
     * @param savedCredentials The list of saved credentials.
     */
    private void showCredentialPopup(Component invoker, List<SavedCredential> savedCredentials) {
        JPopupMenu popup = new JPopupMenu();

        boolean noneActive = loadedCredential == null;
        JMenuItem noneItem = noneActive
                ? createActiveMenuItem("● New Credential")
                : createInactiveMenuItem("New Credential");
        noneItem.addActionListener(e -> clearCredential());
        popup.add(noneItem);
        popup.addSeparator();

        for (SavedCredential cred : savedCredentials) {
            String label = (cred.label != null && !cred.label.isEmpty()) ? cred.label : cred.vaultDNS;
            boolean active = cred == loadedCredential;
            JMenuItem item = active
                    ? createActiveMenuItem("● " + label)
                    : createInactiveMenuItem(label);
            item.addActionListener(e -> loadSavedCredential(cred));
            popup.add(item);
        }
        popup.show(invoker, 0, invoker.getHeight());
    }

    /**
     * Creates a menu item for an active selection.
     *
     * @param text The text for the menu item.
     * @return The created JMenuItem.
     */
    private JMenuItem createActiveMenuItem(String text) {
        Color orange = new Color(0xFF, 0x9E, 0x16);
        JMenuItem item = new JMenuItem(text) {
            @Override
            public Color getForeground() {
                return orange;
            }

            @Override
            protected void paintComponent(Graphics g) {
                ButtonModel m = getModel();
                boolean wasArmed = m.isArmed();
                if (wasArmed) m.setArmed(false);
                super.paintComponent(g);
                if (wasArmed) m.setArmed(true);
            }
        };
        item.setFont(item.getFont().deriveFont(java.awt.Font.BOLD));
        return item;
    }

    /**
     * Creates a menu item for an inactive selection.
     *
     * @param text The text for the menu item.
     * @return The created JMenuItem.
     */
    private JMenuItem createInactiveMenuItem(String text) {
        Color normalBg = javax.swing.UIManager.getColor("PopupMenu.background");

        JMenuItem item = new JMenuItem(text) {
            @Override
            protected void paintComponent(Graphics g) {
                ButtonModel m = getModel();
                boolean wasArmed = m.isArmed();
                if (wasArmed) m.setArmed(false);
                super.paintComponent(g);
                if (wasArmed) m.setArmed(true);
            }
        };
        item.setOpaque(true);
        if (normalBg != null) item.setBackground(normalBg);

        item.addMouseListener(new java.awt.event.MouseAdapter() {
            private Color hoverColor;

            private Color getHoverColor() {
                if (hoverColor != null) return hoverColor;
                Color h = javax.swing.UIManager.getColor("List.hoverRowBackground");
                if (h != null && h.getAlpha() == 255) {
                    hoverColor = h;
                } else {
                    Color base = item.getBackground() != null ? item.getBackground() : new Color(240, 240, 240);
                    boolean dark = (base.getRed() + base.getGreen() + base.getBlue()) < 384;
                    int d = dark ? 30 : -20;
                    hoverColor = new Color(
                        Math.max(0, Math.min(255, base.getRed() + d)),
                        Math.max(0, Math.min(255, base.getGreen() + d)),
                        Math.max(0, Math.min(255, base.getBlue() + d)));
                }
                return hoverColor;
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                item.setBackground(getHoverColor());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                item.setBackground(normalBg);
            }
        });

        return item;
    }

    /**
     * Clears the currently loaded credential and resets the UI fields.
     */
    private void clearCredential() {
        loadedCredential = null;
        vaultDnsField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        sessionIdField.setText("");
    }

    /**
     * Loads a saved credential into the UI fields.
     *
     * @param credential The credential to load.
     */
    private void loadSavedCredential(SavedCredential credential) {
        loadedCredential = credential;
        vaultDnsField.setText(credential.vaultDNS != null ? credential.vaultDNS : "");

        boolean isBasic = credential.authenticationType != Vault.AuthenticationType.SESSION_ID;
        if (isBasic) {
            authTabs.setSelectedIndex(0);
            usernameField.setText(credential.username != null ? credential.username : "");
        } else {
            authTabs.setSelectedIndex(1);
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String password = null;
            String session = null;
            if (isBasic) {
                BasicAuth auth = VaultCredentialManager.getUsernamePasswordById(credential.id);
                if (auth != null) password = auth.getPassword();
            } else {
                session = VaultCredentialManager.getSessionIdById(credential.id);
            }
            final String finalPassword = password;
            final String finalSession = session;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (finalPassword != null) passwordField.setText(finalPassword);
                if (finalSession != null) sessionIdField.setText(finalSession);
            }, com.intellij.openapi.application.ModalityState.any());
        });
    }
}
