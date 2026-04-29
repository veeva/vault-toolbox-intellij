package com.veeva.vault.toolbox.intellij.groups;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolboxMenuGroup extends DefaultActionGroup {
	private static final Logger logger = LoggerFactory.getLogger(ToolboxMenuGroup.class);

	ToolboxProject toolboxProject;
	boolean isEnabled = false;
	boolean isVisible = false;

	@Override
	public void update(AnActionEvent anActionEvent) {
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