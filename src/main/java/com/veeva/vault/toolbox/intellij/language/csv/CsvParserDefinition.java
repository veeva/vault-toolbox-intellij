package com.veeva.vault.toolbox.intellij.language.csv;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

/**
 * Parser definition for the CSV language.
 */
public class CsvParserDefinition implements ParserDefinition {
    
    /** The file element type for CSV. */
    public static final IFileElementType FILE = new IFileElementType(CsvLanguage.INSTANCE);

    /**
     * Creates a lexer for parsing CSV files.
     *
     * @param project the project
     * @return the lexer
     */
    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new CsvLexer();
    }

    /**
     * Returns the set of tokens representing comments.
     *
     * @return an empty token set
     */
    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    /**
     * Returns the set of tokens representing string literals.
     *
     * @return an empty token set
     */
    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return TokenSet.EMPTY;
    }

    /**
     * Creates a parser for CSV files.
     *
     * @param project the project
     * @return the parser
     */
    @NotNull
    @Override
    public PsiParser createParser(final Project project) {
        return (root, builder) -> {
            builder.setDebugMode(true);
            var mark = builder.mark();
            while (!builder.eof()) {
                builder.advanceLexer();
            }
            mark.done(root);
            return builder.getTreeBuilt();
        };
    }

    /**
     * Gets the file node type.
     *
     * @return the file node type
     */
    @NotNull
    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    /**
     * Creates a PSI file for the given view provider.
     *
     * @param viewProvider the file view provider
     * @return the created PSI file
     */
    @NotNull
    @Override
    public PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new CsvFile(viewProvider);
    }

    /**
     * Creates a PSI element for the given AST node.
     *
     * @param node the AST node
     * @return the created PSI element
     */
    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        throw new UnsupportedOperationException("Not supported for basic CSV highlighter");
    }
}
