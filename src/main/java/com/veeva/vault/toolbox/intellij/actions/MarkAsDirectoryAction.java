package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared logic for actions that designate a project directory as one of the
 * Vault Toolbox roles (config, logs, MDL, or VPK). Subclasses implement
 * {@link #setDirectory(VirtualFile)} to assign the directory on the active
 * {@link ToolboxProject}; this class handles enablement and persistence.
 */
abstract class MarkAsDirectoryAction extends ToolboxAction {
	private static final Logger logger = LoggerFactory.getLogger(MarkAsDirectoryAction.class);

	/**
	 * Assigns the selected directory to the appropriate role on the active
	 * {@link ToolboxProject}.
	 *
	 * @param virtualFile the directory selected by the user
	 */
	protected abstract void setDirectory(VirtualFile virtualFile);

	/**
	 * Records the selected directory against the active project and persists the change.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		super.actionPerformed(anActionEvent);
		try {
			VirtualFile virtualFile = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
			if (virtualFile != null && virtualFile.isDirectory()
					&& toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				setDirectory(virtualFile);
				toolboxProject.saveAsync();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Enables and shows the action when the selection is a directory inside a linked
	 * project that has not already been assigned to one of the toolbox roles.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void update(@NotNull AnActionEvent anActionEvent) {
		super.update(anActionEvent);
		try {
			if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				VirtualFile virtualFile = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
				if (virtualFile != null
						&& virtualFile.isDirectory()
						&& !ToolboxProject.isProjectFile(virtualFile, anActionEvent.getProject())
						&& !isAssignedToolboxDirectory(virtualFile)) {
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

	private boolean isAssignedToolboxDirectory(VirtualFile virtualFile) {
		String path = virtualFile.getPath();
		return toolboxProject.getConfigDirectory().getPath().equals(path)
				|| toolboxProject.getLogsDirectory().getPath().equals(path)
				|| toolboxProject.getMdlDirectory().getPath().equals(path)
				|| toolboxProject.getVpkDirectory().getPath().equals(path);
	}

}
