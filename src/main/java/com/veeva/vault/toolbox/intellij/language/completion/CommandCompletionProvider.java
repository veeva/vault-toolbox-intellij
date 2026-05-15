package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Suggests the top-level MDL commands that start a statement.
 */
class CommandCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final List<String> COMMANDS = List.of("CREATE", "RECREATE", "ALTER", "RENAME", "DROP");

    @Override
    protected void addCompletions(
            @NotNull CompletionParameters completionParameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        for (String command : COMMANDS) {
            resultSet.addElement(LookupElementBuilderFactory.create(command).withCaseSensitivity(false));
        }
    }
}
