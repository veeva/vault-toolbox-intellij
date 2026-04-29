package com.veeva.vault.toolbox.intellij.language;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles folding MDL commands. Folding is allowed on those tokens that are present in {@link MdlTokenSets#FOLDING_TOKEN_TYPES}
 */
public class MdlFoldingBuilder extends FoldingBuilderEx implements DumbAware {
    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean b) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();

        root.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if(MdlTokenSets.FOLDING_TOKEN_TYPES.contains(element.getNode().getElementType())) {
                    descriptors.add(new FoldingDescriptor(element, element.getTextRange()));
                }
                super.visitElement(element);
            }
        });

        return descriptors.toArray(FoldingDescriptor.EMPTY_ARRAY);
    }

    private static final int MAX_LENGTH = 40;

    /**
     * Calculates the text that should be visible when a node is folded. In this case, we show a max of 40 characters.
     * For nodes with open parenthesis we show values up to, not including the parenthesis
     */
    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode astNode) {
        String text = astNode.getText();
        text = text.split("\\R")[0];
        int i = text.indexOf('(');
        if(i != -1) {
            text = text.substring(0, i);
        }
        if(text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH) + "...";
        }

        return text;
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode astNode) {
        return false;
    }
}
