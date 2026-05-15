package com.veeva.vault.toolbox.intellij.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManagerListener;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Reacts to virtual file system changes that affect a Toolbox-enabled project.
 * <p>
 * Currently, this listener detects deletion of the Toolbox settings file and unlinks the project
 * accordingly so the IDE state stays consistent with the file system.
 */
public class ToolboxFileListener implements VirtualFileManagerListener, BulkFileListener {

	private static final Logger logger = LoggerFactory.getLogger(ToolboxFileListener.class);

	private final ToolboxProject toolboxProject;

	/**
	 * Creates a listener with no associated project. Used by the platform when registering this class
	 * as an application-level extension; in this mode the listener is a no-op.
	 */
	public ToolboxFileListener() {
		this.toolboxProject = null;
	}

	/**
	 * Creates a listener bound to the given project. Used by the platform when registering this class
	 * as a project-level listener.
	 *
	 * @param project the project this listener is associated with.
	 */
	public ToolboxFileListener(Project project) {
		this.toolboxProject = ToolboxProject.getInstance(project);
	}

	/**
	 * Processes the supplied batch of file events after they have been applied to the virtual file system.
	 * If the Toolbox settings file has been deleted, the project is unlinked from Toolbox.
	 *
	 * @param events the file events that have just occurred.
	 */
	@Override
	public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
		try {
			if (toolboxProject == null || !toolboxProject.isToolboxEnabled()) {
				BulkFileListener.super.after(events);
				return;
			}

			String settingsPath = toolboxProject.getSettingsFile().getPath();
			for (VFileEvent event : events) {
				VirtualFile eventFile = event.getFile();
				if (eventFile == null || eventFile.exists()) {
					continue;
				}
				if (eventFile.getPath().equals(settingsPath)) {
					logger.debug("Toolbox settings file deleted; unlinking project");
					toolboxProject.unlinkProject();
				}
			}

			BulkFileListener.super.after(events);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
