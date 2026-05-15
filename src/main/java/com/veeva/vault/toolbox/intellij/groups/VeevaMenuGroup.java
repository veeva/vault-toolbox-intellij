package com.veeva.vault.toolbox.intellij.groups;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Menu group specifically for Veeva Vault Toolbox actions.
 * Visible and enabled only if the Toolbox is enabled for the project.
 */
public class VeevaMenuGroup extends ToolboxMenuGroup {

    /**
     * Updates the internal state for the Veeva menu group.
     * Sets isEnabled and isVisible to true if the toolbox is enabled.
     *
     * @param anActionEvent The action event
     */
    @Override
    protected void updateState(@NotNull AnActionEvent anActionEvent) {
        super.updateState(anActionEvent);
        if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
            isEnabled = true;
            isVisible = true;
        }
    }
}
