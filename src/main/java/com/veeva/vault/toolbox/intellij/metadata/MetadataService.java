package com.veeva.vault.toolbox.intellij.metadata;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.veeva.vault.toolbox.intellij.metadata.model.AttributeMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.ComponentTypeMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.FieldMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.ObjectMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.PicklistMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.PicklistValueMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.RelationshipMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.VaultMetadata;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.BuildMetadataIndexTask;
import com.veeva.vault.vapil.api.model.common.ComponentType;
import com.veeva.vault.vapil.api.model.metadata.VaultObject;
import com.veeva.vault.vapil.api.model.metadata.VaultObjectField;
import com.veeva.vault.vapil.api.model.response.MetaDataComponentTypeResponse;
import com.veeva.vault.vapil.api.model.response.MetaDataObjectResponse;
import com.veeva.vault.vapil.api.model.response.PicklistValueResponse;
import com.veeva.vault.vapil.api.request.MetaDataRequest;
import com.veeva.vault.vapil.api.request.PicklistRequest;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Project-level holder for the cached Vault metadata snapshot. Registered as a project
 * service in {@code plugin.xml} and retrieved via {@link #getInstance(Project)}.
 *
 * <p>The current snapshot is held behind a single {@code volatile} reference so that readers
 * (MDL completion and the schema annotator) perform a plain, lock-free, I/O-free volatile read
 * and are safe to call from the EDT. All fetching, disk I/O, and snapshot construction happen
 * on a background thread (see {@link BuildMetadataIndexTask}); the background thread is the only
 * writer and publishes by assigning {@link #current} as its final step.</p>
 */
public final class MetadataService {

    private static final Logger logger = LoggerFactory.getLogger(MetadataService.class);

    /** Hidden cache directory placed beside the extracted MDL for a vault. */
    public static final String SCHEMA_DIR = ".schema";
    private static final String CACHE_FILE = "metadata.json";
    /** Re-fetch eager metadata after this age on a non-forced refresh. */
    private static final long CACHE_TTL_MILLIS = 24L * 60 * 60 * 1000;

    private final Project project;

    /** The published snapshot. Never {@code null}; starts as the empty index. */
    private volatile MetadataIndexImpl current = MetadataIndexImpl.EMPTY;

    /**
     * Vault id of the load that is allowed to publish. Guards against a slow background load
     * for a previous vault overwriting the snapshot after the user switches environments.
     */
    private volatile String expectedVaultId;

    /** Entities whose deep-slice fetch is in flight, to avoid duplicate/looping loads. */
    private final Set<String> loadingComponentTypes = ConcurrentHashMap.newKeySet();
    private final Set<String> loadingObjects = ConcurrentHashMap.newKeySet();
    private final Set<String> loadingPicklists = ConcurrentHashMap.newKeySet();

    /** UI listeners notified (on the EDT) whenever the snapshot changes. */
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    public MetadataService(Project project) {
        this.project = project;
    }

    public static MetadataService getInstance(Project project) {
        return project.getService(MetadataService.class);
    }

    /**
     * @return the current snapshot view. Safe to call from the EDT: a volatile read with no I/O.
     */
    public MetadataIndex getIndex() {
        return current;
    }

    /** Registers a listener invoked on the EDT whenever the snapshot changes. */
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    // ---------------------------------------------------------------------------------------
    // Connection lifecycle (wired from ToolboxProjectPanel's ConnectionListener)
    // ---------------------------------------------------------------------------------------

    /** Invoked when a vault connection is established; refreshes the eager metadata. */
    public void onConnected() {
        refreshAsync(false);
    }

    /** Invoked when the connection is lost; clears the snapshot so validation stops firing. */
    public void onDisconnected() {
        expectedVaultId = null;
        loadingComponentTypes.clear();
        loadingObjects.clear();
        loadingPicklists.clear();
        publishEmpty();
    }

    /**
     * Launches a background build of the eager metadata snapshot.
     *
     * @param force when {@code true}, ignores cache freshness and re-fetches from Vault
     */
    public void refreshAsync(boolean force) {
        ToolboxProject toolboxProject = ToolboxProject.getInstance(project);
        if (toolboxProject == null || !toolboxProject.isConnected()) {
            return;
        }
        ProgressManager.getInstance().run(new BuildMetadataIndexTask(project, force));
    }

    // ---------------------------------------------------------------------------------------
    // Called by BuildMetadataIndexTask (background thread)
    // ---------------------------------------------------------------------------------------

    /** Records the vault id whose load is currently authoritative. */
    public void beginLoad(String vaultId) {
        this.expectedVaultId = vaultId;
    }

    /** @return whether the eager cache is missing or older than the TTL. */
    public boolean isStale(VaultMetadata metadata) {
        return metadata == null || System.currentTimeMillis() - metadata.getFetchedEpochMillis() > CACHE_TTL_MILLIS;
    }

    /**
     * Publishes a freshly built snapshot, unless a newer load for a different vault has since
     * started. Triggers a re-highlight so squiggles reflect the new data.
     */
    public void publish(VaultMetadata metadata) {
        if (metadata == null) {
            return;
        }
        if (expectedVaultId != null && !expectedVaultId.equals(metadata.getVaultId())) {
            logger.debug("Discarding metadata for stale vault {}", metadata.getVaultId());
            return;
        }
        current = new MetadataIndexImpl(metadata);
        onPublished();
    }

    private void publishEmpty() {
        current = MetadataIndexImpl.EMPTY;
        onPublished();
    }

    // ---------------------------------------------------------------------------------------
    // Lazy component-type attribute loading (triggered from completion)
    // ---------------------------------------------------------------------------------------

    /**
     * Ensures the attributes for the given component type are loaded, fetching them on a
     * background thread if necessary. Returns immediately; the snapshot is republished when the
     * fetch completes. A per-type guard prevents duplicate fetches and fetch/restart loops.
     */
    public void ensureComponentTypeAttributesLoaded(String componentType) {
        if (componentType == null || componentType.isEmpty()) {
            return;
        }
        MetadataIndex index = current;
        if (!index.isReady() || index.attributesLoaded(componentType)) {
            return;
        }
        if (!loadingComponentTypes.add(componentType)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                doLoadComponentTypeAttributes(componentType);
            } catch (Exception e) {
                logger.warn("Failed to load attributes for component type {}", componentType, e);
            } finally {
                loadingComponentTypes.remove(componentType);
            }
        });
    }

    private void doLoadComponentTypeAttributes(String componentType) {
        VaultMetadata snapshot = current.getMetadata();
        if (snapshot == null) {
            return;
        }
        ComponentTypeMeta meta = snapshot.getComponentTypesByName().get(componentType);
        if (meta == null || meta.isAttributesLoaded()) {
            return;
        }
        ToolboxProject toolboxProject = ToolboxProject.getInstance(project);
        if (toolboxProject == null || !toolboxProject.isConnected()) {
            return;
        }
        MetaDataComponentTypeResponse response = toolboxProject.getVaultClient()
                .newRequest(MetaDataRequest.class)
                .retrieveComponentTypeMetadata(componentType);
        if (response == null || response.isFailure() || response.getData() == null) {
            toolboxProject.handleSessionExpiration(response);
            return;
        }

        Map<String, AttributeMeta> attributes = new LinkedHashMap<>();
        List<ComponentType.Attribute> data = response.getData().getAttributes();
        if (data != null) {
            for (ComponentType.Attribute attribute : data) {
                if (attribute == null || attribute.getName() == null) {
                    continue;
                }
                AttributeMeta attributeMeta = new AttributeMeta();
                attributeMeta.setName(attribute.getName());
                attributeMeta.setType(attribute.getType());
                attributeMeta.setRequiredness(attribute.getRequiredness());
                attributes.put(attribute.getName(), attributeMeta);
            }
        }

        meta.setAttributesByName(attributes);
        meta.setAttributesLoaded(true);
        publish(snapshot);
        saveToDisk(snapshot);
    }

    /**
     * Ensures the fields and relationships for the given object are loaded, fetching them synchronously.
     * This performs network I/O and should not be called directly on the EDT.
     */
    public void ensureObjectFieldsLoadedSync(String objectName) {
        if (objectName == null || objectName.isEmpty()) {
            return;
        }
        MetadataIndex index = current;
        if (!index.isReady() || index.fieldsLoaded(objectName)) {
            return;
        }
        if (!loadingObjects.add(objectName)) {
            while (loadingObjects.contains(objectName)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            return;
        }
        try {
            doLoadObjectFields(objectName);
        } catch (Exception e) {
            logger.warn("Failed to load fields for object {}", objectName, e);
        } finally {
            loadingObjects.remove(objectName);
        }
    }

    /**
     * Ensures the fields and relationships for the given object are loaded, fetching them on a
     * background thread if necessary. Returns immediately; the snapshot is republished when done.
     */
    public void ensureObjectFieldsLoaded(String objectName) {
        if (objectName == null || objectName.isEmpty()) {
            return;
        }
        MetadataIndex index = current;
        if (!index.isReady() || index.fieldsLoaded(objectName)) {
            return;
        }
        if (!loadingObjects.add(objectName)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                doLoadObjectFields(objectName);
            } catch (Exception e) {
                logger.warn("Failed to load fields for object {}", objectName, e);
            } finally {
                loadingObjects.remove(objectName);
            }
        });
    }

    private void doLoadObjectFields(String objectName) {
        VaultMetadata snapshot = current.getMetadata();
        if (snapshot == null) {
            return;
        }
        ObjectMeta meta = snapshot.getObjectsByName().get(objectName);
        if (meta == null || meta.isFieldsLoaded()) {
            return;
        }
        ToolboxProject toolboxProject = ToolboxProject.getInstance(project);
        if (toolboxProject == null || !toolboxProject.isConnected()) {
            return;
        }
        MetaDataObjectResponse response = toolboxProject.getVaultClient()
                .newRequest(MetaDataRequest.class)
                .retrieveObjectMetadata(objectName);
        if (response == null || response.isFailure() || response.getObject() == null) {
            toolboxProject.handleSessionExpiration(response);
            return;
        }
        VaultObject object = response.getObject();

        Map<String, FieldMeta> fields = new LinkedHashMap<>();
        if (object.getFields() != null) {
            for (VaultObjectField field : object.getFields()) {
                if (field == null || field.getName() == null) {
                    continue;
                }
                FieldMeta fieldMeta = new FieldMeta();
                fieldMeta.setName(field.getName());
                fieldMeta.setLabel(field.getLabel());
                fieldMeta.setType(field.getType());
                fieldMeta.setPicklist(field.getPicklist());
                fieldMeta.setRequired(Boolean.TRUE.equals(field.getRequired()));
                if (field.getObjectReference() != null) {
                    fieldMeta.setReferencedObject(field.getObjectReference().getName());
                }
                fields.put(field.getName(), fieldMeta);
            }
        }

        Map<String, RelationshipMeta> relationships = new LinkedHashMap<>();
        if (object.getRelationships() != null) {
            for (VaultObject.Relationship relationship : object.getRelationships()) {
                if (relationship == null || relationship.getRelationshipName() == null) {
                    continue;
                }
                RelationshipMeta relationshipMeta = new RelationshipMeta();
                relationshipMeta.setName(relationship.getRelationshipName());
                relationshipMeta.setLabel(relationship.getRelationshipLabel());
                relationshipMeta.setType(relationship.getRelationshipType());
                if (relationship.getObjectReference() != null) {
                    relationshipMeta.setReferencedObject(relationship.getObjectReference().getName());
                }
                relationships.put(relationshipMeta.getName(), relationshipMeta);
            }
        }

        meta.setFieldsByName(fields);
        meta.setRelationshipsByName(relationships);
        meta.setFieldsLoaded(true);
        publish(snapshot);
        saveToDisk(snapshot);
    }

    /**
     * Ensures the values for the given picklist are loaded, fetching them on a background thread
     * if necessary. Returns immediately; the snapshot is republished when done.
     */
    public void ensurePicklistValuesLoaded(String picklistName) {
        if (picklistName == null || picklistName.isEmpty()) {
            return;
        }
        MetadataIndex index = current;
        if (!index.isReady() || index.valuesLoaded(picklistName)) {
            return;
        }
        if (!loadingPicklists.add(picklistName)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                doLoadPicklistValues(picklistName);
            } catch (Exception e) {
                logger.warn("Failed to load values for picklist {}", picklistName, e);
            } finally {
                loadingPicklists.remove(picklistName);
            }
        });
    }

    private void doLoadPicklistValues(String picklistName) {
        VaultMetadata snapshot = current.getMetadata();
        if (snapshot == null) {
            return;
        }
        PicklistMeta meta = snapshot.getPicklistsByName().get(picklistName);
        if (meta == null || meta.isValuesLoaded()) {
            return;
        }
        ToolboxProject toolboxProject = ToolboxProject.getInstance(project);
        if (toolboxProject == null || !toolboxProject.isConnected()) {
            return;
        }
        PicklistValueResponse response = toolboxProject.getVaultClient()
                .newRequest(PicklistRequest.class)
                .retrievePicklistValues(picklistName);
        if (response == null || response.isFailure() || response.getPicklistValues() == null) {
            toolboxProject.handleSessionExpiration(response);
            return;
        }

        Map<String, PicklistValueMeta> values = new LinkedHashMap<>();
        for (PicklistValueResponse.PicklistValue value : response.getPicklistValues()) {
            if (value == null || value.getName() == null) {
                continue;
            }
            PicklistValueMeta valueMeta = new PicklistValueMeta();
            valueMeta.setName(value.getName());
            valueMeta.setLabel(value.getLabel());
            values.put(value.getName(), valueMeta);
        }

        meta.setValuesByName(values);
        meta.setValuesLoaded(true);
        publish(snapshot);
        saveToDisk(snapshot);
    }

    // ---------------------------------------------------------------------------------------
    // Disk cache
    // ---------------------------------------------------------------------------------------

    /** @return the cache file for a vault id, or {@code null} if the MDL directory is unavailable. */
    public File cacheFile(String vaultId) {
        ToolboxProject toolboxProject = ToolboxProject.getInstance(project);
        if (toolboxProject == null || vaultId == null) {
            return null;
        }
        File mdlDirectory = toolboxProject.getMdlDirectory();
        if (mdlDirectory == null) {
            return null;
        }
        return new File(new File(new File(mdlDirectory, vaultId), SCHEMA_DIR), CACHE_FILE);
    }

    /** Loads a cached snapshot from disk, or {@code null} if absent or incompatible. */
    public VaultMetadata loadFromDisk(String vaultId) {
        File file = cacheFile(vaultId);
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            VaultMetadata metadata = mapper().readValue(file, VaultMetadata.class);
            if (metadata == null || metadata.getSchemaVersion() != VaultMetadata.CURRENT_SCHEMA_VERSION) {
                return null;
            }
            return metadata;
        } catch (Exception e) {
            logger.warn("Failed to read metadata cache from {}", file, e);
            return null;
        }
    }

    /** Persists a snapshot to disk. Safe to call from the background thread only. */
    public void saveToDisk(VaultMetadata metadata) {
        if (metadata == null) {
            return;
        }
        File file = cacheFile(metadata.getVaultId());
        if (file == null) {
            return;
        }
        try {
            String json = mapper().configure(SerializationFeature.INDENT_OUTPUT, true)
                    .writeValueAsString(metadata);
            FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Failed to write metadata cache to {}", file, e);
        }
    }

    /**
     * Creates and configures a default {@link ObjectMapper} for JSON serialization and deserialization.
     *
     * @return the configured ObjectMapper instance
     */
    private static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    /** Re-runs highlighting and notifies UI listeners; coalesced onto a single EDT runnable. */
    private void onPublished() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            DaemonCodeAnalyzer daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
            VirtualFile[] openFiles = FileEditorManager.getInstance(project).getOpenFiles();
            PsiManager psiManager = PsiManager.getInstance(project);
            ApplicationManager.getApplication().runReadAction(() -> {
                for (VirtualFile vf : openFiles) {
                    PsiFile psiFile = psiManager.findFile(vf);
                    if (psiFile != null) {
                        restartDaemon(daemonCodeAnalyzer, psiFile);
                    }
                }
            });
            for (Runnable listener : changeListeners) {
                try {
                    listener.run();
                } catch (Exception e) {
                    logger.warn("Metadata change listener failed", e);
                }
            }
        });
    }

    /**
     * Restarts daemon highlighting for a single file, using the reason-aware overload
     * introduced in 2026.1 when available and falling back to the older single-argument
     * form on earlier runtimes. The reflective dispatch keeps the compiled bytecode free
     * of a direct reference to the deprecated {@code restart(PsiFile)} signature, which
     * would otherwise surface as a plugin-verifier warning against newer IDE versions.
     */
    private void restartDaemon(DaemonCodeAnalyzer analyzer, PsiFile psiFile) {
        try {
            analyzer.getClass()
                    .getMethod("restart", PsiFile.class, Object.class)
                    .invoke(analyzer, psiFile, "MetadataService");
        } catch (NoSuchMethodException e) {
            // restart(PsiFile, Object) was added in 2026.1; fall back to restart(PsiFile) on older runtimes
            try {
                analyzer.getClass()
                        .getMethod("restart", PsiFile.class)
                        .invoke(analyzer, psiFile);
            } catch (ReflectiveOperationException ex) {
                logger.warn("Failed to restart daemon for {}", psiFile.getName(), ex);
            }
        } catch (ReflectiveOperationException e) {
            logger.warn("Failed to restart daemon for {}", psiFile.getName(), e);
        }
    }
}
