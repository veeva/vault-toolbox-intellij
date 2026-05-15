package com.veeva.vault.toolbox.intellij.language;

import com.intellij.lang.Language;

/**
 * IntelliJ language definition for Vault Package (VPK) files.
 */
public class VpkLanguage extends Language {

    public static final VpkLanguage INSTANCE = new VpkLanguage();

    public VpkLanguage() {
        super("VPK");
    }
}
