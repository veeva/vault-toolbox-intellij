package com.veeva.vault.toolbox.intellij.groups;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Toolbox menu groups.
 * Handles common initialization and default visibility/enabled state.
 */
public class ToolboxMenuGroup extends DefaultActionGroup {
    private static final Logger logger = LoggerFactory.getLogger(ToolboxMenuGroup.class);

    protected ToolboxProject toolboxProject;
    protected boolean isEnabled = false;
    protected boolean isVisible = false;

    /**
     * Updates the state of the menu group.
     *
     * @param anActionEvent The action event
     */
    @Override
    public void update(AnActionEvent anActionEvent) {
        super.update(anActionEvent);
        try {
            updateState(anActionEvent);
            updatePresentation(anActionEvent);
        } catch (Exception e) {
            logger.error("Error updating Toolbox menu group", e);
        }
    }

    /**
     * Updates the internal state for the menu group.
     * Subclasses should override this to set isEnabled and isVisible.
     *
     * @param anActionEvent The action event
     */
    protected void updateState(AnActionEvent anActionEvent) {
        isEnabled = false;
        isVisible = false;
        toolboxProject = ToolboxProject.getInstance(anActionEvent.getProject());
    }

    /**
     * Updates the presentation based on the current internal state.
     *
     * @param anActionEvent The action event
     */
    protected void updatePresentation(AnActionEvent anActionEvent) {
        anActionEvent.getPresentation().setEnabled(isEnabled);
        anActionEvent.getPresentation().setVisible(isVisible);
    }
}
