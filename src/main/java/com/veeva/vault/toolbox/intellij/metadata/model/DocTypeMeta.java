package com.veeva.vault.toolbox.intellij.metadata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A Vault document type. Not populated in the current milestone (VAPIL exposes no clean
 * bulk document-type metadata call); the model exists so the cache format is
 * forward-compatible and validation can extend to document types later without a schema
 * version bump.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocTypeMeta {

    private String name;
    private String label;

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
}
