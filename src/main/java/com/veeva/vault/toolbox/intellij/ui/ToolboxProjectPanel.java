package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.toolbox.intellij.listeners.ConnectionListener;
import com.veeva.vault.toolbox.intellij.metadata.MetadataService;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import icons.ToolboxIcons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import com.veeva.vault.toolbox.intellij.settings.SavedCredential;
import com.veeva.vault.toolbox.intellij.settings.AppSettings;
import com.veeva.vault.toolbox.intellij.settings.Vault;
import com.veeva.vault.toolbox.intellij.credentials.VaultCredentialManager;
import java.util.Objects;

/**
 * Main project-level panel for the Vault Toolbox plugin.
 * Manages the top-level navigation, login/status display, and connection state transitions.
 */
public class ToolboxProjectPanel extends JBPanel<ToolboxProjectPanel> {
    private static final Logger logger = LoggerFactory.getLogger(ToolboxProjectPanel.class);

    private final JBLabel currentVault = new JBLabel();
    private final ToolboxProject toolboxProject;
    private final JButton logoutButton = new JButton();
    private JBPanel<?> mainPanel = new JBPanel<>();
    private JBTabbedPane navigationTabs = new JBTabbedPane();
    private JBPanel<?> topPanel = new JBPanel<>();
    private JBPanel<?> bottomPanel = new JBPanel<>();
    private JBPanel<?> infoTabPanel;
    private JBPanel<?> logTabPanel;
    private LoginPanel loginPanel;
    private VaultInfoPanel vaultInfoPanel;

    private JPanel saveBanner;
    private JPanel bannerCards;
    private JLabel bannerMessageLabel;
    private JTextField bannerLabelField;
    private LoginPanel.PendingCredentialSave pendingSave;

    /**
     * Initializes the toolbox project panel.
     *
     * @param project The current IntelliJ project.
     */
    public ToolboxProjectPanel(Project project) {
        super(new BorderLayout());
        this.toolboxProject = ToolboxProject.getInstance(project);
        this.add(getTopPanel(), BorderLayout.NORTH);
        this.add(getMainPanel(), BorderLayout.CENTER);
        this.add(getBottomPanel(), BorderLayout.SOUTH);

        MetadataService metadataService = MetadataService.getInstance(project);
        toolboxProject.addConnectionListener(new ConnectionListener() {
            @Override
            public void connected() {
                refreshStatus();
                if (metadataService != null) {
                    metadataService.onConnected();
                }
            }

            @Override
            public void disconnected() {
                refreshStatus();
                if (metadataService != null) {
                    metadataService.onDisconnected();
                }
            }
        });
        refreshStatus();
        if (metadataService != null && toolboxProject.isConnected()) {
            metadataService.onConnected();
        }
    }

