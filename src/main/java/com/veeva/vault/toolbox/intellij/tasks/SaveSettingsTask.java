package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists the current toolbox project settings to disk.
 */
public class SaveSettingsTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(SaveSettingsTask.class);

	/**
	 * @param project the IntelliJ project, may be {@code null}
	 */
	public SaveSettingsTask(@Nullable Project project) {
		super(project, "Saving Toolbox Settings");
	}

	/**
	 * Persists the current toolbox project settings to disk.
	 *
	 * @param indicator the progress indicator for the background task
	 */
	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				toolboxProject.save();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
