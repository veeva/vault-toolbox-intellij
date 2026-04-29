package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class PostAttributeCommandCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(
            @NotNull CompletionParameters completionParameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        resultSet.addElement(LookupElementBuilder.create("FIRST").withCaseSensitivity(false));
        resultSet.addElement(LookupElementBuilder.create("LAST").withCaseSensitivity(false));
        resultSet.addElement(LookupElementBuilder.create("AFTER").withCaseSensitivity(false));
    }
}
