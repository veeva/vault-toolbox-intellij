package com.veeva.vault.toolbox.intellij.language;

import com.intellij.openapi.fileTypes.LanguageFileType;
import icons.ToolboxIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * File type registration for the {@code .vpk} extension.
 */
public final class VpkFileType extends LanguageFileType {

    public static final VpkFileType INSTANCE = new VpkFileType();

    private VpkFileType() {
        super(VpkLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "VPK File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "VPK language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "vpk";
    }

    @Override
    public Icon getIcon() {
        return ToolboxIcons.Vpk;
    }
}
