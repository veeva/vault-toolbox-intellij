package com.veeva.vault.toolbox.intellij.ui;

import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jdesktop.swingx.JXComboBox;

import javax.swing.*;
import java.awt.*;

/**
 * Main panel for package deployment management. 
 * Provides a switcher to toggle between {@link LocalPackagesPanel} and {@link InboundPackagesPanel}.
 */
public class DeploymentPanel extends JPanel {
    private final ToolboxProject toolboxProject;
    private final DeploymentDialog.PackageType initialType;

    private JXComboBox typeComboBox;

    private JPanel cardPanel;
    private CardLayout cardLayout;

    private LocalPackagesPanel localPanel;
    private InboundPackagesPanel inboundPanel;

    /**
     * Initializes the deployment panel.
     *
     * @param toolboxProject The toolbox project context.
     * @param initialType    The type of package view to display initially.
     */
    public DeploymentPanel(ToolboxProject toolboxProject, DeploymentDialog.PackageType initialType) {
        this.toolboxProject = toolboxProject;
        this.initialType = initialType;
        setLayout(new BorderLayout());
        
        setPreferredSize(new Dimension(1000, 600));

        initTopPanel();
        initCenterPanel();
        
        typeComboBox.setSelectedItem(initialType);
    }

    /**
     * Configures the top toolbar containing the package type switcher.
     */
    private void initTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        typeComboBox = new JXComboBox(DeploymentDialog.PackageType.values());
        topPanel.add(typeComboBox);

        add(topPanel, BorderLayout.NORTH);

        typeComboBox.addActionListener(e -> updateActivePanel());
    }

    /**
     * Configures the center area using a CardLayout to host different package panels.
     */
    private void initCenterPanel() {
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        localPanel = new LocalPackagesPanel(toolboxProject);
        inboundPanel = new InboundPackagesPanel(toolboxProject);

        cardPanel.add(localPanel, DeploymentDialog.PackageType.LOCAL.name());
        cardPanel.add(inboundPanel, DeploymentDialog.PackageType.INBOUND.name());

        add(cardPanel, BorderLayout.CENTER);
    }

    /**
     * Switches the visible card based on the current selection in the type dropdown 
     * and triggers a data refresh for the newly active panel.
     */
    private void updateActivePanel() {
        DeploymentDialog.PackageType selectedType = (DeploymentDialog.PackageType) typeComboBox.getSelectedItem();
        if (selectedType != null) {
            cardLayout.show(cardPanel, selectedType.name());
            if (selectedType == DeploymentDialog.PackageType.LOCAL) {
                localPanel.loadData();
            } else if (selectedType == DeploymentDialog.PackageType.INBOUND) {
                inboundPanel.loadData();
            }
        }
    }
}
