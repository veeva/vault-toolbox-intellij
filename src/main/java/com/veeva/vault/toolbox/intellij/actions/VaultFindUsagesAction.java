package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.veeva.vault.toolbox.intellij.language.MdlReferenceResolver;
import com.veeva.vault.toolbox.intellij.ui.MdlUsageSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Editor right-click action: finds where the component referenced under the caret is used across
 * the extracted MDL files, showing the matches in a navigable popup.
 */
public class VaultFindUsagesAction extends MdlEditorActionBase {

    @Override
    protected boolean isEnabled(@Nullable MdlReferenceResolver.Ref ref) {
        return ref != null && ref.name != null && !ref.name.isEmpty();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = editor(e);
        MdlReferenceResolver.Ref ref = resolve(e);
        if (project == null || editor == null || ref == null || ref.name == null) {
            return;
        }
        MdlUsageSearch.findInMdl(project, editor.getContentComponent(), ref.name);
    }
}
