package com.veeva.vault.toolbox.intellij.language;

import com.intellij.psi.tree.TokenSet;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;

/**
 * Container for sets of Tokens
 */
public class MdlTokenSets {
    /**
     * Identifies comment-based tokens
     */
    public static TokenSet COMMENTS = TokenSet.create(MdlTypes.COMMENT);

    /**
     * Identifies tokens that may be used for new indentations. These typically include blocks and block items
     */
    public static TokenSet INDENT_TOKEN_TYPES = TokenSet.create(MdlTypes.COMMAND_BLOCK, MdlTypes.COMMAND_ITEM, MdlTypes.XML_BLOCK, MdlTypes.XML_ATTRIBUTE_VALUE);

    /**
     * Identifies tokens that would be considered as used in XML
     */
    public static TokenSet XML_TYPES = TokenSet.create(MdlTypes.XML_OPEN_TAG, MdlTypes.XML_ATTRIBUTE_VALUE, MdlTypes.XML_CLOSED_TAG, MdlTypes.XML_SELF_CLOSING_TAG);

    /**
     * Identifies tokens that may be folded. These typically includes blocks and block items but can also include multi line attributes.
     */
    public static TokenSet FOLDING_TOKEN_TYPES = TokenSet.create(MdlTypes.COMMAND_LIST, MdlTypes.SUBCOMPONENT_COMMAND_LIST, MdlTypes.NEW_ATTRIBUTE, MdlTypes.ATTRIBUTE_PAIR);
}
