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

final class ToolboxIconProvider extends IconProvider {
	private static final Logger logger = LoggerFactory.getLogger(ToolboxIconProvider.class);

	private ToolboxProject toolboxProject;

	@Override
	public @Nullable Icon getIcon(@NotNull PsiElement element, int flags) {
		try	{
			logger.debug("Toolbox icon provider called");
			toolboxProject = ToolboxProject.getInstance(element.getProject());
			if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				if (element.getText().toLowerCase().contains("pizza")) {
					return ToolboxIcons.Pizza;
				}
				else  if (element instanceof XmlFile xmlFile) {
					if (xmlFile.getName().equalsIgnoreCase("vaultpackage.xml")) {
						return ToolboxIcons.Xml;
					}
				}
				if (element instanceof MdlFile) {
					return ToolboxIcons.Component;
				}
				else if (element instanceof PsiDirectory psiDirectory) {
					if (psiDirectory.getVirtualFile().getPath().equalsIgnoreCase(toolboxProject.getConfigDirectory().getPath())) {
						return ToolboxIcons.ConfigFolder;
					}
					else
					if (psiDirectory.getVirtualFile().getPath().equalsIgnoreCase(toolboxProject.getLogsDirectory().getPath())) {
						return ToolboxIcons.LogsFolder;
					}
					else if (psiDirectory.getVirtualFile().getPath().equalsIgnoreCase(toolboxProject.getMdlDirectory().getPath())) {
						return ToolboxIcons.ComponentFolder;
					}
					else if (psiDirectory.getVirtualFile().getPath().equalsIgnoreCase(toolboxProject.getVpkDirectory().getPath())) {
						return ToolboxIcons.VpkFolder;
					} else if (psiDirectory.getVirtualFile().getPath().equalsIgnoreCase(toolboxProject.getToolboxDirectory().getPath())) {
                        return ToolboxIcons.Configured;
                    }

				}
				else {
					//System.out.println("Unknown element type: " + element.getClass());
				}




			}

		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
}