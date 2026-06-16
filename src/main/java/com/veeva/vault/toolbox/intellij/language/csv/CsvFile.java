package com.veeva.vault.toolbox.intellij.language.csv;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a CSV file in the IntelliJ PSI tree.
 */
public class CsvFile extends PsiFileBase {
    
    /**
     * Constructs a new CsvFile.
     *
     * @param viewProvider the file view provider
     */
    public CsvFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, CsvLanguage.INSTANCE);
    }

    /**
     * Gets the file type associated with this file.
     *
     * @return the CSV file type
     */
    @NotNull
    @Override
    public FileType getFileType() {
        return CsvFileType.INSTANCE;
    }

    /**
     * Returns a string representation of this file.
     *
     * @return the string "CSV File"
     */
    @Override
    public String toString() {
        return "CSV File";
    }
}
