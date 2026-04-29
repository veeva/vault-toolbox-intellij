package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolboxAction extends AnAction {
	private static final Logger logger = LoggerFactory.getLogger(ToolboxAction.class);

	protected ToolboxProject toolboxProject;
	boolean isEnabled = false;
	boolean isVisible = false;

	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		return ActionUpdateThread.BGT;
	}

	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		super.update(anActionEvent);
		try {
			toolboxProject = ToolboxProject.getInstance(anActionEvent.getProject());
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

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
