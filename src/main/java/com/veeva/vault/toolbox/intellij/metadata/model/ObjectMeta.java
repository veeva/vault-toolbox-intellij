package com.veeva.vault.toolbox.intellij.metadata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Vault object (e.g. {@code product__v}). The name and label are loaded eagerly from
 * the object collection; fields are loaded lazily per object in a later milestone,
 * indicated by {@link #isFieldsLoaded()}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectMeta {

    private String name;
    private String label;
    private boolean fieldsLoaded;
    private Map<String, FieldMeta> fieldsByName = new LinkedHashMap<>();
    private Map<String, RelationshipMeta> relationshipsByName = new LinkedHashMap<>();

    /** @return the object API name (e.g. {@code product__v}) */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** @return the human-readable label, or {@code null} */
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /** @return whether this object's fields have been fetched and cached */
    public boolean isFieldsLoaded() {
        return fieldsLoaded;
    }

    public void setFieldsLoaded(boolean fieldsLoaded) {
        this.fieldsLoaded = fieldsLoaded;
    }

    /** @return fields keyed by field name; empty until lazily loaded */
    public Map<String, FieldMeta> getFieldsByName() {
        return fieldsByName;
    }

    public void setFieldsByName(Map<String, FieldMeta> fieldsByName) {
        this.fieldsByName = fieldsByName != null ? fieldsByName : new LinkedHashMap<>();
    }

    /** @return relationships keyed by relationship name; empty until fields are lazily loaded */
    public Map<String, RelationshipMeta> getRelationshipsByName() {
        return relationshipsByName;
    }

    public void setRelationshipsByName(Map<String, RelationshipMeta> relationshipsByName) {
        this.relationshipsByName = relationshipsByName != null ? relationshipsByName : new LinkedHashMap<>();
    }
}
