package com.veeva.vault.toolbox.intellij.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Releases Toolbox resources associated with a project when it is closed.
 */
final class ProjectCloseListener implements ProjectManagerListener {

	private static final Logger logger = LoggerFactory.getLogger(ProjectCloseListener.class);

	/**
	 * Closes the {@link ToolboxProject} instance bound to the given project, if Toolbox is enabled for it.
	 *
	 * @param project the project that has just been closed.
	 */
	@Override
	public void projectClosed(@NotNull Project project) {
		logger.debug("ProjectCloseListener.projectClosed");
		ToolboxProject toolboxProject = ToolboxProject.getInstance(project);
		if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
			ToolboxProject.closeInstance(project);
		}
	}
}
