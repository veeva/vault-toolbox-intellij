package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import icons.MessageIcons;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;

import javax.swing.*;

public class Message {

	ToolboxProject toolboxProject;
	String title = "Vault Toolbox";
	StringBuilder messageText = new StringBuilder();

	public Message(ToolboxProject toolboxProject) {
		this.toolboxProject = toolboxProject;
		this.title = title;
	}

	public String getTitle() {
		if (title == null) {
			return "Vault Toolbox";
		}
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void newLine() {
		messageText.append("\n");
	}

	public void appendSeparator() {
		messageText.append("\n\n-----------------------------------------------\n");
	}

	public void append(String text) {
		messageText.append(text);
	}

	public void append(String text, boolean newLine) {
		if (newLine) {
			messageText.append("\n");
		}

		messageText.append(text);
	}

	public void showError() {
		showMessage(UIManager.getIcon(MessageIcons.Error.getName()));
	}

	public boolean showInformation() {
		showMessage(UIManager.getIcon(MessageIcons.Information.getName()));
		return false;
	}

	public void showWarning() {
		showMessage(UIManager.getIcon(MessageIcons.Warning.getName()));
	}

	public void showMessage(Icon dialogIcon) {
		ApplicationManager.getApplication().invokeLater(() -> {
			if (toolboxProject.getProject() != null) {
				Messages.showMessageDialog(toolboxProject.getProject(), messageText.toString(), getTitle(), dialogIcon);
			}
		});
	}
}
