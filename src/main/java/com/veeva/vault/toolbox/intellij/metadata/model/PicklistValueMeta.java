package com.veeva.vault.toolbox.intellij.metadata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single value of a Vault picklist. Reserved for the lazy per-picklist value loading
 * added in a later milestone; the model exists now so the cache format is
 * forward-compatible.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PicklistValueMeta {

    private String name;
    private String label;
    private boolean active = true;

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
