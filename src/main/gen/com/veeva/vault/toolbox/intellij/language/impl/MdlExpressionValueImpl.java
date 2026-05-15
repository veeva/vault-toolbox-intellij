// This is a generated file. Not intended for manual editing.
package com.veeva.vault.toolbox.intellij.language.impl;

import java.util.List;

import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;

import static com.veeva.vault.toolbox.intellij.language.psi.MdlTypes.*;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.veeva.vault.toolbox.intellij.language.psi.*;

public class MdlExpressionValueImpl extends ASTWrapperPsiElement implements MdlExpressionValue {

    public MdlExpressionValueImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull MdlVisitor visitor) {
        visitor.visitExpressionValue(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof MdlVisitor) accept((MdlVisitor) visitor);
        else super.accept(visitor);
    }

}
