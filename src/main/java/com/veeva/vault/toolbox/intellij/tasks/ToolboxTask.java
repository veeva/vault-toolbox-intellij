package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ToolboxTask extends Task.Backgroundable {
	protected final ToolboxProject toolboxProject;
	private final TaskResults taskResults = new TaskResults();
	protected static boolean isRunning = false;

	public ToolboxTask(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title) {
		super(project, title);
		toolboxProject = ToolboxProject.getInstance(project);
	}

	public ToolboxTask(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled) {
		super(project, title, canBeCancelled);
		toolboxProject = ToolboxProject.getInstance(project);
	}

	public ToolboxTask(@Nullable Project project, @Nullable JComponent parentComponent, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled, @Nullable PerformInBackgroundOption backgroundOption) {
		super(project, parentComponent, title, canBeCancelled, backgroundOption);
		toolboxProject = ToolboxProject.getInstance(project);
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		isRunning = true;
	}

	@Override
	public void onCancel() {
		super.onCancel();
	}

	@Override
	public void onSuccess() {
		super.onSuccess();
	}

	@Override
	public void onFinished() {
		super.onFinished();
		toolboxProject.refresh();
		isRunning = false;
	}
}
