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

public class MdlXmlAttributeValueImpl extends ASTWrapperPsiElement implements MdlXmlAttributeValue {

    public MdlXmlAttributeValueImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull MdlVisitor visitor) {
        visitor.visitXmlAttributeValue(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof MdlVisitor) accept((MdlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public List<MdlXmlAttributeValue> getXmlAttributeValueList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, MdlXmlAttributeValue.class);
    }

    @Override
    @Nullable
    public MdlXmlClosedTag getXmlClosedTag() {
        return findChildByClass(MdlXmlClosedTag.class);
    }

    @Override
    @Nullable
    public MdlXmlInfoTag getXmlInfoTag() {
        return findChildByClass(MdlXmlInfoTag.class);
    }

    @Override
    @Nullable
    public MdlXmlOpenTag getXmlOpenTag() {
        return findChildByClass(MdlXmlOpenTag.class);
    }

    @Override
    @Nullable
    public MdlXmlSelfClosingTag getXmlSelfClosingTag() {
        return findChildByClass(MdlXmlSelfClosingTag.class);
    }

    @Override
    @NotNull
    public List<MdlXmlText> getXmlTextList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, MdlXmlText.class);
    }

}
