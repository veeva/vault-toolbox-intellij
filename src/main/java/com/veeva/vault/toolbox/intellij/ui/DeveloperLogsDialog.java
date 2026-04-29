package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DeveloperLogsDialog extends DialogWrapper {
    public enum LogType {
        API_USAGE("API Usage"),
        SDK_DEBUG("SDK Debug"),
        SDK_PROFILER("SDK Profiler"),
        SDK_RUNTIME("SDK Runtime");

        private final String displayName;

        LogType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }

    private final ToolboxProject toolboxProject;
    private final LogType initialType;
    private DeveloperLogsPanel developerLogsPanel;

    public DeveloperLogsDialog(ToolboxProject toolboxProject, LogType initialType) {
        super(true); // use current window as parent
        this.toolboxProject = toolboxProject;
        this.initialType = initialType;
        init();
        setTitle("Developer Logs");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        developerLogsPanel = new DeveloperLogsPanel(toolboxProject, initialType);
        return developerLogsPanel;
    }

    @Override
    protected void doOKAction() {
        if (developerLogsPanel != null) {
            developerLogsPanel.downloadSelectedLog();
        }
        super.doOKAction();
    }
}
