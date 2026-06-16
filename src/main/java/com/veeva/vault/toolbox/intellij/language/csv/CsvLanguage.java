package com.veeva.vault.toolbox.intellij.language.csv;

import com.intellij.lang.Language;

/**
 * Defines the CSV language for the Vault toolbox.
 */
public class CsvLanguage extends Language {
    
    /**
     * The singleton instance of the CsvLanguage.
     */
    public static final CsvLanguage INSTANCE = new CsvLanguage();

    /**
     * Constructs the CsvLanguage.
     */
    private CsvLanguage() {
        super("VaultCSV");
    }
}
