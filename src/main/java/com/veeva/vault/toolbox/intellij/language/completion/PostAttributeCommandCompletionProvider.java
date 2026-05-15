package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Suggests positioning keywords (such as {@code FIRST} or {@code AFTER}) that may
 * follow an attribute command to control where an attribute is inserted.
 */
public class PostAttributeCommandCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final List<String> POSITIONS = List.of("FIRST", "LAST", "AFTER");

    @Override
    protected void addCompletions(
            @NotNull CompletionParameters completionParameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        for (String position : POSITIONS) {
            resultSet.addElement(LookupElementBuilder.create(position).withCaseSensitivity(false));
        }
    }
}
