package com.veeva.vault.toolbox.intellij.language;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.veeva.vault.toolbox.intellij.language.psi.MdlTypes;
import com.veeva.vault.toolbox.intellij.metadata.mdl.MdlReferenceKindRegistry;
import com.veeva.vault.toolbox.intellij.metadata.mdl.MdlReferenceKindRegistry.RefKind;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the Vault reference under the caret (or at a PSI leaf) in an MDL file — the schema
 * "subject" a given position points at. Shared by the editor right-click actions so that
 * navigation, find-usages, and documentation all agree on what the caret is pointing to.
 */
public final class MdlReferenceResolver {

    private MdlReferenceResolver() {
    }

    /** The resolved reference: a kind, the referenced name, and (for fields) the owning object. */
    public static final class Ref {
        public final RefKind kind;
        public final String name;
        public final String objectContext;

        Ref(RefKind kind, String name, String objectContext) {
            this.kind = kind;
            this.name = name;
            this.objectContext = objectContext;
        }
    }

    @Nullable
    public static Ref resolveAtCaret(@Nullable Editor editor, @Nullable PsiFile file) {
        if (editor == null || file == null) {
            return null;
        }
        return resolve(file.findElementAt(editor.getCaretModel().getOffset()));
    }

    @Nullable
    public static Ref resolve(@Nullable PsiElement leaf) {
        if (leaf == null || leaf.getNode() == null) {
            return null;
        }
        com.intellij.psi.tree.IElementType type = leaf.getNode().getElementType();

        if (type == MdlTypes.COMPONENT_TYPE) {
            return new Ref(RefKind.COMPONENT_TYPE, leaf.getText(), null);
        }
        if (type == MdlTypes.RECORD_NAME) {
            String componentType = MdlPsiContext.componentTypeOf(leaf);
            RefKind kind = "Object".equalsIgnoreCase(componentType) ? RefKind.OBJECT
                    : "Picklist".equalsIgnoreCase(componentType) ? RefKind.PICKLIST : RefKind.NONE;
            return new Ref(kind, leaf.getText(), null);
        }

        PsiElement stringValue = enclosingStringValue(leaf);
        if (stringValue == null) {
            return null;
        }
        RefKind kind = MdlReferenceKindRegistry.kindFor(
                MdlPsiContext.componentTypeOf(stringValue), MdlPsiContext.attributeNameOf(stringValue));
        if (kind == RefKind.NONE) {
            return null;
        }
        MdlPsiContext.UnquotedValue value = MdlPsiContext.unquote(stringValue);
        if (value == null || value.getText().isEmpty()) {
            return null;
        }
        // MDL references may be qualified (e.g. Picklist.default_status__v); the index keys on the
        // bare name, and definition files are named <Type>.<bareName>.mdl.
        String name = MdlPsiContext.bareName(value.getText());
        String objectContext = kind == RefKind.FIELD
                ? MdlPsiContext.recordNameForComponentType(stringValue, "Object") : null;
        return new Ref(kind, name, objectContext);
    }

    @Nullable
    private static PsiElement enclosingStringValue(PsiElement element) {
        ASTNode node = element.getNode();
        while (node != null && node.getElementType() != MdlTypes.STRING_VALUE) {
            node = node.getTreeParent();
        }
        return node != null ? node.getPsi() : null;
    }
}
