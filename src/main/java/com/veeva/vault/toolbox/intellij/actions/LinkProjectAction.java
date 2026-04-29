package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.veeva.vault.toolbox.intellij.tasks.LinkProjectTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkProjectAction extends ToolboxAction {
	private static final Logger logger = LoggerFactory.getLogger(LinkProjectAction.class);

	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		super.actionPerformed(anActionEvent);
		try {
			if (toolboxProject != null && !toolboxProject.isToolboxEnabled()) {
				LinkProjectTask task = new LinkProjectTask(toolboxProject.getProject());
				task.queue();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

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
