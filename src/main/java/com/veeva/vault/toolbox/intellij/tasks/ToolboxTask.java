package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for background tasks that operate on a {@link ToolboxProject}.
 * Subclasses should override {@link #run(ProgressIndicator)} to perform their work
 * and {@link #onSuccess()} to handle post-execution behavior on the EDT.
 */
public class ToolboxTask extends Task.Backgroundable {
	/** The toolbox project context associated with this task. */
	protected final ToolboxProject toolboxProject;

	/**
	 * Creates a cancellable background task with the given title.
	 *
	 * @param project the IntelliJ project, may be {@code null}
	 * @param title   the progress title shown to the user
	 */
	public ToolboxTask(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title) {
		super(project, title);
		toolboxProject = ToolboxProject.getInstance(project);
	}

	/**
	 * Creates a background task with the given title and cancellation behavior.
	 *
	 * @param project        the IntelliJ project, may be {@code null}
	 * @param title          the progress title shown to the user
	 * @param canBeCancelled whether the user can cancel this task
	 */
	public ToolboxTask(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled) {
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
	 * Selects and reveals {@code vFolder} in the Project tool window without stealing focus.
	 * Only navigates if the Project tool window is already visible; does not open it when
	 * hidden or in auto-hide mode to avoid it briefly flashing and closing itself.
	 * Safe to call from {@link #onSuccess()} (EDT).
	 */
	protected void selectInProjectView(VirtualFile vFolder) {
		Project project = getProject();
		if (vFolder == null || project == null) {
			return;
		}
		ToolWindow projectView = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
		if (projectView != null && projectView.isVisible()) {
			ProjectView.getInstance(project).select(null, vFolder, false);
		}
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
