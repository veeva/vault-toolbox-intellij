package com.veeva.vault.toolbox.intellij.listeners;

import com.intellij.ide.IconProvider;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import com.veeva.vault.toolbox.intellij.language.MdlFile;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import icons.ToolboxIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Supplies Toolbox-specific icons for files and directories that belong to a Toolbox-enabled project.
 */
final class ToolboxIconProvider extends IconProvider {

	private static final Logger logger = LoggerFactory.getLogger(ToolboxIconProvider.class);

	private static final String VAULT_PACKAGE_FILE_NAME = "vaultpackage.xml";

	/**
	 * Resolves the icon to display for the supplied PSI element. Returns {@code null} when the element
	 * is not part of a Toolbox-enabled project or does not match a recognized Toolbox file or directory,
	 * allowing other icon providers to take over.
	 *
	 * @param element the PSI element whose icon is being requested.
	 * @param flags   IntelliJ icon flags (currently unused).
	 * @return the icon to use, or {@code null} to defer to other providers.
	 */
	@Override
	public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
		try {
			ToolboxProject toolboxProject = ToolboxProject.getInstance(element.getProject());
			if (toolboxProject == null || !toolboxProject.isToolboxEnabled()) {
				return null;
			}

			if (element instanceof MdlFile) {
				return ToolboxIcons.Component;
			}

			if (element instanceof XmlFile xmlFile) {
				if (VAULT_PACKAGE_FILE_NAME.equalsIgnoreCase(xmlFile.getName())) {
					return ToolboxIcons.Xml;
				}
				return null;
			}

			if (element instanceof PsiDirectory psiDirectory) {
				return resolveDirectoryIcon(psiDirectory, toolboxProject);
			}

			return null;
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Resolves the Toolbox icon associated with one of the well-known Toolbox project directories.
	 *
	 * @param psiDirectory   the directory whose icon is being resolved.
	 * @param toolboxProject the Toolbox project providing the well-known directory paths.
	 * @return the matching Toolbox icon, or {@code null} if the directory is not a recognized Toolbox directory.
	 */
	private @Nullable Icon resolveDirectoryIcon(@NotNull PsiDirectory psiDirectory, @NotNull ToolboxProject toolboxProject) {
		String directoryPath = psiDirectory.getVirtualFile().getPath();

		if (directoryPath.equalsIgnoreCase(toolboxProject.getConfigDirectory().getPath())) {
			return ToolboxIcons.ConfigFolder;
		}
		if (directoryPath.equalsIgnoreCase(toolboxProject.getLogsDirectory().getPath())) {
			return ToolboxIcons.LogsFolder;
		}
		if (directoryPath.equalsIgnoreCase(toolboxProject.getMdlDirectory().getPath())) {
			return ToolboxIcons.ComponentFolder;
		}
		if (directoryPath.equalsIgnoreCase(toolboxProject.getVpkDirectory().getPath())) {
			return ToolboxIcons.VpkFolder;
		}
		if (directoryPath.equalsIgnoreCase(toolboxProject.getToolboxDirectory().getPath() + "/sdk")) {
			return ToolboxIcons.CodeFile;
		}
		if (directoryPath.equalsIgnoreCase(toolboxProject.getToolboxDirectory().getPath())) {
			return ToolboxIcons.Configured;
		}
		return null;
	}
}
