package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.vfs.VirtualFile;

/**
 * Marks the selected directory as the Vault Toolbox MDL directory.
 */
public class MarkAsMdlAction extends MarkAsDirectoryAction {

	@Override
	protected void setDirectory(VirtualFile virtualFile) {
		toolboxProject.setMdlDirectory(virtualFile);
	}

}
