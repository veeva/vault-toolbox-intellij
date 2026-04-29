package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class LoginDialog extends DialogWrapper {

    private final ToolboxProject toolboxProject;
    private final LoginPanel loginControl;

    public LoginDialog(ToolboxProject toolboxProject) {
        super(true);
        this.toolboxProject = toolboxProject;
        this.loginControl = new LoginPanel(toolboxProject, false);
        this.setModal(true);
        this.setUndecorated(true);
        this.setSize(400, 400);
        this.setResizable(false);
        init();
    }

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

    @FunctionalInterface
    public interface FieldListener extends DocumentListener {
        void update(DocumentEvent e);

        @Override
        default void insertUpdate(DocumentEvent e) { update(e); }
        @Override
        default void removeUpdate(DocumentEvent e) { update(e); }
        @Override
        default void changedUpdate(DocumentEvent e) { update(e); }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return loginControl;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();
        this.setOKButtonText("Login");
        return new Action[] { getOKAction(), getCancelAction() };
    }

    @NotNull
    protected Action[] createLeftSideActions() {
        return new Action[] {  };
    }
}