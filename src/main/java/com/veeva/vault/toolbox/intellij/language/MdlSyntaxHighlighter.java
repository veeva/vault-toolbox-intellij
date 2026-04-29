package com.veeva.vault.toolbox.intellij.language;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class MdlSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey COMMAND =
            createTextAttributesKey("MDL_COMMAND", DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey COMPONENT_TYPE =
            createTextAttributesKey("MDL_COMPONENT_TYPE", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey RECORD_NAME =
            createTextAttributesKey("MDL_RECORD_NAME", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey XML_IDENTIFIER =
            createTextAttributesKey("MDL_XML_IDENTIFIER", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey CHARACTER =
            createTextAttributesKey("MDL_CHARACTER", DefaultLanguageHighlighterColors.INSTANCE_METHOD);
    public static final TextAttributesKey ATTRIBUTE =
            createTextAttributesKey("MDL_ATTRIBUTE", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey XML_TEXT =
            createTextAttributesKey("MDL_XML_TEXT", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey VALUE =
            createTextAttributesKey("MDL_VALUE", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey COMMENT =
            createTextAttributesKey("MDL_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BAD_CHARACTER =
            createTextAttributesKey("MDL_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);


    private static final TextAttributesKey[] BAD_CHAR_KEYS = new TextAttributesKey[]{BAD_CHARACTER};
    private static final TextAttributesKey[] COMMAND_KEYS = new TextAttributesKey[]{COMMAND};
    private static final TextAttributesKey[] COMPONENT_TYPE_KEYS = new TextAttributesKey[]{COMPONENT_TYPE};
    private static final TextAttributesKey[] RECORD_NAME_KEYS = new TextAttributesKey[]{RECORD_NAME};
    private static final TextAttributesKey[] XML_IDENTIFIER_KEYS = new TextAttributesKey[]{XML_IDENTIFIER};
    private static final TextAttributesKey[] CHARACTER_KEYS = new TextAttributesKey[]{CHARACTER};
    private static final TextAttributesKey[] ATTRIBUTE_KEYS = new TextAttributesKey[]{ATTRIBUTE};
    private static final TextAttributesKey[] XML_TEXT_KEYS = new TextAttributesKey[]{XML_TEXT};
    private static final TextAttributesKey[] VALUE_KEYS = new TextAttributesKey[]{VALUE};
    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

    @NotNull
    @Override
    public Lexer getHighlightingLexer() {
        return new MdlLexerAdapter();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType.equals(MdlTypes.COMMAND)
                || tokenType.equals(MdlTypes.DROP)
                || tokenType.equals(MdlTypes.RENAME)
                || tokenType.equals(MdlTypes.SUBCOMMAND)
                || tokenType.equals(MdlTypes.ATTRIBUTE_COMMAND)
                || tokenType.equals(MdlTypes.POST_ATTRIBUTE_COMMAND)) {
            return COMMAND_KEYS;
        }
        if (tokenType.equals(MdlTypes.OPERATOR) || tokenType.equals(MdlTypes.TO)) {
            return COMMAND_KEYS;
        }
        if (tokenType.equals(MdlTypes.COMPONENT_TYPE) || tokenType.equals(MdlTypes.COMPONENT_TYPE_LITERAL)) {
            return COMPONENT_TYPE_KEYS;
        }
        if (tokenType.equals(MdlTypes.RECORD_NAME)) {
            return RECORD_NAME_KEYS;
        }
        if (tokenType.equals(MdlTypes.XML_IDENTIFIER)) {
            return XML_IDENTIFIER_KEYS;
        }
        if (tokenType.equals(MdlTypes.COMMA)
                || tokenType.equals(MdlTypes.SEMICOLON)
                || tokenType.equals(MdlTypes.EQUALS)
                || tokenType.equals(MdlTypes.XML_SLASH)
                || tokenType.equals(MdlTypes.START_BRACE)
                || tokenType.equals(MdlTypes.END_BRACE)
                || tokenType.equals(MdlTypes.OPEN_ANGLE_BRACKET)
                || tokenType.equals(MdlTypes.CLOSED_ANGLE_BRACKET)
                || tokenType.equals(MdlTypes.START_PAREN)
                || tokenType.equals(MdlTypes.END_PAREN)
                || tokenType.equals(MdlTypes.QUESTION)) {
            return CHARACTER_KEYS;
        }
        if (tokenType.equals(MdlTypes.ATTRIBUTE_LITERAL)) {
            return COMPONENT_TYPE_KEYS;
        }
        if (tokenType.equals(MdlTypes.XML_TAG_CONTENT)) {
            return XML_TEXT_KEYS;
        }
        if (tokenType.equals(MdlTypes.ATTRIBUTE) || tokenType.equals(MdlTypes.XML_ATTRIBUTE)) {
            return ATTRIBUTE_KEYS;
        }
        if (tokenType.equals(MdlTypes.VALUE)
                || tokenType.equals(MdlTypes.XML_VALUE)
                || tokenType.equals(MdlTypes.START_QUOTE)
                || tokenType.equals(MdlTypes.END_QUOTE)) {
            return VALUE_KEYS;
        }
        if (tokenType.equals(MdlTypes.COMMENT)) {
            return COMMENT_KEYS;
        }
        if (tokenType.equals(TokenType.BAD_CHARACTER)) {
            return BAD_CHAR_KEYS;
        }
        return EMPTY_KEYS;
    }
}
