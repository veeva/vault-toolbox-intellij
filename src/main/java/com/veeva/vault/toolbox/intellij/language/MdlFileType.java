package com.veeva.vault.toolbox.intellij.language;

import com.intellij.openapi.fileTypes.LanguageFileType;
import icons.ToolboxIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class MdlFileType extends LanguageFileType {

    public static final MdlFileType INSTANCE = new MdlFileType();

    private MdlFileType() {
        super(MdlLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "MDL File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "MDL language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "mdl";
    }

    @Override
    public Icon getIcon() {
        return ToolboxIcons.Mdl;
    }

}
