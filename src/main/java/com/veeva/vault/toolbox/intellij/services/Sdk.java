package com.veeva.vault.toolbox.intellij.services;

import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;

import java.util.ArrayList;
import java.util.List;

/**
 * Discovers Vault Java SDK module folders within an IntelliJ project file tree.
 */
public final class Sdk {

	private static final String SDK_SOURCE_ROOT_SUFFIX = "/src/main/java/com/veeva/vault/custom";

	private Sdk() {
	}

	/**
	 * Recursively searches the file tree rooted at {@code parent} for Vault Java
	 * SDK module folders. A folder qualifies when its path ends with the canonical
	 * SDK source root and is not located inside the project's VPK directory. The
	 * parent of each matching source root is returned, since that parent represents
	 * the SDK module folder.
	 *
	 * @param toolboxProject the toolbox project context, used to locate the VPK directory
	 * @param parent         the directory to search
	 * @return the list of SDK module folders discovered under {@code parent}
	 */
	public static List<VirtualFile> getSdkFolders(ToolboxProject toolboxProject, VirtualFile parent) {
		List<VirtualFile> results = new ArrayList<>();
		String vpkPath = toolboxProject.getVpkDirectory().getPath();
		collectSdkFolders(parent, vpkPath, results);
		return results;
	}

	private static void collectSdkFolders(VirtualFile parent, String vpkPath, List<VirtualFile> results) {
		boolean isVpkFolder = parent.getPath().contains(vpkPath);
		boolean isCodeFolder = parent.getPath().endsWith(SDK_SOURCE_ROOT_SUFFIX);

		if (isCodeFolder && !isVpkFolder) {
			results.add(parent.getParent());
		}
		for (VirtualFile child : parent.getChildren()) {
			collectSdkFolders(child, vpkPath, results);
		}
	}
}
