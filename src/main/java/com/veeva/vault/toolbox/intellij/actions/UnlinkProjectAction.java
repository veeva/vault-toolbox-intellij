package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.UnlinkProjectTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unlinks the active IntelliJ project from its Vault Toolbox configuration,
 * removing toolbox-specific behavior from the project.
 */
public class UnlinkProjectAction extends ToolboxAction {
	private static final Logger logger = LoggerFactory.getLogger(UnlinkProjectAction.class);

	/**
	 * Queues an {@link UnlinkProjectTask} when the project is currently linked.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		super.actionPerformed(anActionEvent);
		try {
			if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				new UnlinkProjectTask(toolboxProject.getProject()).queue();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Shows the action only when the project is linked and the project root is
	 * the current selection.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
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
