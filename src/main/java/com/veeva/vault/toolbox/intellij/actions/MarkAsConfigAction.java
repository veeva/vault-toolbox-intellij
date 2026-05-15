package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.vfs.VirtualFile;

/**
 * Marks the selected directory as the Vault Toolbox configuration directory.
 */
public class MarkAsConfigAction extends MarkAsDirectoryAction {

	@Override
	protected void setDirectory(VirtualFile virtualFile) {
		toolboxProject.setConfigDirectory(virtualFile);
	}

}
