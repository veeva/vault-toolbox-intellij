package com.veeva.vault.toolbox.intellij.language.vql;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a VQL document in the IntelliJ PSI tree.
 */
public class VqlFile extends PsiFileBase {

    public VqlFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, VqlLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return VqlFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "VQL File";
    }
}
