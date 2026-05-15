package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Suggests Vault component types that may follow a top-level command (such as
 * {@code Permissionset}, {@code Workflow}, or {@code Object}).
 */
class ComponentTypeCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final List<String> COMPONENT_TYPES = List.of("Permissionset", "Workflow", "Object");

    @Override
    protected void addCompletions(
            @NotNull CompletionParameters completionParameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        for (String componentType : COMPONENT_TYPES) {
            resultSet.addElement(LookupElementBuilderFactory.create(componentType));
        }
    }
}
