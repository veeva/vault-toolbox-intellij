package com.veeva.vault.toolbox.intellij.language;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Helpers for relating MDL PSI elements to their schema context: the enclosing component type,
 * the owning attribute name, and the unquoted text of a string value. Operates on the AST/leaf
 * token level because the generated PSI exposes no accessors for the relevant leaf tokens, and
 * tolerates partial/incomplete trees produced while the user is still typing.
 */
public final class MdlPsiContext {

    /** Command element types that carry a {@code COMPONENT_TYPE} token. */
    private static final Set<IElementType> COMMAND_TYPES = Set.of(
            MdlTypes.RECORD_CHANGE_COMMAND,
            MdlTypes.COMPONENT_CHANGE_COMMAND,
            MdlTypes.SUBCOMPONENT_CHANGE_COMMAND,
            MdlTypes.RECORD_DROP_COMMAND,
            MdlTypes.COMPONENT_DROP_COMMAND,
            MdlTypes.SUBCOMPONENT_DROP_COMMAND,
            MdlTypes.RECORD_RENAME_COMMAND,
            MdlTypes.COMPONENT_RENAME_COMMAND,
            MdlTypes.SUBCOMPONENT_RENAME_COMMAND);

    private static final TokenSet QUOTE_TOKENS = TokenSet.create(MdlTypes.START_QUOTE, MdlTypes.END_QUOTE);

    private MdlPsiContext() {
    }

    /**
     * @return the name of the nearest enclosing component type (e.g. {@code Field} for an
     * attribute inside a {@code Field} subcomponent), or {@code null} if none is found.
     */
    @Nullable
    public static String componentTypeOf(PsiElement element) {
        ASTNode node = element != null ? element.getNode() : null;
        while (node != null) {
            if (COMMAND_TYPES.contains(node.getElementType())) {
                ASTNode typeNode = node.findChildByType(MdlTypes.COMPONENT_TYPE);
                if (typeNode != null) {
                    return typeNode.getText();
                }
            }
            node = node.getTreeParent();
        }
        return null;
    }

    /**
     * Walks up the enclosing command chain and returns the {@code RECORD_NAME} of the nearest
     * command whose component type equals {@code wantedComponentType} (case-insensitive). For
     * example, from anywhere inside {@code Recreate Object product__v(...)} this returns
     * {@code product__v} when asked for {@code Object}.
     *
     * @return the matching record name, or {@code null} if none is found
     */
    @Nullable
    public static String recordNameForComponentType(PsiElement element, String wantedComponentType) {
        ASTNode node = element != null ? element.getNode() : null;
        while (node != null) {
            if (COMMAND_TYPES.contains(node.getElementType())) {
                ASTNode typeNode = node.findChildByType(MdlTypes.COMPONENT_TYPE);
                if (typeNode != null && typeNode.getText().equalsIgnoreCase(wantedComponentType)) {
                    ASTNode recordNode = node.findChildByType(MdlTypes.RECORD_NAME);
                    return recordNode != null ? recordNode.getText() : null;
                }
            }
            node = node.getTreeParent();
        }
        return null;
    }

    /**
     * @return the attribute name owning the given element (e.g. {@code object} for a string
     * inside {@code object('product__v')}), or {@code null} if the element is not within an
     * attribute pair.
     */
    @Nullable
    public static String attributeNameOf(PsiElement element) {
        ASTNode node = element != null ? element.getNode() : null;
        while (node != null) {
            if (node.getElementType() == MdlTypes.ATTRIBUTE_PAIR) {
                ASTNode attributeNode = node.findChildByType(MdlTypes.ATTRIBUTE);
                return attributeNode != null ? attributeNode.getText() : null;
            }
            node = node.getTreeParent();
        }
        return null;
    }

    /**
     * Extracts the inner text and text range of an MDL {@code STRING_VALUE} element, excluding
     * the surrounding quotes.
     *
     * @param stringValue a {@code STRING_VALUE} PSI element
     * @return the unquoted value, or {@code null} if the element is not a string value
     */
    @Nullable
    public static UnquotedValue unquote(PsiElement stringValue) {
        if (stringValue == null || stringValue.getNode() == null
                || stringValue.getNode().getElementType() != MdlTypes.STRING_VALUE) {
            return null;
        }
        ASTNode node = stringValue.getNode();
        ASTNode firstValue = null;
        ASTNode lastValue = null;
        for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (child.getElementType() == MdlTypes.VALUE) {
                if (firstValue == null) {
                    firstValue = child;
                }
                lastValue = child;
            }
        }
        if (firstValue != null) {
            int start = firstValue.getStartOffset();
            int end = lastValue.getStartOffset() + lastValue.getTextLength();
            return new UnquotedValue(node.getText().substring(start - node.getStartOffset(), end - node.getStartOffset()),
                    new TextRange(start, end));
        }
        // Empty string literal (just quotes): nothing to validate or suggest against.
        return null;
    }

    /**
     * Strips a leading {@code ComponentType.} qualifier from an MDL reference value, returning the
     * bare API name used by the metadata index. MDL often writes references in qualified form
     * (e.g. {@code Picklist.default_status__v}) while the schema stores {@code default_status__v}.
     * Vault API names contain no dots, so the first dot reliably separates the type from the name.
     */
    public static String bareName(String referenceValue) {
        if (referenceValue == null) {
            return null;
        }
        int dot = referenceValue.indexOf('.');
        return dot > 0 && dot < referenceValue.length() - 1 ? referenceValue.substring(dot + 1) : referenceValue;
    }

    /** @return whether the given AST node is a quote delimiter token. */
    public static boolean isQuote(ASTNode node) {
        return node != null && QUOTE_TOKENS.contains(node.getElementType());
    }

    /** The inner text of a string literal together with its absolute text range. */
    public static final class UnquotedValue {
        private final String text;
        private final TextRange range;

        public UnquotedValue(String text, TextRange range) {
            this.text = text;
            this.range = range;
        }

        public String getText() {
            return text;
        }

        public TextRange getRange() {
            return range;
        }
    }
}
