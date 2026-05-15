package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Dialog for confirming the extraction of MDL from the connected Vault.
 */
public class ExtractMdlDialog extends DialogWrapper {

	private final ToolboxProject toolboxProject;

	/**
	 * Initializes the Extract MDL dialog.
	 *
	 * @param toolboxProject The toolbox project context.
	 */
	public ExtractMdlDialog(ToolboxProject toolboxProject) {
		super(toolboxProject.getProject(), false);
		this.toolboxProject = toolboxProject;
		this.setModal(true);
		this.setUndecorated(true);
		this.setResizable(false);
		setTitle("Extract MDL from Vault");
		init();
	}

	@Override
	protected @Nullable ValidationInfo doValidate() {
		return super.doValidate();
	}

	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		JPanel mainPanel = new JPanel();
		mainPanel.add(new JLabel("Do you want to extract the MDL from Vault?"));
		return mainPanel;
	}

	/**
	 * Configures the dialog actions, setting the primary action text to "Extract".
	 *
	 * @return The array of actions for the dialog.
	 */
	@NotNull
	@Override
	protected Action[] createActions() {
		this.setOKButtonText("Extract");
		return new Action[] { getOKAction(), getCancelAction() };
	}

	/**
	 * Returns an empty set of actions for the left side of the dialog footer.
	 *
	 * @return An empty array of Actions.
	 */
	@NotNull
	@Override
	protected Action[] createLeftSideActions() {
		return new Action[] {  };
	}
}
