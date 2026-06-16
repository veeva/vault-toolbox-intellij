package com.veeva.vault.toolbox.intellij.language.annotator;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * Quick-fix that replaces the text in a range with a suggested name (e.g. correcting a typo'd
 * object reference to a real object name).
 */
class ChangeReferenceFix implements IntentionAction {

    private final TextRange range;
    private final String replacement;

    ChangeReferenceFix(TextRange range, String replacement) {
        this.range = range;
        this.replacement = replacement;
    }

    @NotNull
    @Override
    public String getText() {
        return "Change to '" + replacement + "'";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Vault schema";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return file != null && range.getEndOffset() <= file.getTextLength();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }
        document.replaceString(range.getStartOffset(), range.getEndOffset(), replacement);
        PsiDocumentManager.getInstance(project).commitDocument(document);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
