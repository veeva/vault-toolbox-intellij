package com.veeva.vault.toolbox.intellij.ui;

import com.veeva.vault.toolbox.intellij.project.ToolboxProject;

import javax.swing.*;

public class ToolboxButton extends JButton {
	ToolboxProject toolboxProject;
	public ToolboxButton(ToolboxProject project) {
		super();
		this.toolboxProject = project;
	}

	public ToolboxButton(ToolboxProject project, String title) {
		super(title);
		this.toolboxProject = project;
	}
}
