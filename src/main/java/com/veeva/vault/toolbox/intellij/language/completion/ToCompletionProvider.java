package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Suggests the {@code TO} keyword that follows a {@code RENAME} command.
 */
class ToCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(
            @NotNull CompletionParameters completionParameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        resultSet.addElement(LookupElementBuilderFactory.create("TO").withCaseSensitivity(false));
    }
}
