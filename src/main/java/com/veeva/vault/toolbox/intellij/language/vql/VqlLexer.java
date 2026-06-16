package com.veeva.vault.toolbox.intellij.language.vql;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Hand-written lexer for VQL used purely for syntax highlighting and completion in the
 * VQL Console. It recognises keywords, identifiers, string and number literals, and
 * punctuation; it does not build a meaningful PSI tree (see {@link VqlParserDefinition}).
 */
public class VqlLexer extends LexerBase {

    public static final IElementType KEYWORD = new IElementType("VQL_KEYWORD", VqlLanguage.INSTANCE);
    public static final IElementType IDENTIFIER = new IElementType("VQL_IDENTIFIER", VqlLanguage.INSTANCE);
    public static final IElementType STRING = new IElementType("VQL_STRING", VqlLanguage.INSTANCE);
    public static final IElementType NUMBER = new IElementType("VQL_NUMBER", VqlLanguage.INSTANCE);
    public static final IElementType OPERATOR = new IElementType("VQL_OPERATOR", VqlLanguage.INSTANCE);
    public static final IElementType COMMA = new IElementType("VQL_COMMA", VqlLanguage.INSTANCE);
    public static final IElementType PARENS = new IElementType("VQL_PARENS", VqlLanguage.INSTANCE);
    public static final IElementType DOT = new IElementType("VQL_DOT", VqlLanguage.INSTANCE);

    /** Recognised VQL keywords (compared case-insensitively). */
    static final Set<String> KEYWORDS = Set.of(
            "select", "from", "where", "and", "or", "not", "in", "like", "between",
            "order", "by", "asc", "desc", "nulls", "first", "last",
            "limit", "offset", "skip", "pagesize", "maxrows",
            "group", "having", "as", "contains", "find", "scope", "typeof",
            "when", "then", "else", "end", "null", "true", "false");

    private CharSequence buffer;
    private int endOffset;
    private int position;
    private int tokenStart;
    private int tokenEnd;
    private IElementType tokenType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.endOffset = endOffset;
        this.position = startOffset;
        advance();
    }

    @Override
    public int getState() {
        return 0;
    }

    @Nullable
    @Override
    public IElementType getTokenType() {
        return tokenType;
    }

    @Override
    public int getTokenStart() {
        return tokenStart;
    }

    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }

    @Override
    public void advance() {
        if (position >= endOffset) {
            tokenType = null;
            return;
        }

        tokenStart = position;
        char c = buffer.charAt(position);

        if (Character.isWhitespace(c)) {
            while (position < endOffset && Character.isWhitespace(buffer.charAt(position))) {
                position++;
            }
            tokenType = TokenType.WHITE_SPACE;
            tokenEnd = position;
            return;
        }

        if (c == '\'') {
            position++;
            while (position < endOffset) {
                char ch = buffer.charAt(position);
                position++;
                if (ch == '\\' && position < endOffset) {
                    position++; // skip escaped character
                } else if (ch == '\'') {
                    break;
                }
            }
            tokenType = STRING;
            tokenEnd = position;
            return;
        }

        if (Character.isDigit(c)) {
            position++;
            while (position < endOffset
                    && (Character.isDigit(buffer.charAt(position)) || buffer.charAt(position) == '.')) {
                position++;
            }
            tokenType = NUMBER;
            tokenEnd = position;
            return;
        }

        if (isWordStart(c)) {
            position++;
            while (position < endOffset && isWordPart(buffer.charAt(position))) {
                position++;
            }
            String word = buffer.subSequence(tokenStart, position).toString();
            tokenType = KEYWORDS.contains(word.toLowerCase()) ? KEYWORD : IDENTIFIER;
            tokenEnd = position;
            return;
        }

        position++;
        switch (c) {
            case ',':
                tokenType = COMMA;
                break;
            case '(':
            case ')':
                tokenType = PARENS;
                break;
            case '.':
                tokenType = DOT;
                break;
            case '=':
            case '<':
            case '>':
            case '!':
            case '+':
            case '-':
            case '*':
                tokenType = OPERATOR;
                break;
            default:
                tokenType = TokenType.BAD_CHARACTER;
                break;
        }
        tokenEnd = position;
    }

    @NotNull
    @Override
    public CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }

    private static boolean isWordStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isWordPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
