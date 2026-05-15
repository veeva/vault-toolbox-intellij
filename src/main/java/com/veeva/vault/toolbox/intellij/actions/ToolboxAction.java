package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Vault Toolbox actions.
 * <p>
 * Resolves the current {@link ToolboxProject} from the action event and exposes
 * shared presentation flags. Subclasses must invoke {@code super.actionPerformed}
 * and {@code super.update} so the project reference and default flags are
 * initialized before they apply their own enablement rules.
 */
public class ToolboxAction extends AnAction {
	private static final Logger logger = LoggerFactory.getLogger(ToolboxAction.class);

	protected ToolboxProject toolboxProject;
	protected boolean isEnabled = false;
	protected boolean isVisible = false;

	/**
	 * Returns the thread on which {@link #update(AnActionEvent)} is invoked. Background
	 * threading is used so project state lookups do not block the EDT.
	 */
	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		return ActionUpdateThread.BGT;
	}

	/**
	 * Resolves the active {@link ToolboxProject} so subclasses can operate on it.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		try {
			toolboxProject = ToolboxProject.getInstance(anActionEvent.getProject());
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Resets the enabled/visible flags, resolves the active {@link ToolboxProject},
	 * and applies the default (hidden, disabled) presentation. Subclasses override
	 * this method to opt-in to visibility based on the current selection.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void update(@NotNull AnActionEvent anActionEvent) {
		super.update(anActionEvent);
		try {
			isEnabled = false;
			isVisible = false;
			toolboxProject = ToolboxProject.getInstance(anActionEvent.getProject());
			anActionEvent.getPresentation().setEnabled(isEnabled);
			anActionEvent.getPresentation().setVisible(isVisible);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
