package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

class CommandCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(
            @NotNull CompletionParameters completionParameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        resultSet.addElement(LookupElementBuilderFactory.create("CREATE").withCaseSensitivity(false));
        resultSet.addElement(LookupElementBuilderFactory.create("RECREATE").withCaseSensitivity(false));
        resultSet.addElement(LookupElementBuilderFactory.create("ALTER").withCaseSensitivity(false));
        resultSet.addElement(LookupElementBuilderFactory.create("RENAME").withCaseSensitivity(false));
        resultSet.addElement(LookupElementBuilderFactory.create("DROP").withCaseSensitivity(false));
    }
}
