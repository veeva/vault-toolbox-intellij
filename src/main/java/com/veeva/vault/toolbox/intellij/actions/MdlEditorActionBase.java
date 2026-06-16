package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.veeva.vault.toolbox.intellij.language.MdlFile;
import com.veeva.vault.toolbox.intellij.language.MdlReferenceResolver;
import org.jetbrains.annotations.Nullable;

/**
 * Base for the editor right-click actions that operate on the Vault reference under the caret in
 * an MDL file. These actions exist so every schema capability (navigate, find usages, show docs)
 * is reachable from the mouse menu — users never have to learn a keyboard shortcut.
 */
abstract class MdlEditorActionBase extends AnAction {

    @Nullable
    protected Editor editor(AnActionEvent e) {
        return e.getData(CommonDataKeys.EDITOR);
    }

    @Nullable
    protected PsiFile psiFile(AnActionEvent e) {
        return e.getData(CommonDataKeys.PSI_FILE);
    }

    @Nullable
    protected MdlReferenceResolver.Ref resolve(AnActionEvent e) {
        return MdlReferenceResolver.resolveAtCaret(editor(e), psiFile(e));
    }

    @Override
    public void update(AnActionEvent e) {
        boolean inMdl = psiFile(e) instanceof MdlFile && editor(e) != null;
        e.getPresentation().setVisible(inMdl);
        e.getPresentation().setEnabled(inMdl && isEnabled(resolve(e)));
    }

    /** @return whether the action applies to the resolved reference (may be {@code null}). */
    protected abstract boolean isEnabled(@Nullable MdlReferenceResolver.Ref ref);

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
