package com.veeva.vault.toolbox.intellij.language.psi;

import com.intellij.psi.tree.IElementType;
import com.veeva.vault.toolbox.intellij.language.MdlLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a non-terminal element type produced by the MDL parser. Each
 * {@code MdlElementType} is associated with the {@link MdlLanguage} so the IDE
 * can correctly attribute PSI nodes back to their language.
 */
public class MdlElementType extends IElementType {
    public MdlElementType(@NotNull @NonNls String debugName) {
        super(debugName, MdlLanguage.INSTANCE);
    }
}
