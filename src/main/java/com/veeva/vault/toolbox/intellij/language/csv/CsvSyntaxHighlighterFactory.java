package com.veeva.vault.toolbox.intellij.language.csv;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating CSV syntax highlighters.
 */
public class CsvSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    
    /**
     * Gets the syntax highlighter for the given project and virtual file.
     *
     * @param project     the project
     * @param virtualFile the virtual file
     * @return the syntax highlighter
     */
    @NotNull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile) {
        return new CsvSyntaxHighlighter();
    }
}
