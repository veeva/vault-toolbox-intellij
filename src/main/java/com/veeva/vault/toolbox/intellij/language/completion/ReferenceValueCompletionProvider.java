package com.veeva.vault.toolbox.intellij.language.completion;

import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.veeva.vault.toolbox.intellij.language.MdlPsiContext;
import com.veeva.vault.toolbox.intellij.metadata.MetadataIndex;
import com.veeva.vault.toolbox.intellij.metadata.MetadataService;
import com.veeva.vault.toolbox.intellij.metadata.mdl.MdlReferenceKindRegistry;
import com.veeva.vault.toolbox.intellij.metadata.mdl.MdlReferenceKindRegistry.RefKind;
import com.veeva.vault.toolbox.intellij.metadata.model.FieldMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Suggests Vault component names inside reference-bearing string slots — e.g. object names
 * inside {@code object('…')} or picklist names inside {@code picklist('…')}. The slot's
 * reference kind is resolved via {@link MdlReferenceKindRegistry}; unmapped slots produce no
 * suggestions, so this provider never adds noise to ordinary string literals.
 */
class ReferenceValueCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(
            @NotNull CompletionParameters parameters,
            @NotNull ProcessingContext processingContext,
            @NotNull CompletionResultSet resultSet) {
        PsiElement position = parameters.getPosition();
        String attributeName = MdlPsiContext.attributeNameOf(position);
        if (attributeName == null) {
            return;
        }
        RefKind kind = MdlReferenceKindRegistry.kindFor(MdlPsiContext.componentTypeOf(position), attributeName);
        if (kind == RefKind.NONE) {
            return;
        }

        Project project = position.getProject();
        MetadataService service = MetadataService.getInstance(project);
        if (service == null) {
            return;
        }
        MetadataIndex index = service.getIndex();
        if (!index.isReady()) {
            return;
        }

        Collection<String> names = namesFor(kind, index, service, position);
        if (names.isEmpty()) {
            return;
        }

        // The position's text contains the synthetic completion identifier; strip it so the
        // prefix matches what the user actually typed inside the quotes.
        String prefix = position.getText().replace(CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED, "");
        CompletionResultSet result = resultSet.withPrefixMatcher(prefix);
        for (String name : names) {
            result.addElement(LookupElementBuilderFactory.create(name));
        }
    }

    private Collection<String> namesFor(RefKind kind, MetadataIndex index, MetadataService service, PsiElement position) {
        switch (kind) {
            case OBJECT:
                return index.objectNames();
            case PICKLIST:
                // MDL references picklists in qualified form, e.g. picklist('Picklist.color__c').
                return qualified("Picklist.", index.picklistNames());
            case COMPONENT_TYPE:
                return index.componentTypeNames();
            case DOC_TYPE:
                return index.docTypeNames();
            case FIELD:
                return fieldNames(index, service, position);
            default:
                return Collections.emptyList();
        }
    }

    private Collection<String> qualified(String prefix, Collection<String> names) {
        List<String> result = new ArrayList<>(names.size());
        for (String name : names) {
            result.add(prefix + name);
        }
        return result;
    }

    /** Field names of the enclosing {@code Object} declaration; triggers a lazy load if needed. */
    private Collection<String> fieldNames(MetadataIndex index, MetadataService service, PsiElement position) {
        String objectName = MdlPsiContext.recordNameForComponentType(position, "Object");
        if (objectName == null) {
            return Collections.emptyList();
        }
        if (!index.fieldsLoaded(objectName)) {
            service.ensureObjectFieldsLoaded(objectName);
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (FieldMeta field : index.fieldsFor(objectName)) {
            if (field.getName() != null) {
                names.add(field.getName());
            }
        }
        return names;
    }
}
