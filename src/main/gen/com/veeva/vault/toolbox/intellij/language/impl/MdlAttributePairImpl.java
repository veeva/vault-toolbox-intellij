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

public class MdlAttributePairImpl extends ASTWrapperPsiElement implements MdlAttributePair {

  public MdlAttributePairImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MdlVisitor visitor) {
    visitor.visitAttributePair(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MdlVisitor) accept((MdlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<MdlAttributeContent> getAttributeContentList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MdlAttributeContent.class);
  }

}
