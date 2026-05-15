package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for configuring and confirming MDL (Metadata Definition Language) operations in Vault.
 * Provides a warning to the user about potential local file deletion during refresh operations.
 */
public class MdlDialog extends DialogWrapper {

    /**
     * Defines the types of MDL actions supported by the dialog.
     */
    public enum ActionType {
        DOWNLOAD("DOWNLOAD", "Download");

        private final String label;
        private final String typeName;

        ActionType(String typeName, String label) {
            this.typeName = typeName;
            this.label = label;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getLabel() {
            return label;
        }
    }

    private final ToolboxProject toolboxProject;
    private final ActionType actionType;

    /**
     * Initializes the MDL dialog.
     *
     * @param toolboxProject The toolbox project context.
     * @param actionType     The type of MDL action to perform.
     */
    public MdlDialog(ToolboxProject toolboxProject, ActionType actionType) {
        super(toolboxProject.getProject(), false);
        this.toolboxProject = toolboxProject;
        this.actionType = actionType;
        
        setTitle("MDL Refresh Warning");
        setModal(true);
        setResizable(false);
        init();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        return super.doValidate();
    }

    /**
     * Creates the center panel of the dialog with a warning icon and message.
     *
     * @return The center panel component.
     */
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
        
        JLabel iconLabel = new JLabel(AllIcons.General.WarningDialog);
        panel.add(iconLabel, BorderLayout.WEST);
        
        JLabel messageLabel = new JLabel("Do you want to refresh all MDL? Local components not in Vault will be deleted.");
        panel.add(messageLabel, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * Configures the dialog actions, setting the primary button text based on the action type.
     *
     * @return The array of actions for the dialog.
     */
    @NotNull
    @Override
    protected Action[] createActions() {
        this.setOKButtonText(actionType.getLabel());
        return new Action[] { getOKAction(), getCancelAction() };
    }

    /**
     * Returns an empty set of actions for the left side of the dialog footer.
     *
     * @return An empty array of Actions.
     */
    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[] {  };
    }
}
