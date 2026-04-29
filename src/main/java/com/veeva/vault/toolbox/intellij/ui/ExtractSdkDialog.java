package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ExtractSdkDialog extends DialogWrapper {

	ToolboxProject toolboxProject;
	MdlDialog.ActionType actionType;

	JPanel mainPanel = new JPanel();
	public ExtractSdkDialog(ToolboxProject toolboxProject) {
		super(false);
		this.toolboxProject = toolboxProject;
		this.setModal(true);
		this.setUndecorated(true);
		this.setResizable(false);
		init();
	}

	@Override
	protected @Nullable ValidationInfo doValidate() {
		return super.doValidate();
	}

    /*
    private class ExecuteAction extends DialogWrapperAction {
        protected ExecuteAction() {
            super("Execute");
            putValue(Action.NAME, actionType.getLabel());
        }

        @Override
        protected void doAction(ActionEvent e) {
            ValidationInfo validationInfo = doValidate();
            if (validationInfo == null) {
                //getOKAction().setEnabled(isOkEnabled());
                doOKAction();
            }
            else {
                String message = validationInfo.message;
                Messages.showMessageDialog(mainPanel, message, CommonBundle.getErrorTitle(), Messages.getErrorIcon());
            }
            // set implementation specific values to signal that this custom button was the cause for closing the dialog
            // .....

        }
    }

     */

	boolean isOkEnabled() {
		// return true if dialog can be closed
		return true;
	}

	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		mainPanel.add(new JLabel("Do you want to refresh all MDL? Local components not in Vault will be deleted"));
		return mainPanel;
	}

	@NotNull
	@Override
	protected Action[] createActions() {
		super.createDefaultActions();
		// return right hand side action buttons
		this.setOKButtonText(actionType.getLabel());
		return new Action[] { getOKAction(), getCancelAction() };
	}

	@NotNull
	protected Action[] createLeftSideActions() {
		// return left hand side action buttons
		return new Action[] {  };
	}
}