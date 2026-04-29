package com.veeva.vault.toolbox.intellij.language;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enables brace matching. Note this can only be done with specific brace types and does not support any
 */
public class MdlBraceMatcher implements PairedBraceMatcher {
    @Override
    public BracePair @NotNull [] getPairs() {
        return new BracePair[] {
                new BracePair(MdlTypes.START_PAREN, MdlTypes.END_PAREN, true),
                new BracePair(MdlTypes.START_BRACE, MdlTypes.END_BRACE, false),
                new BracePair(MdlTypes.START_QUOTE, MdlTypes.END_QUOTE, false),
                new BracePair(MdlTypes.OPEN_ANGLE_BRACKET, MdlTypes.CLOSED_ANGLE_BRACKET, false)
        };
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType iElementType, @Nullable IElementType iElementType1) {
        return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile psiFile, int i) {
        return i;
    }
}
