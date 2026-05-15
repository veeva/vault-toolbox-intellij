package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enables the toolbox integration for the current project. The link operation itself
 * runs on the EDT because it triggers UI updates.
 */
public class LinkProjectTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(LinkProjectTask.class);

	/**
	 * @param project the IntelliJ project, may be {@code null}
	 */
	public LinkProjectTask(@Nullable Project project) {
		super(project, "Linking Toolbox Project", false);
	}

	/**
	 * Triggers the project linking process on the EDT.
	 *
	 * @param indicator the progress indicator for the background task
	 */
	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			if (toolboxProject != null && !toolboxProject.isToolboxEnabled()) {
				ApplicationManager.getApplication().invokeLater(toolboxProject::linkProject);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
