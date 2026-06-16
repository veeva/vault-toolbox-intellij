package com.veeva.vault.toolbox.intellij.language.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.veeva.vault.toolbox.intellij.language.MdlReferenceResolver;
import org.jetbrains.annotations.Nullable;

/**
 * Lets {@code Ctrl/Cmd-Click} (or Go To Declaration) on a reference inside an MDL string slot —
 * e.g. {@code object('product__v')} or {@code picklist('color__c')} — jump to that component's
 * extracted {@code .mdl} file. The same navigation is exposed on the editor right-click menu so it
 * does not require knowing a shortcut. Works entirely offline via {@link MdlDefinitionNavigator}.
 */
public class MdlGotoDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
        if (sourceElement == null) {
            return null;
        }
        MdlReferenceResolver.Ref ref = MdlReferenceResolver.resolve(sourceElement);
        if (ref == null) {
            return null;
        }
        PsiFile containingFile = sourceElement.getContainingFile();
        VirtualFile source = containingFile != null ? containingFile.getVirtualFile() : null;
        VirtualFile target = MdlDefinitionNavigator.findDefinition(source, ref.kind, ref.name);
        if (target == null) {
            return null;
        }
        PsiFile targetPsi = PsiManager.getInstance(sourceElement.getProject()).findFile(target);
        return targetPsi != null ? new PsiElement[]{targetPsi} : null;
    }
}
