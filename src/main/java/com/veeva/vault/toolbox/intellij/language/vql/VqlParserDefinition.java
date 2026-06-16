package com.veeva.vault.toolbox.intellij.language.vql;

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
 * Minimal parser definition for VQL. It produces a flat tree (all tokens under the file
 * node), which is sufficient to back an editor with syntax highlighting and completion.
 */
public class VqlParserDefinition implements ParserDefinition {

    /** The file element type for VQL. */
    public static final IFileElementType FILE = new IFileElementType(VqlLanguage.INSTANCE);

    private static final TokenSet STRINGS = TokenSet.create(VqlLexer.STRING);

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new VqlLexer();
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return STRINGS;
    }

    @NotNull
    @Override
    public PsiParser createParser(final Project project) {
        return (root, builder) -> {
            var mark = builder.mark();
            while (!builder.eof()) {
                builder.advanceLexer();
            }
            mark.done(root);
            return builder.getTreeBuilt();
        };
    }

    @NotNull
    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @NotNull
    @Override
    public PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new VqlFile(viewProvider);
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        throw new UnsupportedOperationException("VQL uses a flat tree for highlighting only");
    }
}
