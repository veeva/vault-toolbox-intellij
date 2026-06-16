package com.veeva.vault.toolbox.intellij.language.vql;

import com.intellij.lang.Language;

/**
 * Defines the VQL (Vault Query Language) language used by the VQL Console editor.
 */
public class VqlLanguage extends Language {

    /** The singleton instance of the VqlLanguage. */
    public static final VqlLanguage INSTANCE = new VqlLanguage();

    private VqlLanguage() {
        super("VQL");
    }
}
