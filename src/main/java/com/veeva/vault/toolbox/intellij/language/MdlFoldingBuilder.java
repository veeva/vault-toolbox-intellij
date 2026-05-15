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
 * Builds code folding regions for MDL files. Any element whose type is in
 * {@link MdlTokenSets#FOLDING_TOKEN_TYPES} contributes a foldable region.
 */
public class MdlFoldingBuilder extends FoldingBuilderEx implements DumbAware {
    private static final int MAX_PLACEHOLDER_LENGTH = 40;

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        root.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (MdlTokenSets.FOLDING_TOKEN_TYPES.contains(element.getNode().getElementType())) {
                    descriptors.add(new FoldingDescriptor(element, element.getTextRange()));
                }
                super.visitElement(element);
            }
        });
        return descriptors.toArray(FoldingDescriptor.EMPTY_ARRAY);
    }

    /**
     * Builds the placeholder text shown when a region is folded. The first line of
     * the node's text is used, truncated to {@value #MAX_PLACEHOLDER_LENGTH}
     * characters; any text from the first opening parenthesis onward is dropped so
     * the placeholder shows just the leading identifier.
     */
    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode astNode) {
        String text = astNode.getText().split("\\R")[0];
        int parenIndex = text.indexOf('(');
        if (parenIndex != -1) {
            text = text.substring(0, parenIndex);
        }
        if (text.length() > MAX_PLACEHOLDER_LENGTH) {
            text = text.substring(0, MAX_PLACEHOLDER_LENGTH) + "...";
        }
        return text;
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode astNode) {
        return false;
    }
}
