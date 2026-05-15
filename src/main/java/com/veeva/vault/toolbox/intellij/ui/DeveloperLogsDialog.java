package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * A dialog wrapper that hosts the developer logs management interface.
 * Supports viewing and managing different types of Vault developer logs including API Usage,
 * SDK Debug, SDK Profiler, and SDK Runtime logs.
 */
public class DeveloperLogsDialog extends DialogWrapper {

    /**
     * Defines the supported types of developer logs.
     */
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

    /**
     * Initializes the developer logs dialog.
     *
     * @param toolboxProject The toolbox project context.
     * @param initialType    The log type to display initially.
     */
    public DeveloperLogsDialog(ToolboxProject toolboxProject, LogType initialType) {
        super(toolboxProject.getProject(), true);
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

    /**
     * Configures the dialog actions, providing only a "Close" button.
     *
     * @return The array of actions for the dialog.
     */
    @NotNull
    @Override
    protected Action[] createActions() {
        Action closeAction = getCancelAction();
        closeAction.putValue(Action.NAME, "Close");
        return new Action[]{closeAction};
    }
}
