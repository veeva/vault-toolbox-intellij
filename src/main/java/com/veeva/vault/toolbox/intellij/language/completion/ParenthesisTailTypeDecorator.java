package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.TailTypeDecorator;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.Nullable;

/**
 * Lookup element decorator that, after a completion is accepted, inserts a pair of
 * parentheses and positions the caret between them. Used for completions whose
 * resulting tokens take an argument list (for example MDL attributes).
 */
class ParenthesisTailTypeDecorator extends TailTypeDecorator<LookupElement> {

    public ParenthesisTailTypeDecorator(LookupElement delegate) {
        super(delegate);
    }

    @Override
    protected @Nullable TailType computeTailType(InsertionContext insertionContext) {
        return new ParenthesisTailType();
    }

    private static class ParenthesisTailType extends TailType {
        @Override
        public int processTail(Editor editor, int tailOffSet) {
            tailOffSet = insertChar(editor, tailOffSet, '(');
            tailOffSet = insertChar(editor, tailOffSet, ')');
            moveCaret(editor, tailOffSet, -1);
            return tailOffSet;
        }
    }
}
