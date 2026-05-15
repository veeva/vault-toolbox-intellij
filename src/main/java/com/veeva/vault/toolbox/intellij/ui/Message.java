package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import icons.MessageIcons;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;

import javax.swing.*;

/**
 * Utility class for constructing and displaying localized message dialogs within the IDE.
 * Provides a builder-like interface for creating complex message strings.
 */
public class Message {

	private final ToolboxProject toolboxProject;
	private String title = "Vault Toolbox";
	private final StringBuilder messageText = new StringBuilder();

	/**
	 * Initializes a new message builder.
	 *
	 * @param toolboxProject The toolbox project context.
	 */
	public Message(ToolboxProject toolboxProject) {
		this.toolboxProject = toolboxProject;
	}

	/**
	 * @return The current title of the message dialog.
	 */
	public String getTitle() {
		return title != null ? title : "Vault Toolbox";
	}

	/**
	 * Sets the title for the message dialog.
	 *
	 * @param title The dialog title.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Appends a new line to the message content.
	 */
	public void newLine() {
		messageText.append("\n");
	}

	/**
	 * Appends a visual separator line to the message content.
	 */
	public void appendSeparator() {
		messageText.append("\n\n-----------------------------------------------\n");
	}

	/**
	 * Appends text to the current message content.
	 *
	 * @param text The text to append.
	 */
	public void append(String text) {
		messageText.append(text);
	}

	/**
	 * Appends text to the current message content, optionally starting on a new line.
	 *
	 * @param text    The text to append.
	 * @param newLine true to prepend a newline character.
	 */
	public void append(String text, boolean newLine) {
		if (newLine) {
			messageText.append("\n");
		}
		messageText.append(text);
	}

	/**
	 * Displays the constructed message as an error dialog.
	 */
	public void showError() {
		showMessage(UIManager.getIcon(MessageIcons.Error.getName()));
	}

	/**
	 * Displays the constructed message as an information dialog.
	 *
	 * @return Always returns false (legacy return type).
	 */
	public boolean showInformation() {
		showMessage(UIManager.getIcon(MessageIcons.Information.getName()));
		return false;
	}

	/**
	 * Displays the constructed message as a warning dialog.
	 */
	public void showWarning() {
		showMessage(UIManager.getIcon(MessageIcons.Warning.getName()));
	}

	/**
	 * Internal method to show the dialog on the Event Dispatch Thread.
	 *
	 * @param dialogIcon The icon to display in the dialog.
	 */
	private void showMessage(Icon dialogIcon) {
		ApplicationManager.getApplication().invokeLater(() -> {
			if (toolboxProject.getProject() != null) {
				Messages.showMessageDialog(toolboxProject.getProject(), messageText.toString(), getTitle(), dialogIcon);
			}
		});
	}
}
