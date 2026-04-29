package com.veeva.vault.toolbox.intellij.language.psi;

import com.intellij.psi.tree.IElementType;
import com.veeva.vault.toolbox.intellij.language.MdlLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class MdlTokenType extends IElementType {

    public MdlTokenType(@NotNull @NonNls String debugName) {
        super(debugName, MdlLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "MdlTokenType." + super.toString();
    }
}
