package com.veeva.vault.toolbox.intellij.language.psi;

import com.intellij.psi.tree.IElementType;
import com.veeva.vault.toolbox.intellij.language.MdlLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class MdlElementType extends IElementType {
    public MdlElementType(@NotNull @NonNls String debugName) {
        super(debugName, MdlLanguage.INSTANCE);
    }
}
