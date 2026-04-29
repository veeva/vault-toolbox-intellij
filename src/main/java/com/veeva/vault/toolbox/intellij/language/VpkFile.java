package com.veeva.vault.toolbox.intellij.language;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public class VpkFile extends PsiFileBase {

    public VpkFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, VpkLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return VpkFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "VPK File";
    }

    private void test() {

    }
}
