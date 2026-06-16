package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.settings.AppSettingsConfigurable;
import icons.ToolboxIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

import java.awt.Component;
import java.awt.Dimension;

/**
 * Factory class for creating and managing the Vault Toolbox tool window.
 * This class is "DumbAware", meaning it can be active while the project is being indexed.
 */
final class ToolWindowPanel implements ToolWindowFactory, DumbAware {

	/**
	 * Wrapper to enforce a strict minimum visual layout size while allowing the parent to shrink smaller,
	 * causing the layout to clip rather than squishing child components and triggering layout loop bugs.
	 */
	private static class ClippingWrapper extends JPanel {
		private final Component child;

		public ClippingWrapper(Component child) {
			super(null); // Absolute positioning to disable automatic squishing
			this.child = child;
			add(child);
		}

		@Override
		public void doLayout() {
			int w = Math.max(getWidth(), 400);
			int h = Math.max(getHeight(), 240);
			child.setBounds(0, 0, w, h);
		}

		@Override
		public Dimension getMinimumSize() {
			return new Dimension(0, 0);
		}

		@Override
		public Dimension getPreferredSize() {
			return child.getPreferredSize();
		}
	}

	/**
	 * Performs additional initialization when the tool window is created.
	 * Sets the tool window in the {@link ToolboxProject} and updates its icon based on the connection status.
	 *
	 * @param toolWindow The tool window instance being initialized.
	 */
	@Override
	public void init(@NotNull ToolWindow toolWindow) {
		ToolWindowFactory.super.init(toolWindow);
		ToolboxProject toolboxProject = ToolboxProject.getInstance(toolWindow.getProject());
		toolboxProject.setToolWindow(toolWindow);
		toolWindow.setIcon(toolboxProject.isConnected() ? ToolboxIcons.Connected : ToolboxIcons.Disconnected);
	}

	/**
	 * Creates the content for the tool window.
	 * This method initializes the {@link ToolWindowContent} and adds it to the tool window's content manager.
	 *
	 * @param project    The current project.
	 * @param toolWindow The tool window instance.
	 */
	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		ToolWindowContent toolWindowContent = new ToolWindowContent(toolWindow, project);
		ClippingWrapper wrapper = new ClippingWrapper(toolWindowContent.getContentPanel());
		Content content = ContentFactory.getInstance().createContent(wrapper, "", false);
		toolWindow.getContentManager().addContent(content);

		AnAction settingsAction = new AnAction("Settings", "Open Vault Toolbox Settings", ToolboxIcons.Gear) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				ShowSettingsUtil.getInstance().showSettingsDialog(project, AppSettingsConfigurable.class);
			}
		};
		toolWindow.setTitleActions(List.of(settingsAction));
	}

	/**
	 * Determines if the tool window should be available based on whether the Toolbox is enabled for the project.
	 *
	 * @param project The current project.
	 * @return true if the tool window should be shown.
	 */
	@Override
	public boolean shouldBeAvailable(@NotNull Project project) {
		ToolboxProject toolboxProject = ToolboxProject.getInstance(project);
		return toolboxProject != null && toolboxProject.isToolboxEnabled();
	}

	/**
	 * Internal class to manage the content components of the Tool Window.
	 */
	private static class ToolWindowContent {
		private final ToolboxProjectPanel contentPanel;
		private final ToolboxProject toolboxProject;
		private final ToolWindow toolWindow;
		private final Project project;

		/**
		 * Initializes the content panel for the tool window.
		 *
		 * @param toolWindow The tool window instance.
		 * @param project    The current project.
		 */
		public ToolWindowContent(ToolWindow toolWindow, Project project) {
			this.toolWindow = toolWindow;
			this.project = project;
			this.toolboxProject = ToolboxProject.getInstance(project);
			this.contentPanel = new ToolboxProjectPanel(project);
		}

		/**
		 * @return The main UI panel for the toolbox.
		 */
		public JPanel getContentPanel() {
			return contentPanel;
		}
	}
}
