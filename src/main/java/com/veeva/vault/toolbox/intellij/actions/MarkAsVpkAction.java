package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.vfs.VirtualFile;

/**
 * Marks the selected directory as the Vault Toolbox VPK directory.
 */
public class MarkAsVpkAction extends MarkAsDirectoryAction {

	@Override
	protected void setDirectory(VirtualFile virtualFile) {
		toolboxProject.setVpkDirectory(virtualFile);
	}

}
