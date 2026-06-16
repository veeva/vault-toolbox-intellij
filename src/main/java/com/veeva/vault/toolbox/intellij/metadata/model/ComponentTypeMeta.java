package com.veeva.vault.toolbox.intellij.metadata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An MDL component type (e.g. {@code Object}, {@code Picklist}) and the attributes that
 * are valid on it. Component-type names are loaded eagerly; the attribute list may be
 * loaded lazily, indicated by {@link #isAttributesLoaded()}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComponentTypeMeta {

    private String name;
    private String label;
    private boolean attributesLoaded;
    private Map<String, AttributeMeta> attributesByName = new LinkedHashMap<>();

    /** @return the component type name in MDL casing (e.g. {@code Object}) */
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

    /** @return whether this type's attributes have been fetched and cached */
    public boolean isAttributesLoaded() {
        return attributesLoaded;
    }

    public void setAttributesLoaded(boolean attributesLoaded) {
        this.attributesLoaded = attributesLoaded;
    }

    /** @return attributes keyed by attribute name; empty until lazily loaded */
    public Map<String, AttributeMeta> getAttributesByName() {
        return attributesByName;
    }

    public void setAttributesByName(Map<String, AttributeMeta> attributesByName) {
        this.attributesByName = attributesByName != null ? attributesByName : new LinkedHashMap<>();
    }
}
