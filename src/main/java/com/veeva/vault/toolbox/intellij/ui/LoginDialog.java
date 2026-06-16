package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * A modal dialog that wraps the {@link LoginPanel} to facilitate Vault authentication.
 */
public class LoginDialog extends DialogWrapper {

    private final ToolboxProject toolboxProject;
    private final LoginPanel loginControl;

    /**
     * Initializes the login dialog with the specified project context.
     *
     * @param toolboxProject The toolbox project context.
     */
    public LoginDialog(ToolboxProject toolboxProject) {
        super(toolboxProject.getProject(), true);
        this.toolboxProject = toolboxProject;
        this.loginControl = new LoginPanel(toolboxProject, false);
        this.loginControl.setCredentialSaveHandler(pending -> toolboxProject.promptToSaveCredential(pending));
        this.setModal(true);
        this.setUndecorated(true);
        this.setResizable(false);
        init();
    }

    /**
     * Overrides the default OK action to perform asynchronous login through the {@link LoginPanel}.
     * Provides visual feedback by updating the button text during the connection attempt.
     */
    @Override
    protected void doOKAction() {
        JButton okButton = this.getButton(this.getOKAction());

        if (okButton != null) {
            okButton.setText("Connecting...");
            okButton.setEnabled(false);
        }

        loginControl.doAsyncLogin(
                () -> {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                        if (okButton != null) {
                            okButton.setEnabled(true);
                            okButton.setText("Login");
                        }
                        this.close(OK_EXIT_CODE);
                    });
                },
                () -> {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                        if (okButton != null) {
                            okButton.setText("Login");
                            okButton.setEnabled(true);
                        }
                    });
                }
        );
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return loginControl;
    }

    /**
     * Configures the default actions for the dialog, renaming the OK action to "Login".
     *
     * @return The array of actions available in the dialog.
     */
    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();
        this.setOKButtonText("Login");
        return new Action[] { getOKAction(), getCancelAction() };
    }

    /**
     * Returns an empty set of actions for the left side of the dialog footer.
     *
     * @return An empty array of Actions.
     */
    @NotNull
    protected Action[] createLeftSideActions() {
        return new Action[] {  };
    }
}
