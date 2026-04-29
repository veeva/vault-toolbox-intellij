// Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.veeva.vault.toolbox.intellij.templates;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import org.jetbrains.annotations.NotNull;

final class VapilContext extends TemplateContextType {

  VapilContext() {
    super("Java");
  }

	@Override
	public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {

		try {
			String content = new String(templateActionContext.getFile().getOriginalFile().getVirtualFile().contentsToByteArray());
			return templateActionContext.getFile().getName().endsWith(".java");
		}
		catch (Exception e) {
			return false;
		}
	}

}
