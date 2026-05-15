// Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.veeva.vault.toolbox.intellij.templates;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import org.jetbrains.annotations.NotNull;

/**
 * Live template context for VAPIL (Vault API Library) snippets, active in Java source files.
 */
final class VapilContext extends TemplateContextType {

	VapilContext() {
		super("Java");
	}

	/**
	 * Determines whether the given template action context targets a Java source file.
	 *
	 * @param templateActionContext the context in which a live template is being expanded.
	 * @return {@code true} if the underlying file has a {@code .java} extension; {@code false} otherwise.
	 */
	@Override
	public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {
		return templateActionContext.getFile().getName().endsWith(".java");
	}
}
