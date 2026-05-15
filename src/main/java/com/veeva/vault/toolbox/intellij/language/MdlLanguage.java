package com.veeva.vault.toolbox.intellij.language;

import com.intellij.lang.Language;

/**
 * IntelliJ language definition for Vault Metadata Definition Language (MDL).
 */
public class MdlLanguage extends Language {

    public static final MdlLanguage INSTANCE = new MdlLanguage();

    public MdlLanguage() {
        super("MDL");
    }
}
