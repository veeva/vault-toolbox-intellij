package com.veeva.vault.toolbox.intellij.metadata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single attribute that is valid on an MDL component type (e.g. {@code label} on
 * {@code Object}). Populated from VAPIL {@code retrieveComponentTypeMetadata}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributeMeta {

    private String name;
    private String type;
    private String requiredness;

    /** @return the attribute API name (e.g. {@code label}) */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** @return the attribute data type, or {@code null} if unknown */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /** @return the requiredness descriptor reported by Vault, or {@code null} */
    public String getRequiredness() {
        return requiredness;
    }

    public void setRequiredness(String requiredness) {
        this.requiredness = requiredness;
    }
}
