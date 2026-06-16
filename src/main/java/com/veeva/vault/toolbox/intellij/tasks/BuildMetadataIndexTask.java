package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.veeva.vault.toolbox.intellij.metadata.MetadataService;
import com.veeva.vault.toolbox.intellij.metadata.model.AttributeMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.ComponentTypeMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.ObjectMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.PicklistMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.VaultMetadata;
import com.veeva.vault.vapil.api.model.common.ComponentType;
import com.veeva.vault.vapil.api.model.metadata.VaultObject;
import com.veeva.vault.vapil.api.model.response.MetaDataComponentTypeBulkResponse;
import com.veeva.vault.vapil.api.model.response.MetaDataObjectBulkResponse;
import com.veeva.vault.vapil.api.model.response.PicklistResponse;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.MetaDataRequest;
import com.veeva.vault.vapil.api.request.PicklistRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Builds the eager Vault metadata snapshot (object names, picklist names, and MDL component
 * types) from VAPIL and publishes it to the {@link MetadataService}. Deeper slices — object
 * fields, picklist values, per-type attributes — are loaded lazily elsewhere.
 *
 * <p>On a non-forced run a fresh-enough disk cache is published without any network calls; a
 * stale cache is published immediately for responsiveness and then refreshed in the background.</p>
 */
public class BuildMetadataIndexTask extends ToolboxTask {

    private static final Logger logger = LoggerFactory.getLogger(BuildMetadataIndexTask.class);

    private final boolean force;

    /**
     * @param project the IntelliJ project, may be {@code null}
     * @param force   when {@code true}, ignores cache freshness and re-fetches from Vault
     */
    public BuildMetadataIndexTask(@Nullable Project project, boolean force) {
        super(project, "Loading Vault Schema", true);
        this.force = force;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        if (toolboxProject == null || !toolboxProject.isConnected()) {
            return;
        }
        MetadataService service = MetadataService.getInstance(getProject());
        if (service == null) {
            return;
        }

        Integer vaultIdInt = toolboxProject.getVaultId();
        if (vaultIdInt == null) {
            return;
        }
        String vaultId = vaultIdInt.toString();
        service.beginLoad(vaultId);

        indicator.setIndeterminate(true);
        indicator.setText("Loading Vault schema...");

        try {
            // Warm-start from disk; publish a stale cache immediately, then refresh below.
            VaultMetadata cached = service.loadFromDisk(vaultId);
            if (cached != null) {
                service.publish(cached);
                if (!force && !service.isStale(cached)) {
                    return;
                }
            }

            VaultMetadata metadata = new VaultMetadata();
            metadata.setVaultId(vaultId);
            metadata.setVaultDns(toolboxProject.getVaultDNS());

            indicator.checkCanceled();
            indicator.setText("Loading component types...");
            if (!loadComponentTypes(metadata)) {
                return;
            }

            indicator.checkCanceled();
            indicator.setText("Loading objects...");
            if (!loadObjects(metadata)) {
                return;
            }

            indicator.checkCanceled();
            indicator.setText("Loading picklists...");
            if (!loadPicklists(metadata)) {
                return;
            }

            metadata.setFetchedEpochMillis(System.currentTimeMillis());
            service.publish(metadata);
            service.saveToDisk(metadata);
        } catch (com.intellij.openapi.progress.ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to build Vault metadata index", e);
        }
    }

    private boolean loadComponentTypes(VaultMetadata metadata) {
        MetaDataComponentTypeBulkResponse response = toolboxProject.getVaultClient()
                .newRequest(MetaDataRequest.class)
                .retrieveAllComponentMetadata();
        if (failed(response)) {
            return false;
        }
        List<ComponentType> data = response.getData();
        if (data == null) {
            return true;
        }
        for (ComponentType type : data) {
            if (type == null || type.getName() == null) {
                continue;
            }
            ComponentTypeMeta meta = new ComponentTypeMeta();
            meta.setName(type.getName());
            meta.setLabel(type.getLabel());
            // The bulk call may already include attributes; if so, treat the type as loaded.
            List<ComponentType.Attribute> attributes = type.getAttributes();
            if (attributes != null && !attributes.isEmpty()) {
                LinkedHashMap<String, AttributeMeta> attributeMap = new LinkedHashMap<>();
                for (ComponentType.Attribute attribute : attributes) {
                    if (attribute == null || attribute.getName() == null) {
                        continue;
                    }
                    AttributeMeta attributeMeta = new AttributeMeta();
                    attributeMeta.setName(attribute.getName());
                    attributeMeta.setType(attribute.getType());
                    attributeMeta.setRequiredness(attribute.getRequiredness());
                    attributeMap.put(attribute.getName(), attributeMeta);
                }
                meta.setAttributesByName(attributeMap);
                meta.setAttributesLoaded(true);
            }
            metadata.getComponentTypesByName().put(meta.getName(), meta);
        }
        return true;
    }

    private boolean loadObjects(VaultMetadata metadata) {
        MetaDataObjectBulkResponse response = toolboxProject.getVaultClient()
                .newRequest(MetaDataRequest.class)
                .retrieveObjectCollection();
        if (failed(response)) {
            return false;
        }
        List<VaultObject> objects = response.getObjects();
        if (objects == null) {
            return true;
        }
        for (VaultObject object : objects) {
            if (object == null || object.getName() == null) {
                continue;
            }
            ObjectMeta meta = new ObjectMeta();
            meta.setName(object.getName());
            meta.setLabel(object.getLabel());
            metadata.getObjectsByName().put(meta.getName(), meta);
        }
        return true;
    }

    private boolean loadPicklists(VaultMetadata metadata) {
        PicklistResponse response = toolboxProject.getVaultClient()
                .newRequest(PicklistRequest.class)
                .retrieveAllPicklists();
        if (failed(response)) {
            return false;
        }
        List<PicklistResponse.Picklist> picklists = response.getPicklists();
        if (picklists == null) {
            return true;
        }
        for (PicklistResponse.Picklist picklist : picklists) {
            if (picklist == null || picklist.getName() == null) {
                continue;
            }
            PicklistMeta meta = new PicklistMeta();
            meta.setName(picklist.getName());
            meta.setLabel(picklist.getLabel());
            metadata.getPicklistsByName().put(meta.getName(), meta);
        }
        return true;
    }

    /**
     * @return {@code true} when the response is missing or failed; also routes session-expiry
     * failures through the project's standard handler so the user is prompted to re-authenticate.
     */
    private boolean failed(VaultResponse response) {
        if (response == null) {
            return true;
        }
        if (response.isFailure()) {
            toolboxProject.handleSessionExpiration(response);
            return true;
        }
        return false;
    }

    /** This task manages its own snapshot publication; suppress the base VFS refresh. */
    @Override
    public void onFinished() {
        // Intentionally does not call super.onFinished(): no files change during a schema load.
    }
}
