package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBScrollPane;
import com.veeva.vault.toolbox.intellij.language.MdlReferenceResolver;
import com.veeva.vault.toolbox.intellij.language.documentation.MdlDocumentationProvider;
import com.veeva.vault.toolbox.intellij.metadata.mdl.MdlReferenceKindRegistry.RefKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Editor right-click action: shows the schema documentation (fields/values/attributes) for the
 * reference under the caret in a popup. Mouse-discoverable equivalent of Quick Documentation.
 */
public class VaultShowSchemaInfoAction extends MdlEditorActionBase {

    @Override
    protected boolean isEnabled(@Nullable MdlReferenceResolver.Ref ref) {
        return ref != null && ref.kind != RefKind.NONE;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = editor(e);
        PsiFile file = psiFile(e);
        if (project == null || editor == null || file == null) {
            return;
        }
        PsiElement leaf = file.findElementAt(editor.getCaretModel().getOffset());
        if (leaf == null) {
            return;
        }
        String html = new MdlDocumentationProvider().generateDoc(leaf, leaf);
        if (html == null) {
            Messages.showInfoMessage(project,
                    "No schema info available. Connect to a vault to load the schema.", "Vault Schema");
            return;
        }

        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        pane.setBackground(UIManager.getColor("ToolTip.background"));
        JBScrollPane scroll = new JBScrollPane(pane);
        scroll.setPreferredSize(new Dimension(360, 240));
        JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scroll, pane)
                .setTitle("Vault Schema")
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .createPopup()
                .showInBestPositionFor(editor);
    }
}
