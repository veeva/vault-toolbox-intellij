package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.vfs.VirtualFile;

/**
 * Marks the selected directory as the Vault Toolbox logs directory.
 */
public class MarkAsLogsAction extends MarkAsDirectoryAction {

	@Override
	protected void setDirectory(VirtualFile virtualFile) {
		toolboxProject.setLogsDirectory(virtualFile);
	}

}
