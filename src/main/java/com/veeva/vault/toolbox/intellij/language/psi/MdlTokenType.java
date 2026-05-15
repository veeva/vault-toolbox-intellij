package com.veeva.vault.toolbox.intellij.language.psi;

import com.intellij.psi.tree.IElementType;
import com.veeva.vault.toolbox.intellij.language.MdlLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a leaf (lexer) token type produced by the MDL lexer. The overridden
 * {@link #toString()} prefixes the debug name so token types are easy to
 * distinguish from element types in IDE debugging tools.
 */
public class MdlTokenType extends IElementType {

    public MdlTokenType(@NotNull @NonNls String debugName) {
        super(debugName, MdlLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "MdlTokenType." + super.toString();
    }
}
