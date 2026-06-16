package com.veeva.vault.toolbox.intellij.metadata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A relationship declared on a Vault object, pointing at another object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelationshipMeta {

    private String name;
    private String label;
    private String type;
    private String referencedObject;

    /** @return the relationship API name */
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

    /** @return the relationship type (e.g. {@code reference}, {@code parent}) */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /** @return the API name of the object this relationship points to, or {@code null} */
    public String getReferencedObject() {
        return referencedObject;
    }

    public void setReferencedObject(String referencedObject) {
        this.referencedObject = referencedObject;
    }
}
