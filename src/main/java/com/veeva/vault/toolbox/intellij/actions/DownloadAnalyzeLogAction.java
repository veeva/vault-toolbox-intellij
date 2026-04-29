package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.ui.DeveloperLogsDialog;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadAnalyzeLogAction extends ToolboxAction {
	private static final Logger logger = LoggerFactory.getLogger(DownloadAnalyzeLogAction.class);

	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		super.actionPerformed(anActionEvent);
		try {
			VirtualFile virtualFile = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
			if (virtualFile != null && virtualFile.isDirectory()) {
				if (toolboxProject!= null && toolboxProject.prepareRequest()) {
					DeveloperLogsDialog logsDialog = new DeveloperLogsDialog(toolboxProject, DeveloperLogsDialog.LogType.API_USAGE);
					logsDialog.show();
				}
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
				if (virtualFile != null) {
					if (virtualFile.isDirectory()) {
						if (virtualFile.getPath().equalsIgnoreCase(toolboxProject.getLogsDirectory().getPath())) {
							isEnabled = true;
							isVisible = true;
						}
					}
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
