package com.veeva.vault.toolbox.intellij.metadata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root of the per-environment metadata snapshot that is cached to disk (as JSON) and held
 * in memory by the metadata service. Built once on a background thread and treated as
 * immutable after publication, so it is safe to read from the EDT.
 *
 * <p>Names (objects, picklists, component types) are populated eagerly; deeper slices
 * (object fields, picklist values, component-type attributes) are filled in lazily, which
 * the {@code *Loaded} flags on the child models track.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VaultMetadata {

    /** Bump when the model shape changes so incompatible on-disk caches are discarded. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private String vaultId;
    private String vaultDns;
    private long fetchedEpochMillis;
    private int schemaVersion = CURRENT_SCHEMA_VERSION;

    private Map<String, ObjectMeta> objectsByName = new LinkedHashMap<>();
    private Map<String, PicklistMeta> picklistsByName = new LinkedHashMap<>();
    private Map<String, ComponentTypeMeta> componentTypesByName = new LinkedHashMap<>();
    private Map<String, DocTypeMeta> docTypesByName = new LinkedHashMap<>();

    public String getVaultId() {
        return vaultId;
    }

    public void setVaultId(String vaultId) {
        this.vaultId = vaultId;
    }

    public String getVaultDns() {
        return vaultDns;
    }

    public void setVaultDns(String vaultDns) {
        this.vaultDns = vaultDns;
    }

    /** @return epoch millis the snapshot was fetched from Vault */
    public long getFetchedEpochMillis() {
        return fetchedEpochMillis;
    }

    public void setFetchedEpochMillis(long fetchedEpochMillis) {
        this.fetchedEpochMillis = fetchedEpochMillis;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Map<String, ObjectMeta> getObjectsByName() {
        return objectsByName;
    }

    public void setObjectsByName(Map<String, ObjectMeta> objectsByName) {
        this.objectsByName = objectsByName != null ? objectsByName : new LinkedHashMap<>();
    }

    public Map<String, PicklistMeta> getPicklistsByName() {
        return picklistsByName;
    }

    public void setPicklistsByName(Map<String, PicklistMeta> picklistsByName) {
        this.picklistsByName = picklistsByName != null ? picklistsByName : new LinkedHashMap<>();
    }

    public Map<String, ComponentTypeMeta> getComponentTypesByName() {
        return componentTypesByName;
    }

    public void setComponentTypesByName(Map<String, ComponentTypeMeta> componentTypesByName) {
        this.componentTypesByName = componentTypesByName != null ? componentTypesByName : new LinkedHashMap<>();
    }

    public Map<String, DocTypeMeta> getDocTypesByName() {
        return docTypesByName;
    }

    public void setDocTypesByName(Map<String, DocTypeMeta> docTypesByName) {
        this.docTypesByName = docTypesByName != null ? docTypesByName : new LinkedHashMap<>();
    }
}
