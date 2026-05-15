package com.veeva.vault.toolbox.intellij.language;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

/**
 * PSI file representation for an MDL source file.
 */
public class MdlFile extends PsiFileBase {

    public MdlFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, MdlLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return MdlFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "MDL File";
    }
}
