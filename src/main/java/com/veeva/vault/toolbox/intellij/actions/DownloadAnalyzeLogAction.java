package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.ui.DeveloperLogsDialog;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens the developer log download dialog so users can retrieve API usage logs
 * from the connected vault into the project's logs directory.
 */
public class DownloadAnalyzeLogAction extends ToolboxAction {
	private static final Logger logger = LoggerFactory.getLogger(DownloadAnalyzeLogAction.class);

	/**
	 * Opens the {@link DeveloperLogsDialog} for API usage logs once a vault session
	 * has been prepared.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		super.actionPerformed(anActionEvent);
		try {
			VirtualFile virtualFile = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
			if (virtualFile != null && virtualFile.isDirectory()
					&& toolboxProject != null && toolboxProject.prepareRequest()) {
				new DeveloperLogsDialog(toolboxProject, DeveloperLogsDialog.LogType.API_USAGE).show();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Enables the action when the user has selected the configured logs directory
	 * inside a linked project.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void update(@NotNull AnActionEvent anActionEvent) {
		super.update(anActionEvent);
		try {
			if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				VirtualFile virtualFile = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
				if (virtualFile != null && virtualFile.isDirectory()
						&& virtualFile.getPath().equalsIgnoreCase(toolboxProject.getLogsDirectory().getPath())) {
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
