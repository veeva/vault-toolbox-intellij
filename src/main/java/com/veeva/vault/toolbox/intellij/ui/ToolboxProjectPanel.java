package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTabbedPane;
import com.veeva.vault.toolbox.intellij.listeners.ConnectionListener;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import icons.ToolboxIcons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class ToolboxProjectPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(ToolboxProjectPanel.class);

    JLabel currentVault = new JLabel();
    ToolboxProject toolboxProject;
    JButton logoutButton = new JButton();
    JPanel mainPanel = new JPanel();
    JTabbedPane navigationTabs = new JBTabbedPane();
    JPanel topPanel = new JPanel();
    JPanel bottomPanel = new JPanel();
    JPanel infoTabPanel;
    JPanel logTabPanel;
    LoginPanel loginPanel;
    VaultInfoPanel vaultInfoPanel;

    public ToolboxProjectPanel(Project project) {
        super(new BorderLayout());
        this.toolboxProject = ToolboxProject.getInstance(project);
        this.add(getTopPanel(), BorderLayout.NORTH);
        this.add(getMainPanel(), BorderLayout.CENTER);
        this.add(getBottomPanel(), BorderLayout.SOUTH);
        logger.debug("ToolboxProjectPanel: Add listener");
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

    public void refreshStatus() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (toolboxProject != null && toolboxProject.isConnected()) {
                if (toolboxProject.getToolWindow() != null) {
                    toolboxProject.getToolWindow().setIcon(ToolboxIcons.Connected);
                }
                currentVault.setText(toolboxProject.getVaultDNS());
                loginPanel.setVisible(false);
                vaultInfoPanel.setVisible(true);
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
                topPanel.setVisible(true);
                bottomPanel.setVisible(false);
            }
            if (vaultInfoPanel != null) {
                vaultInfoPanel.refreshVaultInfo();
            }
        }, ModalityState.any());
    }

    private JPanel getTopPanel() {
        topPanel = new JPanel(new BorderLayout());

        return topPanel;
    }

    private JPanel getMainPanel() {
        mainPanel = new JPanel(new BorderLayout());
        navigationTabs = new JBTabbedPane();
        navigationTabs.setPreferredSize(new Dimension(180, 100));
        navigationTabs.setTabPlacement(JTabbedPane.TOP);

        navigationTabs.addTab("", ToolboxIcons.Operations, getActionPanel());
        navigationTabs.addTab("", ToolboxIcons.User, getVaultInfoTabPanel());

        logoutButton.setIcon(ToolboxIcons.SignOut);
        mainPanel.add(navigationTabs, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel getBottomPanel() {
        bottomPanel = new JPanel(new BorderLayout());
        JTabbedPane logoutTabbedPane = new JBTabbedPane();
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

    private JPanel getEmptyPanel() {
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        return infoPanel;
    }

    private JPanel getVaultInfoTabPanel() {
        infoTabPanel = new JPanel(new BorderLayout());

        loginPanel = new LoginPanel(toolboxProject, true);
        vaultInfoPanel = new VaultInfoPanel(toolboxProject);

        infoTabPanel.add(loginPanel, BorderLayout.NORTH);
        infoTabPanel.add(vaultInfoPanel, BorderLayout.NORTH);

        return infoTabPanel;
    }

    /**
     * Creates the main actions for Vault Toolbox - akin to the Lifecycle panel for mavens
     *
     * @return panel containing all actions
     */
    private JPanel getActionPanel() {
        logTabPanel = new JPanel(new BorderLayout());
        ToolboxActionPanel logOptionPanel = new ToolboxActionPanel(toolboxProject);
        logTabPanel.add(logOptionPanel, BorderLayout.CENTER);
        return logTabPanel;
    }
}