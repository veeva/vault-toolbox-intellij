package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Suggests attribute-level commands that mutate attribute lists.
 */
public class AttributeCommandCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final List<String> ATTRIBUTE_COMMANDS = List.of("ADD", "DROP");

    @Override
    protected void addCompletions(
            @NotNull CompletionParameters completionParameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        for (String command : ATTRIBUTE_COMMANDS) {
            resultSet.addElement(LookupElementBuilder.create(command).withCaseSensitivity(false));
        }
    }
}
