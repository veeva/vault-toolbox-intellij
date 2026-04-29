package com.veeva.vault.toolbox.intellij.services;

import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Sdk {
	private static final Logger logger = LoggerFactory.getLogger(Deploy.class);
	ToolboxProject toolboxProject;

	public Sdk(ToolboxProject toolboxProject) {
		this.toolboxProject = toolboxProject;
	}

	public java.util.List<VirtualFile> getSdkFolders(VirtualFile parent) {
		java.util.List<VirtualFile> results = new ArrayList<>();

		boolean isVpkFolder = parent.getPath().contains(toolboxProject.getVpkDirectory().getPath());
		boolean isCodeFolder = parent.getPath().endsWith("/src/main/java/com/veeva/vault/custom");

		if (isCodeFolder&& !isVpkFolder) {
			results.add(parent.getParent());
		}
		for (VirtualFile child : parent.getChildren()) {
			results.addAll(getSdkFolders(child));
		}

		return results;
	}
}
