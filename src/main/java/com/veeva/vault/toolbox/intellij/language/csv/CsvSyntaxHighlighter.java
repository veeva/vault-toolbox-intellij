package com.veeva.vault.toolbox.intellij.language.csv;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/**
 * Syntax highlighter for CSV files.
 */
public class CsvSyntaxHighlighter extends SyntaxHighlighterBase {

    /** Text attributes key for commas. */
    public static final TextAttributesKey COMMA =
            createTextAttributesKey("CSV_COMMA", DefaultLanguageHighlighterColors.COMMA);
    
    /** Text attributes key for quotes. */
    public static final TextAttributesKey QUOTE =
            createTextAttributesKey("CSV_QUOTE", DefaultLanguageHighlighterColors.STRING);
    
    /** Text attributes key for text. */
    public static final TextAttributesKey TEXT =
            createTextAttributesKey("CSV_TEXT", DefaultLanguageHighlighterColors.IDENTIFIER);

    private static final TextAttributesKey[] COMMA_KEYS = new TextAttributesKey[]{COMMA};
    private static final TextAttributesKey[] QUOTE_KEYS = new TextAttributesKey[]{QUOTE};
    private static final TextAttributesKey[] TEXT_KEYS = new TextAttributesKey[]{TEXT};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    /**
     * Gets the lexer used for highlighting.
     *
     * @return the lexer
     */
    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new CsvLexer();
    }

    /**
     * Returns the text attributes keys for the given token type.
     *
     * @param tokenType the token type
     * @return an array of text attributes keys
     */
    @NotNull
    @Override
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(CsvLexer.COMMA)) {
            return COMMA_KEYS;
        } else if (tokenType.equals(CsvLexer.QUOTE)) {
            return QUOTE_KEYS;
        } else if (tokenType.equals(CsvLexer.TEXT)) {
            return TEXT_KEYS;
        }
        return EMPTY_KEYS;
    }
}
