package com.veeva.vault.toolbox.intellij.ui;

import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import javax.swing.*;

/**
 * Base panel for toolbox UI components, providing a shared reference to the toolbox project context.
 */
public class ToolboxPanel extends JPanel {
	protected final ToolboxProject toolboxProject;

	/**
	 * Initializes the toolbox panel.
	 *
	 * @param project The toolbox project context.
	 */
	public ToolboxPanel(ToolboxProject project) {
		this.toolboxProject = project;
	}
}
