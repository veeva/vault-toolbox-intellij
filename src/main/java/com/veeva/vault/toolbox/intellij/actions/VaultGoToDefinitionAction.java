package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.veeva.vault.toolbox.intellij.language.MdlReferenceResolver;
import com.veeva.vault.toolbox.intellij.language.navigation.MdlDefinitionNavigator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Editor right-click action: opens the extracted {@code .mdl} definition of the object/picklist
 * referenced under the caret. Mouse-discoverable equivalent of Ctrl/Cmd-Click navigation.
 */
public class VaultGoToDefinitionAction extends MdlEditorActionBase {

    @Override
    protected boolean isEnabled(@Nullable MdlReferenceResolver.Ref ref) {
        return ref != null && MdlDefinitionNavigator.folderFor(ref.kind) != null;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile file = psiFile(e);
        MdlReferenceResolver.Ref ref = resolve(e);
        if (project == null || file == null || ref == null) {
            return;
        }
        VirtualFile target = MdlDefinitionNavigator.findDefinition(file.getVirtualFile(), ref.kind, ref.name);
        if (target == null) {
            Messages.showInfoMessage(project,
                    "No extracted MDL found for '" + ref.name + "'. Run Extract MDL to download component definitions.",
                    "Go to Definition");
            return;
        }
        FileEditorManager.getInstance(project).openFile(target, true);
    }
}
