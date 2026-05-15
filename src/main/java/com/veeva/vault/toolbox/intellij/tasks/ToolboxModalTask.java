package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for modal tasks that block the UI while operating on a {@link ToolboxProject}.
 * Modal tasks are typically used for operations that must complete before the user
 * can continue interacting with the IDE, such as deployments.
 */
public abstract class ToolboxModalTask extends Task.Modal {
    protected final ToolboxProject toolboxProject;

    /**
     * Creates a non-cancellable modal task with the given title. Cancellation is
     * disabled by default to prevent users from accidentally aborting deployments.
     *
     * @param project the IntelliJ project, may be {@code null}
     * @param title   the progress title shown to the user
     */
    public ToolboxModalTask(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title) {
        super(project, title, false);
        toolboxProject = ToolboxProject.getInstance(project);
    }

    /**
     * Creates a modal task with the given title and cancellation behavior.
     *
     * @param project        the IntelliJ project, may be {@code null}
     * @param title          the progress title shown to the user
     * @param canBeCancelled whether the user can cancel this task
     */
    public ToolboxModalTask(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
        toolboxProject = ToolboxProject.getInstance(project);
    }

    /**
     * Default implementation of the task execution logic; intended to be overridden by subclasses.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
    }

    /**
     * Ensures the toolbox project state is refreshed after the task completes (successfully or not).
     */
    @Override
    public void onFinished() {
        super.onFinished();
        if (toolboxProject != null) {
            toolboxProject.refresh();
        }
    }
}
