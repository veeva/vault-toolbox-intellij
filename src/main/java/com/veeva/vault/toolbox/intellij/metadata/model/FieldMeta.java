package com.veeva.vault.toolbox.intellij.metadata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A field on a Vault object. Reserved for the lazy per-object field loading added in a
 * later milestone; the model exists now so the cache format is forward-compatible.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldMeta {

    private String name;
    private String label;
    private String type;
    private String picklist;
    private String referencedObject;
    private boolean required;

    /** @return the field API name */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /** @return the field data type (e.g. {@code String}, {@code Picklist}, {@code Object}) */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /** @return the referenced picklist name when this is a picklist field, else {@code null} */
    public String getPicklist() {
        return picklist;
    }

    public void setPicklist(String picklist) {
        this.picklist = picklist;
    }

    /** @return the referenced object name for relationship/object fields, else {@code null} */
    public String getReferencedObject() {
        return referencedObject;
    }

    public void setReferencedObject(String referencedObject) {
        this.referencedObject = referencedObject;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
