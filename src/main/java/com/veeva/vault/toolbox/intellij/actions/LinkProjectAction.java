package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.veeva.vault.toolbox.intellij.tasks.LinkProjectTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Links the active IntelliJ project to a Vault Toolbox configuration so that
 * Vault SDK and configuration tooling becomes available in the project.
 */
public class LinkProjectAction extends ToolboxAction {
	private static final Logger logger = LoggerFactory.getLogger(LinkProjectAction.class);

	/**
	 * Queues a {@link LinkProjectTask} when the project has not yet been linked.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		super.actionPerformed(anActionEvent);
		try {
			if (toolboxProject != null && !toolboxProject.isToolboxEnabled()) {
				new LinkProjectTask(toolboxProject.getProject()).queue();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Shows the action only when the active project has not yet been linked.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void update(@NotNull AnActionEvent anActionEvent) {
		super.update(anActionEvent);
		try {
			if (toolboxProject != null && !toolboxProject.isToolboxEnabled()) {
				isEnabled = true;
				isVisible = true;
			}
			anActionEvent.getPresentation().setEnabled(isEnabled);
			anActionEvent.getPresentation().setVisible(isVisible);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
