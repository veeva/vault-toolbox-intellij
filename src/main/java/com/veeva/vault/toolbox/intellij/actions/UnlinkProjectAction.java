package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.UnlinkProjectTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlinkProjectAction extends ToolboxAction {
	private static final Logger logger = LoggerFactory.getLogger(UnlinkProjectAction.class);

	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		super.actionPerformed(anActionEvent);
		try {
			if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				UnlinkProjectTask task = new UnlinkProjectTask(toolboxProject.getProject());
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
			if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				VirtualFile virtualFile = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
				if (ToolboxProject.isProjectFile(virtualFile, anActionEvent.getProject())) {
					isEnabled = true;
					isVisible = true;
				}
			}

			anActionEvent.getPresentation().setEnabled(isEnabled);
			anActionEvent.getPresentation().setVisible(isVisible);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
