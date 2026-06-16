package com.veeva.vault.toolbox.intellij.language.vql;

import com.intellij.openapi.fileTypes.LanguageFileType;
import icons.ToolboxIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * Defines the VQL file type backing the VQL Console editor. VQL is not associated with
 * on-disk files in normal use; the file type exists so the in-memory console document
 * gets syntax highlighting and completion.
 */
public final class VqlFileType extends LanguageFileType {

    /** The singleton instance of the VqlFileType. */
    public static final VqlFileType INSTANCE = new VqlFileType();

    private VqlFileType() {
        super(VqlLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "VQL";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Vault Query Language";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "vql";
    }

    @Override
    public Icon getIcon() {
        return ToolboxIcons.Terminal;
    }
}
