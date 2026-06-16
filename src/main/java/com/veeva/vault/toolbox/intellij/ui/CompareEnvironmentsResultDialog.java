package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog wrapper for the environments comparison result panel.
 */
public class CompareEnvironmentsResultDialog extends DialogWrapper {

    private final ToolboxProject toolboxProject;
    private CompareEnvironmentsPanel panel;

    /**
     * Constructs the result dialog.
     *
     * @param toolboxProject the current toolbox project
     */
    public CompareEnvironmentsResultDialog(ToolboxProject toolboxProject) {
        super(toolboxProject.getProject(), true);
        this.toolboxProject = toolboxProject;
        setTitle("Compare Environments");
        init();
    }

    /**
     * Creates the center panel of the result dialog.
     *
     * @return the center panel component
     */
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        panel = new CompareEnvironmentsPanel(toolboxProject);
        Disposer.register(getDisposable(), panel);

        JPanel root = new JPanel(new BorderLayout());
        root.setPreferredSize(new Dimension(1900, 920));
        root.add(panel, BorderLayout.CENTER);
        return root;
    }

    /**
     * Overrides close to prompt the user when there are applied changes with pending backups.
     * Closing will permanently delete all backup files.
     */
    @Override
    public void doCancelAction() {
        if (panel != null && panel.hasAppliedChanges()) {
            int result = Messages.showOkCancelDialog(
                    toolboxProject.getProject(),
                    "You have applied changes with pending backup files.\n\n" +
                    "Closing the dialog will permanently delete all backups,\n" +
                    "making applied changes irreversible from this dialog.\n\n" +
                    "Close anyway?",
                    "Close Compare Environments",
                    "Close",
                    "Cancel",
                    Messages.getWarningIcon());
            if (result != Messages.OK) return;
        }
        super.doCancelAction();
    }

    /**
     * Creates the actions for this dialog.
     *
     * @return an array of actions
     */
    @NotNull
    @Override
    protected Action[] createActions() {
        Action closeAction = getCancelAction();
        closeAction.putValue(Action.NAME, "Close");
        return new Action[]{closeAction};
    }

    /**
     * Creates the left side actions.
     *
     * @return an array of actions
     */
    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[]{};
    }
}
