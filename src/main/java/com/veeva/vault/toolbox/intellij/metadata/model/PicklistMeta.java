package com.veeva.vault.toolbox.intellij.metadata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Vault picklist (e.g. {@code product_status__v}). The name and label are loaded
 * eagerly; values are loaded lazily per picklist in a later milestone, indicated by
 * {@link #isValuesLoaded()}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PicklistMeta {

    private String name;
    private String label;
    private boolean valuesLoaded;
    private Map<String, PicklistValueMeta> valuesByName = new LinkedHashMap<>();

    /** @return the picklist API name */
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

    /** @return whether this picklist's values have been fetched and cached */
    public boolean isValuesLoaded() {
        return valuesLoaded;
    }

    public void setValuesLoaded(boolean valuesLoaded) {
        this.valuesLoaded = valuesLoaded;
    }

    /** @return values keyed by value name; empty until lazily loaded */
    public Map<String, PicklistValueMeta> getValuesByName() {
        return valuesByName;
    }

    public void setValuesByName(Map<String, PicklistValueMeta> valuesByName) {
        this.valuesByName = valuesByName != null ? valuesByName : new LinkedHashMap<>();
    }
}
