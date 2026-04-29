package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveSettingsTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(SaveSettingsTask.class);

	public SaveSettingsTask(@Nullable Project project) {
		super(project, "Saving Toolbox Settings");
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			if (toolboxProject!= null && toolboxProject.isToolboxEnabled()) {
				toolboxProject.save();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void onSuccess() {
		super.onSuccess();
		try {
			if (toolboxProject != null) {
				//Message message = toolboxProject.newMessage();
				//message.setTitle("Analyze");
				//message.append("Analyze Completed");
				//message.showInformation();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
