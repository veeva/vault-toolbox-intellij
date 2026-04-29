package com.veeva.vault.toolbox.intellij.ui;

import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jdesktop.swingx.JXComboBox;

import javax.swing.*;
import java.awt.*;

public class DeploymentPanel extends JPanel {
    private final ToolboxProject toolboxProject;
    private final DeploymentDialog.PackageType initialType;

    private JXComboBox typeComboBox;

    private JPanel cardPanel;
    private CardLayout cardLayout;

    private LocalPackagesPanel localPanel;
    private InboundPackagesPanel inboundPanel;

    public DeploymentPanel(ToolboxProject toolboxProject, DeploymentDialog.PackageType initialType) {
        this.toolboxProject = toolboxProject;
        this.initialType = initialType;
        setLayout(new BorderLayout());
        
        setPreferredSize(new Dimension(1000, 600));

        initTopPanel();
        initCenterPanel();
        
        // Trigger initial selection
        typeComboBox.setSelectedItem(initialType);
    }

    private void initTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        typeComboBox = new JXComboBox(DeploymentDialog.PackageType.values());
        topPanel.add(typeComboBox);

        add(topPanel, BorderLayout.NORTH);

        typeComboBox.addActionListener(e -> updateActivePanel());
    }

    private void initCenterPanel() {
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        localPanel = new LocalPackagesPanel(toolboxProject);
        inboundPanel = new InboundPackagesPanel(toolboxProject);

        cardPanel.add(localPanel, DeploymentDialog.PackageType.LOCAL.name());
        cardPanel.add(inboundPanel, DeploymentDialog.PackageType.INBOUND.name());

        add(cardPanel, BorderLayout.CENTER);
    }

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
