package com.veeva.vault.toolbox.intellij.metadata.mdl;

import java.util.Map;

/**
 * Maps an MDL attribute slot to the kind of Vault component it references, so completion and
 * validation know whether a string value should be checked as an object, a picklist, etc.
 *
 * <p>The mapping is intentionally conservative: only high-confidence attribute names are
 * registered, and anything unrecognized returns {@link RefKind#NONE} (meaning "do not
 * validate, do not suggest"). This keeps the feature safe to ship with partial coverage —
 * new mappings are purely additive and can never introduce false positives for slots that are
 * not yet mapped.</p>
 */
public final class MdlReferenceKindRegistry {

    /** The kind of thing an MDL string slot refers to. */
    public enum RefKind {
        OBJECT,
        FIELD,
        PICKLIST,
        COMPONENT_TYPE,
        DOC_TYPE,
        NONE
    }

    /**
     * High-confidence attribute-name → kind mappings. Keyed by attribute name alone (the
     * enclosing component type is accepted for future, more specific rules but not required
     * today). These attributes carry an unambiguous reference value in MDL.
     */
    private static final Map<String, RefKind> KIND_BY_ATTRIBUTE = Map.of(
            "object", RefKind.OBJECT,
            "picklist", RefKind.PICKLIST,
            "field", RefKind.FIELD
    );

    private MdlReferenceKindRegistry() {
    }

    /**
     * @param componentType the enclosing component type (reserved for future, more specific
     *                      rules), may be {@code null}
     * @param attributeName the attribute name owning the string slot, may be {@code null}
     * @return the reference kind, or {@link RefKind#NONE} when the slot is not mapped
     */
    public static RefKind kindFor(String componentType, String attributeName) {
        if (attributeName == null) {
            return RefKind.NONE;
        }
        return KIND_BY_ATTRIBUTE.getOrDefault(attributeName.toLowerCase(), RefKind.NONE);
    }
}
