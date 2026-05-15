package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Suggests the MDL sub-commands that may appear inside an enclosing command block.
 */
class SubCommandCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final List<String> SUB_COMMANDS = List.of("ADD", "MODIFY", "DROP", "RENAME");

    @Override
    protected void addCompletions(
            @NotNull CompletionParameters completionParameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        for (String subCommand : SUB_COMMANDS) {
            resultSet.addElement(LookupElementBuilderFactory.create(subCommand).withCaseSensitivity(false));
        }
    }
}
