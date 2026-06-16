package com.veeva.vault.toolbox.intellij.language.vql;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/**
 * Maps VQL tokens to editor colours for the VQL Console.
 */
public class VqlSyntaxHighlighter extends SyntaxHighlighterBase {

    public static final TextAttributesKey KEYWORD =
            createTextAttributesKey("VQL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey STRING =
            createTextAttributesKey("VQL_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey NUMBER =
            createTextAttributesKey("VQL_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey IDENTIFIER =
            createTextAttributesKey("VQL_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey OPERATOR =
            createTextAttributesKey("VQL_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey COMMA =
            createTextAttributesKey("VQL_COMMA", DefaultLanguageHighlighterColors.COMMA);
    public static final TextAttributesKey PARENS =
            createTextAttributesKey("VQL_PARENS", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey DOT =
            createTextAttributesKey("VQL_DOT", DefaultLanguageHighlighterColors.DOT);

    private static final TextAttributesKey[] KEYWORD_KEYS = {KEYWORD};
    private static final TextAttributesKey[] STRING_KEYS = {STRING};
    private static final TextAttributesKey[] NUMBER_KEYS = {NUMBER};
    private static final TextAttributesKey[] IDENTIFIER_KEYS = {IDENTIFIER};
    private static final TextAttributesKey[] OPERATOR_KEYS = {OPERATOR};
    private static final TextAttributesKey[] COMMA_KEYS = {COMMA};
    private static final TextAttributesKey[] PARENS_KEYS = {PARENS};
    private static final TextAttributesKey[] DOT_KEYS = {DOT};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new VqlLexer();
    }

    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(VqlLexer.KEYWORD)) {
            return KEYWORD_KEYS;
        } else if (tokenType.equals(VqlLexer.STRING)) {
            return STRING_KEYS;
        } else if (tokenType.equals(VqlLexer.NUMBER)) {
            return NUMBER_KEYS;
        } else if (tokenType.equals(VqlLexer.IDENTIFIER)) {
            return IDENTIFIER_KEYS;
        } else if (tokenType.equals(VqlLexer.OPERATOR)) {
            return OPERATOR_KEYS;
        } else if (tokenType.equals(VqlLexer.COMMA)) {
            return COMMA_KEYS;
        } else if (tokenType.equals(VqlLexer.PARENS)) {
            return PARENS_KEYS;
        } else if (tokenType.equals(VqlLexer.DOT)) {
            return DOT_KEYS;
        }
        return EMPTY_KEYS;
    }
}
