package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.veeva.vault.toolbox.intellij.listeners.ConnectionListener;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import icons.ToolboxIcons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Main project-level panel for the Vault Toolbox plugin.
 * Manages the top-level navigation, login/status display, and connection state transitions.
 */
public class ToolboxProjectPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ToolboxProjectPanel.class);

    private final JLabel currentVault = new JLabel();
    private final ToolboxProject toolboxProject;
    private final JButton logoutButton = new JButton();
    private JPanel mainPanel = new JPanel();
    private JTabbedPane navigationTabs = new JTabbedPane();
    private JPanel topPanel = new JPanel();
    private JPanel bottomPanel = new JPanel();
    private JPanel infoTabPanel;
    private JPanel logTabPanel;
    private LoginPanel loginPanel;
    private VaultInfoPanel vaultInfoPanel;

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

        toolboxProject.addConnectionListener(new ConnectionListener() {
            @Override
            public void connected() {
                refreshStatus();
            }

            @Override
            public void disconnected() {
                refreshStatus();
            }
        });
        refreshStatus();
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
            }
            if (vaultInfoPanel != null) {
                vaultInfoPanel.refreshVaultInfo();
            }
        }, ModalityState.any());
    }

    /**
     * Gets the top panel.
     *
     * @return The top JPanel.
     */
    private JPanel getTopPanel() {
        topPanel = new JPanel(new BorderLayout());
        return topPanel;
    }

    /**
     * Gets the main panel.
     *
     * @return The main JPanel.
     */
    private JPanel getMainPanel() {
        mainPanel = new JPanel(new BorderLayout());
        navigationTabs = new JTabbedPane();
        navigationTabs.setPreferredSize(new Dimension(400, 100));
        navigationTabs.setTabPlacement(JTabbedPane.TOP);

        navigationTabs.addTab("", ToolboxIcons.Operations, getActionPanel());
        navigationTabs.addTab("", ToolboxIcons.User, getVaultInfoTabPanel());

        logoutButton.setIcon(ToolboxIcons.SignOut);
        mainPanel.add(navigationTabs, BorderLayout.CENTER);
        return mainPanel;
    }

    /**
     * Gets the bottom panel.
     *
     * @return The bottom JPanel.
     */
    private JPanel getBottomPanel() {
        bottomPanel = new JPanel(new BorderLayout());
        JTabbedPane logoutTabbedPane = new JTabbedPane();
        logoutTabbedPane.setTabPlacement(JTabbedPane.LEFT);
        JPanel logoutPanel = new JPanel(new BorderLayout());
        logoutPanel.setPreferredSize(new Dimension(0, 0));

        logoutTabbedPane.addTab("", ToolboxIcons.SignOut, logoutPanel);
        logoutTabbedPane.setSelectedIndex(-1);
        logoutTabbedPane.addChangeListener(e -> {
            if (logoutTabbedPane.getSelectedIndex() == -1 && toolboxProject != null) {
                if (toolboxProject.isConnected()) {
                    toolboxProject.disconnect();
                }
            }
            logoutTabbedPane.setSelectedIndex(-1);
        });

        bottomPanel.add(logoutTabbedPane, BorderLayout.WEST);
        bottomPanel.add(currentVault, BorderLayout.CENTER);

        return bottomPanel;
    }

    /**
     * Gets the Vault info tab panel.
     *
     * @return The Vault info tab JPanel.
     */
    private JPanel getVaultInfoTabPanel() {
        infoTabPanel = new JPanel(new CardLayout());

        loginPanel = new LoginPanel(toolboxProject, true);
        vaultInfoPanel = new VaultInfoPanel(toolboxProject);
        loginPanel.setCredentialSaveHandler(pending -> vaultInfoPanel.showSaveCredentialPrompt(pending));

        infoTabPanel.add(loginPanel, "login");
        infoTabPanel.add(vaultInfoPanel, "info");

        return infoTabPanel;
    }

    /**
     * Creates the main actions panel containing the tree-based operation list.
     *
     * @return Panel containing the toolbox action tree.
     */
    private JPanel getActionPanel() {
        logTabPanel = new JPanel(new BorderLayout());
        ToolboxActionPanel logOptionPanel = new ToolboxActionPanel(toolboxProject);
        logTabPanel.add(logOptionPanel, BorderLayout.CENTER);
        return logTabPanel;
    }
}
