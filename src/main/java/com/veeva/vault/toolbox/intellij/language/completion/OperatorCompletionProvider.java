package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Suggests the existence-check operators that may follow an MDL command.
 */
class OperatorCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final List<String> OPERATORS = List.of("IF NOT EXISTS", "IF EXISTS");

    @Override
    protected void addCompletions(
            @NotNull CompletionParameters completionParameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        for (String operator : OPERATORS) {
            resultSet.addElement(LookupElementBuilderFactory.create(operator).withCaseSensitivity(false));
        }
    }
}
