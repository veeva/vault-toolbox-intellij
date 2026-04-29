package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

class ComponentTypeCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(
            @NotNull CompletionParameters completionParameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        getComponentTypeItems().stream()
                .forEach(
                        (componentType) -> {
                            resultSet.addElement(LookupElementBuilderFactory.create(componentType));
                        }
                );
    }


    private List<String> getComponentTypeItems() {
        //TODO Populate with cached/stored call to VAPIL.
        return Arrays.asList("Permissionset", "Workflow", "Object");
    }
}
