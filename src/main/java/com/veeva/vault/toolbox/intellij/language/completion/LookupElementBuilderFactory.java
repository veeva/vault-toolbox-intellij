package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import icons.ToolboxIcons;

class LookupElementBuilderFactory {

    public static LookupElementBuilder create(String name) {
        return LookupElementBuilder.create(name)
                .withIcon(ToolboxIcons.Mdl);
    }
}
