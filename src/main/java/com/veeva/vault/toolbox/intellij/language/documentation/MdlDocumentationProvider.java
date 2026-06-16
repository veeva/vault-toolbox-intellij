package com.veeva.vault.toolbox.intellij.language.documentation;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.xml.util.XmlStringUtil;
import com.veeva.vault.toolbox.intellij.language.MdlReferenceResolver;
import com.veeva.vault.toolbox.intellij.metadata.MetadataIndex;
import com.veeva.vault.toolbox.intellij.metadata.MetadataService;
import com.veeva.vault.toolbox.intellij.metadata.mdl.MdlReferenceKindRegistry.RefKind;
import com.veeva.vault.toolbox.intellij.metadata.model.AttributeMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.ComponentTypeMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.FieldMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.ObjectMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.PicklistMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.PicklistValueMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.RelationshipMeta;
import org.jetbrains.annotations.Nullable;

/**
 * Shows Quick Documentation (Ctrl/Cmd-Q and hover) for MDL references using the cached vault
 * schema: objects list their fields, picklists list their values, component types list their
 * attributes, and an object/picklist declaration name shows its own detail. The first lookup of
 * a deep slice triggers a background fetch, so a second invocation is fully populated.
 */
public class MdlDocumentationProvider extends AbstractDocumentationProvider {

    private static final int MAX_ROWS = 100;

    @Nullable
    @Override
    public PsiElement getCustomDocumentationElement(Editor editor, PsiFile file,
                                                    @Nullable PsiElement contextElement, int targetOffset) {
        // MDL references are plain tokens (no PsiReference); document the token under the caret.
        return contextElement;
    }

    @Nullable
    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        MdlReferenceResolver.Ref ref = MdlReferenceResolver.resolve(originalElement != null ? originalElement : element);
        if (ref == null || ref.kind == RefKind.NONE) {
            return null;
        }
        return ref.kind + " " + ref.name;
    }

    @Nullable
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        PsiElement leaf = originalElement != null ? originalElement : element;
        MdlReferenceResolver.Ref ref = MdlReferenceResolver.resolve(leaf);
        if (ref == null) {
            return null;
        }
        MetadataService service = MetadataService.getInstance(leaf.getProject());
        if (service == null) {
            return null;
        }
        MetadataIndex index = service.getIndex();
        if (!index.isReady()) {
            return null;
        }
        switch (ref.kind) {
            case OBJECT:
                return objectDoc(service, index, ref.name);
            case PICKLIST:
                return picklistDoc(service, index, ref.name);
            case COMPONENT_TYPE:
                return componentTypeDoc(service, index, ref.name);
            case FIELD:
                return fieldDoc(service, index, ref.objectContext, ref.name);
            default:
                return null;
        }
    }

    // ---------------------------------------------------------------------------------------
    // HTML builders
    // ---------------------------------------------------------------------------------------

    private String objectDoc(MetadataService service, MetadataIndex index, String name) {
        ObjectMeta object = index.object(name);
        if (object == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(heading("Object", name, object.getLabel()));
        if (index.fieldsLoaded(name)) {
            sb.append(row("Fields", String.valueOf(object.getFieldsByName().size())));
            sb.append("<hr/>");
            int count = 0;
            for (FieldMeta field : index.fieldsFor(name)) {
                if (count++ >= MAX_ROWS) {
                    sb.append("…<br/>");
                    break;
                }
                String detail = field.getType() != null ? field.getType() : "";
                if (field.getReferencedObject() != null) {
                    detail += " → " + field.getReferencedObject();
                } else if (field.getPicklist() != null) {
                    detail += " → " + field.getPicklist();
                }
                sb.append(escape(field.getName())).append("&nbsp;&nbsp;<i>").append(escape(detail)).append("</i><br/>");
            }
        } else {
            service.ensureObjectFieldsLoaded(name);
            sb.append("<hr/><i>Loading fields…</i>");
        }
        return sb.toString();
    }

    private String picklistDoc(MetadataService service, MetadataIndex index, String name) {
        PicklistMeta picklist = index.picklist(name);
        if (picklist == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(heading("Picklist", name, picklist.getLabel()));
        if (index.valuesLoaded(name)) {
            sb.append(row("Values", String.valueOf(picklist.getValuesByName().size())));
            sb.append("<hr/>");
            int count = 0;
            for (PicklistValueMeta value : index.valuesFor(name)) {
                if (count++ >= MAX_ROWS) {
                    sb.append("…<br/>");
                    break;
                }
                String label = value.getLabel() != null ? "&nbsp;&nbsp;<i>" + escape(value.getLabel()) + "</i>" : "";
                sb.append(escape(value.getName())).append(label).append("<br/>");
            }
        } else {
            service.ensurePicklistValuesLoaded(name);
            sb.append("<hr/><i>Loading values…</i>");
        }
        return sb.toString();
    }

    private String componentTypeDoc(MetadataService service, MetadataIndex index, String name) {
        ComponentTypeMeta type = index.componentType(name);
        if (type == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(heading("Component Type", name, type.getLabel()));
        if (index.attributesLoaded(name)) {
            sb.append(row("Attributes", String.valueOf(type.getAttributesByName().size())));
            sb.append("<hr/>");
            int count = 0;
            for (AttributeMeta attribute : index.attributesFor(name)) {
                if (count++ >= MAX_ROWS) {
                    sb.append("…<br/>");
                    break;
                }
                String detail = attribute.getType() != null ? attribute.getType() : "";
                sb.append(escape(attribute.getName())).append("&nbsp;&nbsp;<i>").append(escape(detail)).append("</i><br/>");
            }
        } else {
            service.ensureComponentTypeAttributesLoaded(name);
            sb.append("<hr/><i>Loading attributes…</i>");
        }
        return sb.toString();
    }

    private String fieldDoc(MetadataService service, MetadataIndex index, String objectContext, String fieldName) {
        if (objectContext == null) {
            return null;
        }
        if (!index.fieldsLoaded(objectContext)) {
            service.ensureObjectFieldsLoaded(objectContext);
            return heading("Field", fieldName, null) + "<hr/><i>Loading " + escape(objectContext) + " fields…</i>";
        }
        ObjectMeta object = index.object(objectContext);
        FieldMeta field = object != null ? object.getFieldsByName().get(fieldName) : null;
        if (field == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(heading("Field", fieldName, field.getLabel()));
        sb.append(row("On object", objectContext));
        if (field.getType() != null) {
            sb.append(row("Type", field.getType()));
        }
        if (field.getReferencedObject() != null) {
            sb.append(row("References", field.getReferencedObject()));
        }
        if (field.getPicklist() != null) {
            sb.append(row("Picklist", field.getPicklist()));
        }
        sb.append(row("Required", field.isRequired() ? "Yes" : "No"));
        return sb.toString();
    }

    private String heading(String kind, String name, @Nullable String label) {
        String labelPart = label != null && !label.isEmpty() ? " — " + escape(label) : "";
        return "<b>" + escape(name) + "</b><br/>" + kind + labelPart + "<br/>";
    }

    private String row(String key, String value) {
        return key + ": " + escape(value) + "<br/>";
    }

    private String escape(String text) {
        return text == null ? "" : XmlStringUtil.escapeString(text);
    }
}
