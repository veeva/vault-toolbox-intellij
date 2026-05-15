package com.veeva.vault.toolbox.intellij.language;

import com.intellij.psi.tree.TokenSet;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;

/**
 * Named groupings of MDL token types used by the parser definition, formatter, and
 * folding builder.
 */
public class MdlTokenSets {
    /** Token types that the IDE should treat as comments. */
    public static final TokenSet COMMENTS = TokenSet.create(MdlTypes.COMMENT);

    /** Token types that introduce a new indentation level when formatting. */
    public static final TokenSet INDENT_TOKEN_TYPES = TokenSet.create(
            MdlTypes.COMMAND_BLOCK,
            MdlTypes.COMMAND_ITEM,
            MdlTypes.XML_BLOCK,
            MdlTypes.XML_ATTRIBUTE_VALUE);

    /** Token types that represent embedded XML structure. */
    public static final TokenSet XML_TYPES = TokenSet.create(
            MdlTypes.XML_OPEN_TAG,
            MdlTypes.XML_ATTRIBUTE_VALUE,
            MdlTypes.XML_CLOSED_TAG,
            MdlTypes.XML_SELF_CLOSING_TAG);

    /** Token types whose elements are eligible for code folding. */
    public static final TokenSet FOLDING_TOKEN_TYPES = TokenSet.create(
            MdlTypes.COMMAND_LIST,
            MdlTypes.SUBCOMPONENT_COMMAND_LIST,
            MdlTypes.NEW_ATTRIBUTE,
            MdlTypes.ATTRIBUTE_PAIR);
}