    /**
     * Refreshes the UI status based on the current connection state of the toolbox project.
     * Updates tool window icons, visibility of login/info panels, and vault DNS display.
     */
    public void refreshStatus() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (toolboxProject != null && toolboxProject.isConnected()) {
                if (toolboxProject.getToolWindow() != null) {
                    toolboxProject.getToolWindow().setIcon(ToolboxIcons.Connected);
                }
                currentVault.setText(toolboxProject.getVaultDNS());
                loginPanel.setVisible(false);
                vaultInfoPanel.setVisible(true);
                if (infoTabPanel != null && infoTabPanel.getLayout() instanceof CardLayout) {
                    ((CardLayout) infoTabPanel.getLayout()).show(infoTabPanel, "info");
                }
                topPanel.setVisible(false);
                bottomPanel.setVisible(true);
            } else {
                if (toolboxProject != null && toolboxProject.getToolWindow() != null) {
                    toolboxProject.getToolWindow().setIcon(ToolboxIcons.Disconnected);
                }
                currentVault.setText("");
                if (loginPanel != null) {
                    loginPanel.setVisible(true);
                }
                if (vaultInfoPanel != null) {
                    vaultInfoPanel.setVisible(false);
                }
                if (infoTabPanel != null && infoTabPanel.getLayout() instanceof CardLayout) {
                    ((CardLayout) infoTabPanel.getLayout()).show(infoTabPanel, "login");
                }
                topPanel.setVisible(true);
                bottomPanel.setVisible(false);
                hideSaveBanner();
            }
            if (vaultInfoPanel != null) {
                vaultInfoPanel.refreshVaultInfo();
            }
        }, ModalityState.any());
    }

    private JPanel buildSaveBanner() {
        bannerMessageLabel = new JLabel();
        bannerLabelField = new JTextField();

        JButton yesButton = new JButton("Yes");
        JButton noButton = new JButton("No");
        yesButton.addActionListener(e -> onBannerYes());
        noButton.addActionListener(e -> hideSaveBanner());

        JPanel askButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        askButtons.setOpaque(false);
        askButtons.add(yesButton);
        askButtons.add(noButton);

        JPanel askCard = new JPanel(new BorderLayout(8, 0));
        askCard.setOpaque(false);
        askCard.add(bannerMessageLabel, BorderLayout.CENTER);
        askCard.add(askButtons, BorderLayout.EAST);

        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        saveButton.addActionListener(e -> onBannerSave());
        cancelButton.addActionListener(e -> hideSaveBanner());

        JPanel labelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        labelButtons.setOpaque(false);
        labelButtons.add(saveButton);
        labelButtons.add(cancelButton);

        JPanel labelLeft = new JPanel(new BorderLayout(6, 0));
        labelLeft.setOpaque(false);
        labelLeft.add(new JLabel("Credential label:"), BorderLayout.WEST);
        labelLeft.add(bannerLabelField, BorderLayout.CENTER);

        JPanel labelCard = new JPanel(new BorderLayout(8, 0));
        labelCard.setOpaque(false);
        labelCard.add(labelLeft, BorderLayout.CENTER);
        labelCard.add(labelButtons, BorderLayout.EAST);

        bannerCards = new JPanel(new CardLayout());
        bannerCards.setOpaque(false);
        bannerCards.add(askCard, "ask");
        bannerCards.add(labelCard, "label");

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(JBUI.Borders.empty(8, 20, 12, 20));
        content.add(bannerCards, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(new JSeparator(), BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    public void showSaveCredentialPrompt(LoginPanel.PendingCredentialSave pending) {
        this.pendingSave = pending;

        if (pending.matchedCred != null) {
            String label = (pending.matchedCred.label != null && !pending.matchedCred.label.isEmpty())
                    ? pending.matchedCred.label : pending.matchedCred.vaultDNS;
            boolean dnsChanged = !pending.vaultDns.equalsIgnoreCase(pending.matchedCred.vaultDNS);
            boolean usernameChanged = pending.isBasic && !pending.username.equals(pending.matchedCred.username);
            if (dnsChanged || usernameChanged) {
                bannerMessageLabel.setText("Update saved credential \"" + label + "\" with these changes?");
            } else {
                bannerMessageLabel.setText("Update saved " + (pending.isBasic ? "password" : "session ID")
                        + " for \"" + label + "\"?");
            }
        } else {
            bannerMessageLabel.setText("Save these credentials?");
            bannerLabelField.setText(pending.vaultDns);
        }

        ((CardLayout) bannerCards.getLayout()).show(bannerCards, "ask");
        saveBanner.setVisible(true);
        revalidate();
    }

    private void onBannerYes() {
        if (pendingSave == null) return;

        if (pendingSave.matchedCred != null) {
            final LoginPanel.PendingCredentialSave save = pendingSave;
            save.matchedCred.vaultDNS = save.vaultDns;
            if (save.isBasic) save.matchedCred.username = save.username;
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                if (save.isBasic) {
                    VaultCredentialManager.setUsernamePasswordById(save.matchedCred.id, save.username, save.password);
                } else {
                    VaultCredentialManager.setSessionIdById(save.matchedCred.id, save.sessionId);
                }
            });
            com.intellij.openapi.application.ApplicationManager.getApplication().saveSettings();
            hideSaveBanner();
        } else {
            ((CardLayout) bannerCards.getLayout()).show(bannerCards, "label");
        }
    }

    private void onBannerSave() {
        if (pendingSave == null) return;

        String name = bannerLabelField.getText().trim();
        if (name.isEmpty()) name = pendingSave.vaultDns;

        AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());

        final String finalName = name;
        SavedCredential duplicate = appState.savedCredentials.stream()
                .filter(c -> finalName.equalsIgnoreCase(c.label))
                .findFirst().orElse(null);

        if (duplicate != null) {
            int choice = javax.swing.JOptionPane.showConfirmDialog(
                    this,
                    "A credential with label \"" + name + "\" already exists. Overwrite it?",
                    "Duplicate Label",
                    javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            if (choice != javax.swing.JOptionPane.YES_OPTION) return;

            final LoginPanel.PendingCredentialSave save = pendingSave;
            duplicate.vaultDNS = save.vaultDns;
            duplicate.authenticationType = save.isBasic
                    ? Vault.AuthenticationType.BASIC
                    : Vault.AuthenticationType.SESSION_ID;
            if (save.isBasic) duplicate.username = save.username;
            final SavedCredential finalDuplicate = duplicate;
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                if (save.isBasic) {
                    VaultCredentialManager.setUsernamePasswordById(finalDuplicate.id, save.username, save.password);
                } else {
                    VaultCredentialManager.setSessionIdById(finalDuplicate.id, save.sessionId);
                }
            });
            com.intellij.openapi.application.ApplicationManager.getApplication().saveSettings();
            hideSaveBanner();
            return;
        }

        SavedCredential newCred = new SavedCredential();
        newCred.label = name;
        newCred.vaultDNS = pendingSave.vaultDns;
        newCred.authenticationType = pendingSave.isBasic
                ? Vault.AuthenticationType.BASIC
                : Vault.AuthenticationType.SESSION_ID;
        if (pendingSave.isBasic) newCred.username = pendingSave.username;

        appState.savedCredentials.add(newCred);
        com.intellij.openapi.application.ApplicationManager.getApplication().saveSettings();

        final LoginPanel.PendingCredentialSave save = pendingSave;
        final SavedCredential finalNewCred = newCred;
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (save.isBasic) {
                VaultCredentialManager.setUsernamePasswordById(finalNewCred.id, save.username, save.password);
            } else {
                VaultCredentialManager.setSessionIdById(finalNewCred.id, save.sessionId);
            }
        });

        hideSaveBanner();
    }

    /**
     * Hides the save credential banner.
     */
    private void hideSaveBanner() {
        pendingSave = null;
        if (saveBanner != null) {
            saveBanner.setVisible(false);
        }
        revalidate();
    }

    /**
     * Gets the top panel.
     *
     * @return The top JPanel.
     */
    private JBPanel<?> getTopPanel() {
        topPanel = new JBPanel<>(new BorderLayout());
        return topPanel;
    }

    /**
     * Gets the main panel.
     *
     * @return The main JPanel.
     */
    private JBPanel<?> getMainPanel() {
        mainPanel = new JBPanel<>(new BorderLayout());
        navigationTabs = new JBTabbedPane();
        navigationTabs.setTabPlacement(JBTabbedPane.TOP);

        navigationTabs.addTab("", ToolboxIcons.Operations, getActionPanel(), "Actions");
        navigationTabs.addTab("", ToolboxIcons.Stack, new SchemaExplorerPanel(toolboxProject), "Schema Explorer");
        navigationTabs.addTab("", ToolboxIcons.Terminal, new VqlConsolePanel(toolboxProject), "VQL Console");
        navigationTabs.addTab("", ToolboxIcons.User, getVaultInfoTabPanel(), "Vault Info");

        logoutButton.setIcon(ToolboxIcons.SignOut);
        logoutButton.setToolTipText("Logout");

        mainPanel.add(navigationTabs, BorderLayout.CENTER);

        saveBanner = buildSaveBanner();
        saveBanner.setVisible(false);
        mainPanel.add(saveBanner, BorderLayout.SOUTH);

        return mainPanel;
    }
    /**
     * Gets the bottom panel.
     *
     * @return The bottom JPanel.
     */
    private JBPanel<?> getBottomPanel() {
        bottomPanel = new JBPanel<>(new BorderLayout());
        JBTabbedPane logoutTabbedPane = new JBTabbedPane();
        logoutTabbedPane.setTabPlacement(JBTabbedPane.LEFT);
        JBPanel<?> logoutPanel = new JBPanel<>(new BorderLayout());
        logoutPanel.setPreferredSize(new Dimension(0, 0));

        logoutTabbedPane.addTab("", ToolboxIcons.SignOut, logoutPanel, "Logout");
        logoutTabbedPane.setSelectedIndex(-1);
        logoutTabbedPane.addChangeListener(e -> {
            if (logoutTabbedPane.getSelectedIndex() == -1 && toolboxProject != null) {
                if (toolboxProject.isConnected()) {
                    toolboxProject.disconnect();
                }
            }
            logoutTabbedPane.setSelectedIndex(-1);
        });

        javax.swing.border.Border topBorder = com.intellij.ui.IdeBorderFactory.createBorder(com.intellij.ui.SideBorder.TOP);
        logoutTabbedPane.setBorder(topBorder);
        currentVault.setBorder(JBUI.Borders.compound(topBorder, JBUI.Borders.emptyLeft(8)));

        bottomPanel.add(logoutTabbedPane, BorderLayout.WEST);
        bottomPanel.add(currentVault, BorderLayout.CENTER);

        return bottomPanel;
    }

    /**
     * Gets the Vault info tab panel.
     *
     * @return The Vault info tab JPanel.
     */
    private JBPanel<?> getVaultInfoTabPanel() {
        infoTabPanel = new JBPanel<>(new CardLayout());

        loginPanel = new LoginPanel(toolboxProject, true);
        vaultInfoPanel = new VaultInfoPanel(toolboxProject);
        loginPanel.setCredentialSaveHandler(pending -> this.showSaveCredentialPrompt(pending));
        toolboxProject.setCredentialSavePromptHandler(pending -> this.showSaveCredentialPrompt(pending));
        toolboxProject.setLoginTabSwitchHandler(() -> {
            if (navigationTabs != null && infoTabPanel != null) {
                navigationTabs.setSelectedComponent(infoTabPanel);
            }
        });

        infoTabPanel.add(loginPanel, "login");
        infoTabPanel.add(vaultInfoPanel, "info");

        return infoTabPanel;
    }

    /**
     * Creates the main actions panel containing the tree-based operation list.
     *
     * @return Panel containing the toolbox action tree.
     */
    private JBPanel<?> getActionPanel() {
        logTabPanel = new JBPanel<>(new BorderLayout());
        ToolboxActionPanel logOptionPanel = new ToolboxActionPanel(toolboxProject);
        logTabPanel.add(logOptionPanel, BorderLayout.CENTER);
        return logTabPanel;
    }
}
