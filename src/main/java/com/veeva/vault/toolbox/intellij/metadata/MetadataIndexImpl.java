package com.veeva.vault.toolbox.intellij.metadata;

import com.veeva.vault.toolbox.intellij.metadata.model.AttributeMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.ComponentTypeMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.FieldMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.ObjectMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.PicklistMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.PicklistValueMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.RelationshipMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.VaultMetadata;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Immutable {@link MetadataIndex} backed by a {@link VaultMetadata} snapshot. Instances are
 * created on a background thread and published via a single volatile reference in
 * {@link MetadataService}; readers never mutate them.
 *
 * <p>Existence checks are keyed by name with O(1) map lookups. When the backing snapshot is
 * absent (the {@link #EMPTY} sentinel) every existence check returns
 * {@link Existence#UNKNOWN} and every enumeration returns empty.</p>
 */
public final class MetadataIndexImpl implements MetadataIndex {

    /** Shared empty index used before the first load and while disconnected. */
    public static final MetadataIndexImpl EMPTY = new MetadataIndexImpl(null);

    private final VaultMetadata metadata;

    public MetadataIndexImpl(VaultMetadata metadata) {
        this.metadata = metadata;
    }

    /** @return the backing snapshot for in-package mutation/persistence, or {@code null} when empty. */
    VaultMetadata getMetadata() {
        return metadata;
    }

    @Override
    public boolean isReady() {
        return metadata != null;
    }

    @Override
    public String vaultId() {
        return metadata != null ? metadata.getVaultId() : null;
    }

    @Override
    public long fetchedEpochMillis() {
        return metadata != null ? metadata.getFetchedEpochMillis() : 0L;
    }

    @Override
    public Existence objectExists(String objectName) {
        return existence(metadata == null ? null : metadata.getObjectsByName(), objectName);
    }

    @Override
    public Existence fieldExists(String objectName, String fieldName) {
        ObjectMeta object = object(objectName);
        if (object == null || !object.isFieldsLoaded()) {
            return Existence.UNKNOWN;
        }
        if (fieldName == null || fieldName.isEmpty()) {
            return Existence.UNKNOWN;
        }
        return object.getFieldsByName().containsKey(fieldName) ? Existence.EXISTS : Existence.MISSING;
    }

    @Override
    public Existence picklistExists(String picklistName) {
        return existence(metadata == null ? null : metadata.getPicklistsByName(), picklistName);
    }

    @Override
    public Existence picklistValueExists(String picklistName, String valueName) {
        PicklistMeta picklist = picklist(picklistName);
        if (picklist == null || !picklist.isValuesLoaded()) {
            return Existence.UNKNOWN;
        }
        if (valueName == null || valueName.isEmpty()) {
            return Existence.UNKNOWN;
        }
        return picklist.getValuesByName().containsKey(valueName) ? Existence.EXISTS : Existence.MISSING;
    }

    @Override
    public Existence componentTypeExists(String componentType) {
        return existence(metadata == null ? null : metadata.getComponentTypesByName(), componentType);
    }

    @Override
    public Existence docTypeExists(String docTypeName) {
        // Document types are not populated yet; an empty map means "unknown", never "missing".
        if (metadata == null || metadata.getDocTypesByName().isEmpty()) {
            return Existence.UNKNOWN;
        }
        return existence(metadata.getDocTypesByName(), docTypeName);
    }

    @Override
    public Collection<String> objectNames() {
        return metadata == null ? Collections.emptyList() : metadata.getObjectsByName().keySet();
    }

    @Override
    public Collection<String> picklistNames() {
        return metadata == null ? Collections.emptyList() : metadata.getPicklistsByName().keySet();
    }

    @Override
    public Collection<String> componentTypeNames() {
        return metadata == null ? Collections.emptyList() : metadata.getComponentTypesByName().keySet();
    }

    @Override
    public Collection<String> docTypeNames() {
        return metadata == null ? Collections.emptyList() : metadata.getDocTypesByName().keySet();
    }

    @Override
    public Collection<AttributeMeta> attributesFor(String componentType) {
        ComponentTypeMeta type = componentType(componentType);
        return type == null ? Collections.emptyList() : type.getAttributesByName().values();
    }

    @Override
    public boolean attributesLoaded(String componentType) {
        ComponentTypeMeta type = componentType(componentType);
        return type != null && type.isAttributesLoaded();
    }

    @Override
    public Collection<FieldMeta> fieldsFor(String objectName) {
        ObjectMeta object = object(objectName);
        return object == null ? Collections.emptyList() : object.getFieldsByName().values();
    }

    @Override
    public Collection<RelationshipMeta> relationshipsFor(String objectName) {
        ObjectMeta object = object(objectName);
        return object == null ? Collections.emptyList() : object.getRelationshipsByName().values();
    }

    @Override
    public boolean fieldsLoaded(String objectName) {
        ObjectMeta object = object(objectName);
        return object != null && object.isFieldsLoaded();
    }

    @Override
    public Collection<PicklistValueMeta> valuesFor(String picklistName) {
        PicklistMeta picklist = picklist(picklistName);
        return picklist == null ? Collections.emptyList() : picklist.getValuesByName().values();
    }

    @Override
    public boolean valuesLoaded(String picklistName) {
        PicklistMeta picklist = picklist(picklistName);
        return picklist != null && picklist.isValuesLoaded();
    }

    @Override
    public ComponentTypeMeta componentType(String componentType) {
        if (metadata == null || componentType == null) {
            return null;
        }
        return metadata.getComponentTypesByName().get(componentType);
    }

    @Override
    public ObjectMeta object(String objectName) {
        if (metadata == null || objectName == null) {
            return null;
        }
        return metadata.getObjectsByName().get(objectName);
    }

    @Override
    public PicklistMeta picklist(String picklistName) {
        if (metadata == null || picklistName == null) {
            return null;
        }
        return metadata.getPicklistsByName().get(picklistName);
    }

    private static Existence existence(Map<String, ?> map, String key) {
        if (map == null) {
            return Existence.UNKNOWN;
        }
        if (key == null || key.isEmpty()) {
            return Existence.UNKNOWN;
        }
        return map.containsKey(key) ? Existence.EXISTS : Existence.MISSING;
    }
}
