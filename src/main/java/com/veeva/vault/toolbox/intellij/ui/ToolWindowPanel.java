// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import icons.ToolboxIcons;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

final class ToolWindowPanel implements ToolWindowFactory, DumbAware {
	private static final Logger logger = LoggerFactory.getLogger(ToolWindowPanel.class);

	@Override
	public void init(@NotNull ToolWindow toolWindow) {
		logger.debug("ToolWindowPanel.init");
		ToolWindowFactory.super.init(toolWindow);
		ToolboxProject toolboxProject = ToolboxProject.getInstance(toolWindow.getProject());
		toolboxProject.setToolWindow(toolWindow);
		toolWindow.setIcon(toolboxProject.isConnected() ? ToolboxIcons.Connected : ToolboxIcons.Disconnected);
	}

	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		logger.debug("ToolboxPanel.createToolWindowContent");
		ToolWindowContent toolWindowContent = new ToolWindowContent(toolWindow, project);
		Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "", false);
		toolWindow.getContentManager().addContent(content);
	}

	@Override
	public @Nullable Object isApplicableAsync(@NotNull Project project, @NotNull Continuation<? super Boolean> $completion) {
		logger.debug("ToolboxPanel.isApplicableAsync");
		//ToolboxProject toolboxProject = ToolboxProject.getInstance(project);
		//return toolboxProject != null && toolboxProject.isToolboxEnabled();
		return ToolWindowFactory.super.isApplicableAsync(project, $completion);
	}

	@Override
	public @Nullable Icon getIcon() {
		logger.debug("ToolboxPanel.getIcon");
		return ToolWindowFactory.super.getIcon();
	}

	@Override
	public @Nullable Object manage(@NotNull ToolWindow toolWindow, @NotNull ToolWindowManager toolWindowManager, @NotNull Continuation<? super Unit> $completion) {
		logger.debug("ToolboxPanel.manage");
		return ToolWindowFactory.super.manage(toolWindow, toolWindowManager, $completion);
	}

	@Override
	public boolean shouldBeAvailable(@NotNull Project project) {
		ToolboxProject toolboxProject = ToolboxProject.getInstance(project);
		return toolboxProject != null & toolboxProject.isToolboxEnabled();
	}


	private static class ToolWindowContent {
		private final ToolboxProjectPanel contentPanel;
		private final ToolboxProject toolboxProject;
		private final ToolWindow toolWindow;
		private final Project project;

		public ToolWindowContent(ToolWindow toolWindow, Project project) {
			logger.debug("ToolboxPanel.ToolWindowContent");
			this.toolWindow = toolWindow;
			this.project = project;
			this.toolboxProject = ToolboxProject.getInstance(project);
			this.contentPanel = new ToolboxProjectPanel(project);
		}

		public JPanel getContentPanel() {
			logger.debug("ToolboxPanel.getContentPanel");
			return contentPanel;
		}
	}
}
