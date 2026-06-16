package com.veeva.vault.toolbox.intellij.language.csv;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * Defines the CSV file type for the Vault toolbox.
 */
public final class CsvFileType extends LanguageFileType {
    
    /**
     * The singleton instance of the CsvFileType.
     */
    public static final CsvFileType INSTANCE = new CsvFileType();

    /**
     * Constructs the CsvFileType.
     */
    private CsvFileType() {
        super(CsvLanguage.INSTANCE);
    }

    /**
     * Gets the name of the file type.
     *
     * @return the file type name
     */
    @NotNull
    @Override
    public String getName() {
        return "CSV";
    }

    /**
     * Gets the description of the file type.
     *
     * @return the file type description
     */
    @NotNull
    @Override
    public String getDescription() {
        return "CSV file";
    }

    /**
     * Gets the default extension for the file type.
     *
     * @return the default extension
     */
    @NotNull
    @Override
    public String getDefaultExtension() {
        return "csv";
    }

    /**
     * Gets the icon for the file type.
     *
     * @return the icon
     */
    @Override
    public Icon getIcon() {
        return AllIcons.FileTypes.Text;
    }
}
