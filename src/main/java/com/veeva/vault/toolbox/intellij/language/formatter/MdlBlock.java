package com.veeva.vault.toolbox.intellij.language.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.TokenSet;
import com.veeva.vault.toolbox.intellij.language.MdlTokenSets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Formatting block representing a single MDL AST node. Children that introduce a
 * new indentation level (per {@link MdlTokenSets#INDENT_TOKEN_TYPES}) get a
 * normal indent; everything else is laid out flush.
 */
class MdlBlock extends AbstractBlock {
    private final SpacingBuilder spacingBuilder;
    private final Indent indent;

    protected MdlBlock(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment,
                       SpacingBuilder spacingBuilder, Indent indent) {
        super(node, wrap, alignment);
        this.spacingBuilder = spacingBuilder;
        this.indent = indent;
    }

    @Override
    protected List<Block> buildChildren() {
        List<Block> blocks = new ArrayList<>();
        for (ASTNode child : getNode().getChildren(null)) {
            if (TokenSet.WHITE_SPACE.contains(child.getElementType())) {
                continue;
            }
            blocks.add(new MdlBlock(child, getWrap(), getAlignment(), spacingBuilder, buildChildIndent(child)));
        }
        return blocks;
    }

    private Indent buildChildIndent(ASTNode child) {
        return MdlTokenSets.INDENT_TOKEN_TYPES.contains(child.getElementType())
                ? Indent.getNormalIndent()
                : Indent.getNoneIndent();
    }

    @Override
    public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
        Indent childIndent = MdlTokenSets.INDENT_TOKEN_TYPES.contains(getNode().getElementType())
                ? Indent.getNormalIndent()
                : Indent.getNoneIndent();
        return new ChildAttributes(childIndent, null);
    }

    @Override
    public boolean isLeaf() {
        return getNode().getFirstChildNode() == null;
    }

    @Override
    public Indent getIndent() {
        return indent;
    }

    @Nullable
    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return spacingBuilder.getSpacing(this, child1, child2);
    }
}
