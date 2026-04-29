package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.util.ProcessingContext;
import com.veeva.vault.toolbox.intellij.language.MdlFile;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class AttributeCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(
            @NotNull CompletionParameters parameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        getAttributeItems(parameters).stream()
                .forEach(
                        (componentType) -> {
                            LookupElementBuilder lookupElementBuilder = LookupElementBuilderFactory
                                    .create(componentType)
                                    .withPresentableText(componentType);
                            resultSet.addElement(new ParenthesisTailTypeDecorator(lookupElementBuilder));
                        }
                );
    }

    private List<String> getAttributeItems(CompletionParameters parameters) {
        String possibleLastCommandTarget = findPossibleLastCommandTarget(parameters);
        if (possibleLastCommandTarget != null) {
            //TODO Populate with cached/stored call to VAPIL.
            //TODO Filter based on values already provided. Don't recommend again
            if("Object".equals(possibleLastCommandTarget)) {
                return Arrays.asList("label", "description");
            }
            if("Field".equals(possibleLastCommandTarget)) {
                return List.of("field_specific_thing");
            }
            if("Permissionset".equals(possibleLastCommandTarget)) {
                return List.of("permissionset_specific_thing");
            }
        }
        return Collections.emptyList();
    }

    private String findPossibleLastCommandTarget(CompletionParameters parameters) {
        PsiElement position = parameters.getPosition();
        parameters.getOriginalFile();
        if(position.getParent() instanceof MdlFile) {
            //Basic structure is we are still building out things. So rather then things being nicely organized,
            // we have all of the elements of the new component type represented as children of the file.
            // As such, need to just walk up positionally to figure out which component type we are updating.
            while(position != null) {
                position = position.getPrevSibling();
                if(position instanceof TreeElement treeElement && MdlTypes.COMPONENT_TYPE.equals(treeElement.getElementType())) {
                    //TODO "Look back" for another nearby COMPONENT_TYPE, handle case where there is a Componenttype update
                    return position.getText();
                }
            }
        }
        return null;
    }


}
