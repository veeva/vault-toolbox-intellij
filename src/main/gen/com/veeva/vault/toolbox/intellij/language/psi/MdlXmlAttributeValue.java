// This is a generated file. Not intended for manual editing.
package com.veeva.vault.toolbox.intellij.language.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface MdlXmlAttributeValue extends PsiElement {

  @NotNull
  List<MdlXmlAttributeValue> getXmlAttributeValueList();

  @Nullable
  MdlXmlClosedTag getXmlClosedTag();

  @Nullable
  MdlXmlInfoTag getXmlInfoTag();

  @Nullable
  MdlXmlOpenTag getXmlOpenTag();

  @Nullable
  MdlXmlSelfClosingTag getXmlSelfClosingTag();

  @NotNull
  List<MdlXmlText> getXmlTextList();

}
