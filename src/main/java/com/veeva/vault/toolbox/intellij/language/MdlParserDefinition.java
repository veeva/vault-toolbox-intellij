package com.veeva.vault.toolbox.intellij.language;

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
import com.veeva.vault.toolbox.intellij.language.parser.MdlParser;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Wires the lexer, parser, and PSI factories for MDL together so the IntelliJ
 * platform can build a syntax tree from MDL source files.
 */
public class MdlParserDefinition implements ParserDefinition {
    public static final IFileElementType FILE = new IFileElementType(MdlLanguage.INSTANCE);

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        return new MdlLexerAdapter();
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return MdlTokenSets.COMMENTS;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return TokenSet.EMPTY;
    }

    @NotNull
    @Override
    public PsiParser createParser(final Project project) {
        return new MdlParser();
    }

    @NotNull
    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @NotNull
    @Override
    public PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new MdlFile(viewProvider);
    }

    @NotNull
    @Override
    public PsiElement createElement(ASTNode node) {
        return MdlTypes.Factory.createElement(node);
    }
}
