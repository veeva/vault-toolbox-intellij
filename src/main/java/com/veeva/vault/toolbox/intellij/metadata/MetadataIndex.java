package com.veeva.vault.toolbox.intellij.metadata;

import com.veeva.vault.toolbox.intellij.metadata.model.AttributeMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.ComponentTypeMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.FieldMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.ObjectMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.PicklistMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.PicklistValueMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.RelationshipMeta;

import java.util.Collection;

/**
 * Read-only view over a cached, per-environment Vault metadata snapshot. This is the single
 * interface consumed by both the Schema Explorer and MDL intelligence (completion +
 * validation).
 *
 * <p>All methods are non-blocking, do no I/O, and never make network calls, so they are safe
 * to call from the EDT. Existence checks return a tri-state {@link Existence}: callers that
 * validate (annotators) must only act on {@link Existence#MISSING} and treat
 * {@link Existence#UNKNOWN} (not connected, snapshot not built, or slice not yet loaded) as
 * "do not flag", to avoid false positives.</p>
 */
public interface MetadataIndex {

    /** Tri-state result for existence queries. */
    enum Existence {
        /** The name is known to exist in the snapshot. */
        EXISTS,
        /** The relevant slice is loaded and the name is known not to exist. */
        MISSING,
        /** No snapshot, not connected, or the relevant slice is not yet loaded. */
        UNKNOWN
    }

    /** @return whether a snapshot has been built (i.e. not the empty index). */
    boolean isReady();

    /** @return the vault id the snapshot was built for, or {@code null} for the empty index. */
    String vaultId();

    /** @return epoch millis the snapshot was fetched, or {@code 0} for the empty index. */
    long fetchedEpochMillis();

    Existence objectExists(String objectName);

    Existence fieldExists(String objectName, String fieldName);

    Existence picklistExists(String picklistName);

    Existence picklistValueExists(String picklistName, String valueName);

    Existence componentTypeExists(String componentType);

    Existence docTypeExists(String docTypeName);

    /** @return all object API names; empty when not ready. */
    Collection<String> objectNames();

    /** @return all picklist API names; empty when not ready. */
    Collection<String> picklistNames();

    /** @return all component type names; empty when not ready. */
    Collection<String> componentTypeNames();

    /** @return all document type names; empty when not ready or not populated. */
    Collection<String> docTypeNames();

    /**
     * @return the attributes valid for the given component type. Empty when not ready, when
     * the component type is unknown, or when its attributes have not yet been lazily loaded.
     */
    Collection<AttributeMeta> attributesFor(String componentType);

    /**
     * @return whether the given component type's attributes have been loaded into the
     * snapshot. Used by callers to decide whether a lazy fetch should be triggered.
     */
    boolean attributesLoaded(String componentType);

    /** @return the fields of an object; empty when not loaded or the object is unknown. */
    Collection<FieldMeta> fieldsFor(String objectName);

    /** @return the relationships of an object; empty when not loaded or the object is unknown. */
    Collection<RelationshipMeta> relationshipsFor(String objectName);

    /** @return whether the given object's fields/relationships have been loaded. */
    boolean fieldsLoaded(String objectName);

    /** @return the values of a picklist; empty when not loaded or the picklist is unknown. */
    Collection<PicklistValueMeta> valuesFor(String picklistName);

    /** @return whether the given picklist's values have been loaded. */
    boolean valuesLoaded(String picklistName);

    /** @return the object detail model, or {@code null} if unknown/not ready. */
    ObjectMeta object(String objectName);

    /** @return the picklist detail model, or {@code null} if unknown/not ready. */
    PicklistMeta picklist(String picklistName);

    /** @return the component type detail model, or {@code null} if unknown/not ready. */
    ComponentTypeMeta componentType(String componentType);
}
