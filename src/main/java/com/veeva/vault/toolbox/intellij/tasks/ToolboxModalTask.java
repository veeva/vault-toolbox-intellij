package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ToolboxModalTask extends Task.Modal {
    protected final ToolboxProject toolboxProject;
    protected static boolean isRunning = false;

    // By default, we set canBeCancelled to 'false' so users don't accidentally abort a deployment
    public ToolboxModalTask(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title) {
        super(project, title, false);
        toolboxProject = ToolboxProject.getInstance(project);
    }

    public ToolboxModalTask(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
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
        if (toolboxProject != null) {
            toolboxProject.refresh();
        }
        isRunning = false;
    }
}