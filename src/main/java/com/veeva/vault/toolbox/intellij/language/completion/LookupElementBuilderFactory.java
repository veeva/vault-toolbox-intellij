package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import icons.ToolboxIcons;

/**
 * Factory for constructing {@link LookupElementBuilder} instances with the standard
 * MDL icon attached, so all completion suggestions render consistently.
 */
class LookupElementBuilderFactory {

    private LookupElementBuilderFactory() {
    }

    /**
     * Creates a lookup element builder for the given completion text, decorated with
     * the MDL icon.
     *
     * @param name the literal text inserted by the completion
     * @return a lookup element builder ready for further customization
     */
    public static LookupElementBuilder create(String name) {
        return LookupElementBuilder.create(name).withIcon(ToolboxIcons.Mdl);
    }
}
