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

public class ToolboxFileListener implements VirtualFileManagerListener, BulkFileListener {
	private static final Logger logger = LoggerFactory.getLogger(ToolboxFileListener.class);

	private final Project project;
	ToolboxProject toolboxProject;

	public ToolboxFileListener() {
		this.project = null;
		toolboxProject = null;
	}

	public ToolboxFileListener(Project project) {
		logger.debug("ToolboxFileListener");
		this.project = project;
		toolboxProject = ToolboxProject.getInstance(project);
	}

	@Override
	public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
		try {
			logger.debug("ToolboxFileListener.after");
			if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				logger.debug("FileListen.after.toolbox");
				events.forEach(event -> {
					if (event instanceof VFileEvent && event.getFile() != null) {
						VirtualFile eventFile = event.getFile();
						boolean deleteEvent = !eventFile.exists();

						if (deleteEvent) {
							if (eventFile.getPath().equals(toolboxProject.getSettingsFile().getPath())) {
								logger.debug("Toolbox setting file deleted");
								toolboxProject.unlinkProject();
							}
							//logger.debug("Removing unlinked file " + eventFile.getPath());
							//toolboxProject.removeFile(eventFile.getPath());
						}
					}
				});
			}
			BulkFileListener.super.after(events);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
