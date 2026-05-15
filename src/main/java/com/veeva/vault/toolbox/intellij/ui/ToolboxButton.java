package com.veeva.vault.toolbox.intellij.ui;

import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import javax.swing.*;

/**
 * A specialized JButton that maintains a reference to the toolbox project context.
 */
public class ToolboxButton extends JButton {
	private final ToolboxProject toolboxProject;

	/**
	 * Initializes a new toolbox button.
	 *
	 * @param project The toolbox project context.
	 */
	public ToolboxButton(ToolboxProject project) {
		this.toolboxProject = project;
	}

	/**
	 * Initializes a new toolbox button with a title.
	 *
	 * @param project The toolbox project context.
	 * @param title   The text to display on the button.
	 */
	public ToolboxButton(ToolboxProject project, String title) {
		super(title);
		this.toolboxProject = project;
	}

	/**
	 * @return The toolbox project context associated with this button.
	 */
	public ToolboxProject getToolboxProject() {
		return toolboxProject;
	}
}
