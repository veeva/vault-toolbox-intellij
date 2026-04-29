package com.veeva.vault.toolbox.intellij.ui;

import com.veeva.vault.toolbox.intellij.project.ToolboxProject;

import javax.swing.*;

public class ToolboxPanel extends JPanel {
	ToolboxProject toolboxProject;
	public ToolboxPanel(ToolboxProject project) {
		this.toolboxProject = project;
	}
}
