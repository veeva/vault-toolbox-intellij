package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlinkProjectTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(UnlinkProjectTask.class);

	public UnlinkProjectTask(@Nullable Project project) {

		super(project, "Unlinking Toolbox Project", false);
	}

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
