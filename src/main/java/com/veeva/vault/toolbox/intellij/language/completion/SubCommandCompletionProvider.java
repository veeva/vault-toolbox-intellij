package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

class SubCommandCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(
            @NotNull CompletionParameters completionParameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        resultSet.addElement(LookupElementBuilderFactory.create("ADD").withCaseSensitivity(false));
        resultSet.addElement(LookupElementBuilderFactory.create("MODIFY").withCaseSensitivity(false));
        resultSet.addElement(LookupElementBuilderFactory.create("DROP").withCaseSensitivity(false));
        resultSet.addElement(LookupElementBuilderFactory.create("RENAME").withCaseSensitivity(false));
    }
}
