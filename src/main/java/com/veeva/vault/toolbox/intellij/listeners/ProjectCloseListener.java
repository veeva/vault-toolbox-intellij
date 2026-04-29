package com.veeva.vault.toolbox.intellij.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener to detect project closing.
 */
final class ProjectCloseListener implements ProjectManagerListener {
	private static final Logger logger = LoggerFactory.getLogger(ProjectCloseListener.class);

	@Override
	public void projectClosed(@NotNull Project project) {
		logger.debug("ProjectCloseListener.projectClosed");
		ToolboxProject toolboxProject = ToolboxProject.getInstance(project);
		if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
			logger.debug("ProjectCloseListener.projectClosed calling close instance");
			//only time this should happen is when the project is closed
			ToolboxProject.closeInstance(project);
		}

	}
}
