package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.util.ProcessingContext;
import com.veeva.vault.toolbox.intellij.language.MdlFile;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;
import com.veeva.vault.toolbox.intellij.metadata.MetadataIndex;
import com.veeva.vault.toolbox.intellij.metadata.MetadataService;
import com.veeva.vault.toolbox.intellij.metadata.model.AttributeMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Suggests attribute names that are valid for the most recently typed component type.
 *
 * <p>Suggestions come from the live, cached Vault schema ({@link MetadataService}) when a
 * snapshot is available; the first request for a component type triggers a lazy, background
 * fetch of its attributes so subsequent invocations are populated. When the schema is not yet
 * available (e.g. not connected), a small static map is used as a fallback so completion never
 * regresses below the previous behavior.</p>
 */
class AttributeCompletionProvider extends CompletionProvider<CompletionParameters> {

    private static final Map<String, List<String>> ATTRIBUTES_BY_COMPONENT_TYPE = Map.of(
            "Object", List.of("label", "description"),
            "Field", List.of("field_specific_thing"),
            "Permissionset", List.of("permissionset_specific_thing")
    );

    /**
     * Adds attribute completions to the result set based on the current context.
     *
     * @param parameters       the completion parameters
     * @param processingContext the processing context
     * @param resultSet        the result set to add completions to
     */
    @Override
    protected void addCompletions(
            @NotNull CompletionParameters parameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        for (String attribute : getAttributeItems(parameters)) {
            LookupElementBuilder lookupElementBuilder = LookupElementBuilderFactory
                    .create(attribute)
                    .withPresentableText(attribute);
            resultSet.addElement(new ParenthesisTailTypeDecorator(lookupElementBuilder));
        }
    }

    /**
     * Retrieves a list of applicable attribute items for the component type at the caret position.
     *
     * @param parameters the completion parameters
     * @return a list of attribute names, or an empty list if none apply
     */
    private List<String> getAttributeItems(CompletionParameters parameters) {
        String lastCommandTarget = findPossibleLastCommandTarget(parameters);
        if (lastCommandTarget == null) {
            return Collections.emptyList();
        }

        Project project = parameters.getPosition().getProject();
        MetadataService service = MetadataService.getInstance(project);
        if (service != null) {
            MetadataIndex index = service.getIndex();
            if (index.isReady() && index.componentTypeExists(lastCommandTarget) == MetadataIndex.Existence.EXISTS) {
                service.ensureComponentTypeAttributesLoaded(lastCommandTarget);
                List<String> names = new ArrayList<>();
                for (AttributeMeta attribute : index.attributesFor(lastCommandTarget)) {
                    if (attribute.getName() != null) {
                        names.add(attribute.getName());
                    }
                }
                if (!names.isEmpty()) {
                    return names;
                }
            }
        }
        return ATTRIBUTES_BY_COMPONENT_TYPE.getOrDefault(lastCommandTarget, Collections.emptyList());
    }

    /**
     * Walks backwards from the current caret position through the previous siblings
     * looking for the most recent {@link MdlTypes#COMPONENT_TYPE} element. While the
     * file is being authored its elements are still flat children of the file rather
     * than fully nested, so a positional walk is the most reliable way to associate
     * an attribute position with its enclosing component type.
     *
     * @param parameters the current completion parameters
     * @return the text of the most recent component type, or {@code null} if none was
     * found before the caret
     */
    private String findPossibleLastCommandTarget(CompletionParameters parameters) {
        PsiElement position = parameters.getPosition();
        if (!(position.getParent() instanceof MdlFile)) {
            return null;
        }
        while (position != null) {
            position = position.getPrevSibling();
            if (position instanceof TreeElement treeElement
                    && MdlTypes.COMPONENT_TYPE.equals(treeElement.getElementType())) {
                return position.getText();
            }
        }
        return null;
    }
}
