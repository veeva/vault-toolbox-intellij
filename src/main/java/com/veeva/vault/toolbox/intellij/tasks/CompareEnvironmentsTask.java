package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.ide.diff.DirDiffSettings;
import com.intellij.ide.diff.VirtualFileDiffElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diff.DirDiffManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.settings.AppSettings;
import com.veeva.vault.toolbox.intellij.ui.CompareEnvironmentsDialog.ComparisonType;
import com.veeva.vault.toolbox.intellij.ui.CompareEnvironmentsDialog.MdlFilter;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.response.ComponentQueryResponse;
import com.veeva.vault.vapil.api.model.response.SDKResponse;
import com.veeva.vault.vapil.api.request.ConfigurationMigrationRequest;
import com.veeva.vault.vapil.api.request.SDKRequest;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Background task that downloads the selected content types (MDL and/or SDK) from two Vault
 * environments into temporary directories and opens IntelliJ's directory diff viewer for each.
 * The MDL download respects the {@link MdlFilter} so only custom, standard, or all components
 * are written depending on the user's selection.
 */
public class CompareEnvironmentsTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(CompareEnvironmentsTask.class);
    private static final String MDL_QUERY =
            "SELECT component_name__v, component_type__v, mdl_definition__v FROM vault_component__v";
    private static final String SDK_QUERY =
            "SELECT component_name__v FROM vault_component__v" +
            " WHERE component_name__v LIKE 'com.veeva.vault.custom%'";

    private final Set<ComparisonType> comparisonTypes;
    private final MdlFilter mdlFilter;
    private final String sourceDns;
    private final VaultClient sourceClient;
    private final String targetDns;
    private final VaultClient targetClient;

    private final Map<ComparisonType, File> sourceDirs = new HashMap<>();
    private final Map<ComparisonType, File> targetDirs = new HashMap<>();

    @Nullable
    private final Consumer<Map<ComparisonType, File[]>> resultsCallback;

    @Nullable
    private final Runnable cancelCallback;

    private volatile File taskBaseDir;

    /**
     * Constructs a new CompareEnvironmentsTask.
     *
     * @param project         the IDE project
     * @param comparisonTypes the types of comparisons to perform
     * @param mdlFilter       the MDL filter
     * @param sourceDns       the DNS of the source Vault
     * @param sourceClient    the Vault API client for the source Vault
     * @param targetDns       the DNS of the target Vault
     * @param targetClient    the Vault API client for the target Vault
     * @param resultsCallback a callback to receive the comparison results
     * @param cancelCallback  a callback invoked on the EDT when the task is cancelled
     */
    public CompareEnvironmentsTask(
            @Nullable Project project,
            Set<ComparisonType> comparisonTypes,
            MdlFilter mdlFilter,
            String sourceDns,
            VaultClient sourceClient,
            String targetDns,
            VaultClient targetClient,
            @Nullable Consumer<Map<ComparisonType, File[]>> resultsCallback,
            @Nullable Runnable cancelCallback) {
        super(project, "Comparing Vault Environments", true);
        this.comparisonTypes = comparisonTypes;
        this.mdlFilter = mdlFilter;
        this.sourceDns = sourceDns;
        this.sourceClient = sourceClient;
        this.targetDns = targetDns;
        this.targetClient = targetClient;
        this.resultsCallback = resultsCallback;
        this.cancelCallback = cancelCallback;
    }

    /**
     * Executes the task to download and compare environments.
     *
     * @param indicator the progress indicator
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            if (AppSettings.requireRestart) {
                showError("IntelliJ requires a restart before a new Vault connection can be established.");
                return;
            }

            indicator.setIndeterminate(true);
            indicator.checkCanceled();

            String diffId = String.valueOf(Instant.now().toEpochMilli());
            File baseDir = new File(System.getProperty("java.io.tmpdir"), "vault-toolbox-diff/" + diffId);
            taskBaseDir = baseDir;

            for (ComparisonType type : comparisonTypes) {
                indicator.checkCanceled();
                File typeBase = new File(baseDir, type.name().toLowerCase());
                File srcDir = new File(typeBase, sanitizeForPath(sourceDns));
                File tgtDir = new File(typeBase, sanitizeForPath(targetDns));
                FileIO.makeDirectories(srcDir);
                FileIO.makeDirectories(tgtDir);

                switch (type) {
                    case MDL -> {
                        String filterDesc = mdlFilter == MdlFilter.ALL ? "" : " [" + mdlFilter + "]";
                        downloadMdl(sourceClient, srcDir, indicator,
                                "MDL source" + filterDesc + " (" + sourceDns + ")");
                        indicator.checkCanceled();
                        downloadMdl(targetClient, tgtDir, indicator,
                                "MDL target" + filterDesc + " (" + targetDns + ")");
                        
                        indicator.setText("Analyzing MDL differences...");
                        downloadJsonForDifferences(sourceClient, targetClient, srcDir, tgtDir, indicator);
                    }
                    case SDK -> {
                        downloadSdk(sourceClient, srcDir, indicator,
                                "SDK source (" + sourceDns + ")");
                        indicator.checkCanceled();
                        downloadSdk(targetClient, tgtDir, indicator,
                                "SDK target (" + targetDns + ")");
                    }
                }

                sourceDirs.put(type, srcDir);
                targetDirs.put(type, tgtDir);
            }
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Compare environments task failed", e);
        }
    }

    /**
     * Called when the background task successfully completes.
     * Triggers the diff viewer or callback.
     */
    @Override
    public void onSuccess() {
        super.onSuccess();

        if (resultsCallback != null) {
            if (sourceDirs.isEmpty()) {
                cleanupTaskBaseDir();
                resultsCallback.accept(Collections.emptyMap());
                return;
            }
            Map<ComparisonType, File[]> results = new HashMap<>();
            for (ComparisonType type : comparisonTypes) {
                File src = sourceDirs.get(type);
                File tgt = targetDirs.get(type);
                if (src != null && tgt != null) {
                    results.put(type, new File[]{src, tgt});
                }
            }
            resultsCallback.accept(results);
        } else {
            if (sourceDirs.isEmpty()) return;
            ApplicationManager.getApplication().invokeLater(() -> {
                for (ComparisonType type : comparisonTypes) {
                    File srcDir = sourceDirs.get(type);
                    File tgtDir = targetDirs.get(type);
                    if (srcDir == null || tgtDir == null) continue;
                    try {
                        VirtualFile srcVFile = LocalFileSystem.getInstance()
                                .refreshAndFindFileByPath(srcDir.getAbsolutePath());
                        VirtualFile tgtVFile = LocalFileSystem.getInstance()
                                .refreshAndFindFileByPath(tgtDir.getAbsolutePath());
                        if (srcVFile == null || tgtVFile == null) {
                            showError("Could not resolve comparison directories for " + type + ".");
                            continue;
                        }
                        srcVFile.refresh(false, true);
                        tgtVFile.refresh(false, true);
                        DirDiffManager.getInstance(getProject()).showDiff(
                                new VirtualFileDiffElement(srcVFile),
                                new VirtualFileDiffElement(tgtVFile),
                                new DirDiffSettings());
                    } catch (Exception e) {
                        logger.error("Failed to open diff viewer for " + type, e);
                        showError("Failed to open diff viewer for " + type + ": " + e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * Called when the task is cancelled. Cleans up any partially written temp files
     * off the EDT, then notifies the panel so it can restore its UI state.
     */
    @Override
    public void onCancel() {
        super.onCancel();
        cleanupTaskBaseDir();
        if (cancelCallback != null) {
            ApplicationManager.getApplication().invokeLater(cancelCallback);
        }
    }

    /**
     * Deletes the task's temp base directory on a pooled thread to avoid blocking the EDT.
     */
    private void cleanupTaskBaseDir() {
        File dir = taskBaseDir;
        if (dir == null) return;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                logger.warn("Could not delete temp comparison dir: {}", dir, e);
            }
        });
    }

    /**
     * Downloads MDL components from the specified Vault client.
     *
     * @param client    the Vault API client
     * @param dir       the directory to save downloaded MDL components
     * @param indicator the progress indicator
     * @param label     the label for progress updates
     * @throws Exception if an error occurs during download
     */
    private void downloadMdl(VaultClient client, File dir, ProgressIndicator indicator, String label) throws Exception {
        String nextPage = null;
        int page = 1;
        do {
            indicator.checkCanceled();
            indicator.setIndeterminate(true);
            indicator.setText(label + ": Downloading MDL (page " + page + ")...");

            ComponentQueryResponse response = (nextPage == null)
                    ? client.newRequest(ConfigurationMigrationRequest.class).componentDefinitionQuery(MDL_QUERY)
                    : client.newRequest(ConfigurationMigrationRequest.class).componentDefinitionQueryByPage(nextPage);

            if (response == null || response.isFailure()) {
                logger.warn("MDL query failed for: " + label);
                break;
            }

            for (var row : response.getData()) {
                try {
                    String name = row.getString("component_name__v");
                    String type = row.getString("component_type__v");
                    String mdl  = row.getString("mdl_definition__v");

                    if (mdl == null || "N/A".equals(mdl)) continue;
                    if (!mdlFilter.accepts(name)) continue;

                    File typeDir = new File(dir, type);
                    FileIO.makeDirectories(typeDir);
                    FileUtils.writeStringToFile(new File(typeDir, type + "." + name + ".mdl"), mdl, "UTF-8");
                } catch (Exception e) {
                    logger.error("Failed to write MDL file", e);
                }
            }

            boolean hasMore = response.isPaginated()
                    && response.getResponseDetails() != null
                    && response.getResponseDetails().getNextPage() != null;
            nextPage = hasMore ? response.getResponseDetails().getNextPage() : null;
            page++;
        } while (nextPage != null);
    }

    /**
     * Downloads SDK components from the specified Vault client.
     *
     * @param client    the Vault API client
     * @param dir       the directory to save downloaded SDK components
     * @param indicator the progress indicator
     * @param label     the label for progress updates
     * @throws Exception if an error occurs during download
     */
    private void downloadSdk(VaultClient client, File dir, ProgressIndicator indicator, String label) throws Exception {
        indicator.setIndeterminate(true);
        indicator.setText(label + ": Discovering SDK components...");

        List<String> componentNames = new ArrayList<>();
        String nextPage = null;
        do {
            indicator.checkCanceled();
            ComponentQueryResponse response = (nextPage == null)
                    ? client.newRequest(ConfigurationMigrationRequest.class).componentDefinitionQuery(SDK_QUERY)
                    : client.newRequest(ConfigurationMigrationRequest.class).componentDefinitionQueryByPage(nextPage);

            if (response == null || response.isFailure()) break;
            response.getData().forEach(row -> componentNames.add(row.getString("component_name__v")));

            boolean hasMore = response.isPaginated()
                    && response.getResponseDetails() != null
                    && response.getResponseDetails().getNextPage() != null;
            nextPage = hasMore ? response.getResponseDetails().getNextPage() : null;
        } while (nextPage != null);

        int total = componentNames.size();
        indicator.setIndeterminate(false);
        for (int i = 0; i < total; i++) {
            indicator.checkCanceled();
            String componentName = componentNames.get(i);
            indicator.setFraction((double) i / total);
            indicator.setText(label + ": Downloading " + componentName + " (" + (i + 1) + "/" + total + ")");

            try {
                SDKResponse sdkResponse = client.newRequest(SDKRequest.class)
                        .retrieveSingleSourceCodeFile(componentName);
                if (sdkResponse == null || sdkResponse.getBinaryContent() == null) continue;

                File localFile = new File(dir, componentName.replace(".", "/") + ".java");
                FileIO.makeDirectories(localFile.getParentFile());
                FileUtils.writeStringToFile(localFile, new String(sdkResponse.getBinaryContent()), "UTF-8");
            } catch (Exception e) {
                logger.error("Failed to download SDK file: " + componentName, e);
            }
        }
        indicator.setFraction(1.0);
    }

    private void downloadJsonForDifferences(VaultClient sourceClient, VaultClient targetClient, File srcDir, File tgtDir, ProgressIndicator indicator) {
        if (!srcDir.exists() || !tgtDir.exists()) return;

        List<File> srcFiles = new ArrayList<>(FileUtils.listFiles(srcDir, new String[]{"mdl"}, true));
        int total = srcFiles.size();
        for (int i = 0; i < total; i++) {
            indicator.checkCanceled();
            File srcFile = srcFiles.get(i);
            String rel = srcDir.toURI().relativize(srcFile.toURI()).getPath();
            File tgtFile = new File(tgtDir, rel);

            if (tgtFile.exists()) {
                try {
                    if (!FileUtils.contentEquals(srcFile, tgtFile)) {
                        String[] parts = parseParts(rel);
                        if (parts != null) {
                            String type = parts[0];
                            String name = parts[1];

                            indicator.setText("Analyzing semantic diff for " + name + " (" + (i + 1) + "/" + total + ")");

                            File srcJsonFile = new File(srcFile.getParentFile(), srcFile.getName() + ".json");
                            if (srcJsonFile.exists()) srcJsonFile.delete();
                            String srcJson = fetchComponentJson(sourceClient, type, name);
                            if (srcJson != null) {
                                FileUtils.writeStringToFile(srcJsonFile, srcJson, "UTF-8");
                            }

                            File tgtJsonFile = new File(tgtFile.getParentFile(), tgtFile.getName() + ".json");
                            if (tgtJsonFile.exists()) tgtJsonFile.delete();
                            String tgtJson = fetchComponentJson(targetClient, type, name);
                            if (tgtJson != null) {
                                FileUtils.writeStringToFile(tgtJsonFile, tgtJson, "UTF-8");
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error fetching JSON for " + rel, e);
                }
            }
        }
    }

    private String fetchComponentJson(VaultClient client, String type, String name) {
        try {
            com.veeva.vault.vapil.api.request.MetaDataRequest req = client.newRequest(com.veeva.vault.vapil.api.request.MetaDataRequest.class);
            req.setAcceptJSON();
            com.veeva.vault.vapil.api.model.response.MetaDataComponentRecordResponse response = req.retrieveComponentRecordXmlJson(type, name);
            if (response != null && !response.isFailure() && response.getData() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.writeValueAsString(response.getData());
            }
        } catch (Exception e) {
            logger.error("Failed to fetch JSON for " + type + " " + name, e);
        }
        return null;
    }

    /**
     * Parses the relative path to extract component type and name.
     *
     * @param relative the relative file path
     * @return an array containing type and name, or null if parsing fails
     */
    private String[] parseParts(String relative) {
        int slash = relative.indexOf('/');
        if (slash > 0) {
            String type = relative.substring(0, slash);
            String fileName = relative.substring(slash + 1);
            String name = fileName.endsWith(".mdl")
                    ? fileName.substring(0, fileName.length() - 4) : fileName;
            int dot = name.indexOf('.');
            if(dot > 0 && name.startsWith(type + ".")) {
                 name = name.substring(dot + 1);
            }
            return new String[]{type, name};
        }
        return null;
    }

    /**
     * Displays an error message to the user.
     *
     * @param message the error message to display
     */
    private void showError(String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (toolboxProject != null) {
                Message msg = toolboxProject.newMessage();
                msg.setTitle("Compare Environments");
                msg.append(message);
                msg.showError();
            }
        });
    }

    /**
     * Sanitizes a DNS string to be used as a directory or file name.
     *
     * @param dns the DNS string
     * @return the sanitized string
     */
    private static String sanitizeForPath(String dns) {
        if (dns == null) return "unknown";
        return dns.replaceAll("[^a-zA-Z0-9._-]", "-");
    }
}
