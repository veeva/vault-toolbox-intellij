package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disables the toolbox integration for the current project. The unlink operation
 * itself runs on the EDT because it triggers UI updates.
 */
public class UnlinkProjectTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(UnlinkProjectTask.class);

	/**
	 * @param project the IntelliJ project, may be {@code null}
	 */
	public UnlinkProjectTask(@Nullable Project project) {
		super(project, "Unlinking Toolbox Project", false);
	}

	/**
	 * Triggers the project unlinking process on the EDT.
	 *
	 * @param indicator the progress indicator for the background task
	 */
	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				ApplicationManager.getApplication().invokeLater(toolboxProject::unlinkProject);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
