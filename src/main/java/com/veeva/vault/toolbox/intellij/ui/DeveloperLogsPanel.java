package com.veeva.vault.toolbox.intellij.ui;

import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jdesktop.swingx.JXComboBox;

import javax.swing.*;
import java.awt.*;

public class DeveloperLogsPanel extends JPanel {
    private final ToolboxProject toolboxProject;
    private final DeveloperLogsDialog.LogType initialType;

    private JXComboBox typeComboBox;

    private JPanel cardPanel;
    private CardLayout cardLayout;

    private DeveloperApiSessionPanel apiPanel;
    private DeveloperDebugSessionPanel debugPanel;
    private DeveloperProfilerSessionPanel profilerPanel;
    private DeveloperRuntimeSessionPanel runtimePanel;

    public DeveloperLogsPanel(ToolboxProject toolboxProject, DeveloperLogsDialog.LogType initialType) {
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

        typeComboBox = new JXComboBox(DeveloperLogsDialog.LogType.values());
        topPanel.add(typeComboBox);

        add(topPanel, BorderLayout.NORTH);

        typeComboBox.addActionListener(e -> updateActivePanel());
    }

    private void initCenterPanel() {
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        apiPanel = new DeveloperApiSessionPanel(toolboxProject);
        debugPanel = new DeveloperDebugSessionPanel(toolboxProject);
        profilerPanel = new DeveloperProfilerSessionPanel(toolboxProject);
        runtimePanel = new DeveloperRuntimeSessionPanel(toolboxProject);

        cardPanel.add(apiPanel, DeveloperLogsDialog.LogType.API_USAGE.name());
        cardPanel.add(debugPanel, DeveloperLogsDialog.LogType.SDK_DEBUG.name());
        cardPanel.add(profilerPanel, DeveloperLogsDialog.LogType.SDK_PROFILER.name());
        cardPanel.add(runtimePanel, DeveloperLogsDialog.LogType.SDK_RUNTIME.name());

        add(cardPanel, BorderLayout.CENTER);
    }

    private void updateActivePanel() {
        DeveloperLogsDialog.LogType selectedType = (DeveloperLogsDialog.LogType) typeComboBox.getSelectedItem();
        if (selectedType != null) {
            cardLayout.show(cardPanel, selectedType.name());
        }
    }

    protected void downloadSelectedLog() {
        DeveloperLogsDialog.LogType selectedType = (DeveloperLogsDialog.LogType) typeComboBox.getSelectedItem();
        if (selectedType == null) return;

        switch (selectedType) {
            case API_USAGE:
                if (apiPanel != null) apiPanel.downloadSelectedLogs();
                break;
            case SDK_DEBUG:
                if (debugPanel != null) debugPanel.downloadSelectedLogs();
                break;
            case SDK_PROFILER:
                if (profilerPanel != null) profilerPanel.downloadSelectedLogs();
                break;
            case SDK_RUNTIME:
                if (runtimePanel != null) runtimePanel.downloadSelectedLogs();
                break;
        }
    }
}
