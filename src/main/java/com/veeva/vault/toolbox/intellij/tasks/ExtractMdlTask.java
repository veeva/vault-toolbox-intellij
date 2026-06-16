package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.vapil.api.model.response.ComponentQueryResponse;
import com.veeva.vault.vapil.api.request.ConfigurationMigrationRequest;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts MDL definitions for every Vault component into a per-vault subdirectory of
 * the project's MDL folder. Local files that no longer have a remote counterpart are
 * deleted so the local view mirrors what is currently in the vault.
 */
public class ExtractMdlTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(ExtractMdlTask.class);
    private static final String COMPONENT_QUERY =
            "SELECT label__v,component_name__v, component_type__v, status__v, mdl_definition__v FROM vault_component__v";

    private final VirtualFile virtualFile;
    private final List<String> oldFiles = new ArrayList<>();
    private final List<String> newFiles = new ArrayList<>();
    private boolean success = true;

    /**
     * @param project the IntelliJ project, may be {@code null}
     */
    public ExtractMdlTask(@Nullable Project project) {
        super(project, "Extracting MDL from Vault", true);

        String vaultId = toolboxProject.getVaultId().toString();
        File extractDirectory = new File(toolboxProject.getMdlDirectory(), vaultId);

        FileIO.makeDirectories(extractDirectory);
        this.virtualFile = VfsUtil.findFileByIoFile(extractDirectory, true);
    }

    /**
     * Performs the full MDL extraction process: listing old files, downloading new ones,
     * and cleaning up orphans.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            if (toolboxProject.isProductionVault()) {
                Message message = toolboxProject.newMessage();
                message.append("This tool cannot be run in a Production domain.");
                message.showError();
                success = false;
                return;
            }
            if (virtualFile == null) {
                logger.error("Could not resolve virtual file for directory: " + toolboxProject.getMdlDirectory());
                success = false;
                return;
            }
            toolboxProject.includeFile(virtualFile.getPath());
            loadOldFiles(virtualFile);
            if (!downloadAllMdl(virtualFile, indicator)) {
                success = false;
                return;
            }
            deleteMissingFiles();
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            success = false;
        }
    }

    /**
     * Removes local MDL files that were not found in the recent vault extraction.
     */
    private void deleteMissingFiles() {
        for (String oldFile : oldFiles) {
            logger.debug("old file: " + oldFile);
            if (!newFiles.contains(oldFile)) {
                File file = new File(oldFile);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Scans the extraction directory recursively to build a list of existing MDL files.
     *
     * @param virtualFile the directory or file to scan
     */
    private void loadOldFiles(VirtualFile virtualFile) {
        if (virtualFile.getPath().endsWith(".mdl")) {
            oldFiles.add(virtualFile.getPath());
        }
        for (VirtualFile child : virtualFile.getChildren()) {
            loadOldFiles(child);
        }
    }

    /**
     * Paginates through {@code vault_component__v} records, writing each
     * MDL definition to a file under the appropriate component-type subfolder.
     *
     * @param mdlDirectory the destination root for the extracted MDL files
     * @param indicator    the progress indicator
     */
    private boolean downloadAllMdl(VirtualFile mdlDirectory, ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        int page = 1;
        String nextPage = null;
        do {
            indicator.checkCanceled();
            indicator.setText("Extracting MDL components (Page " + page + ")...");
            
            try {
                ComponentQueryResponse queryResponse;
                if (nextPage == null) {
                    queryResponse = toolboxProject.getVaultClient().newRequest(ConfigurationMigrationRequest.class)
                            .componentDefinitionQuery(COMPONENT_QUERY);
                }
                else {
                    queryResponse = toolboxProject.getVaultClient().newRequest(ConfigurationMigrationRequest.class)
                            .componentDefinitionQueryByPage(nextPage);
                }

                if (queryResponse == null || queryResponse.isFailure()) {
                    if (queryResponse != null) {
                        if (!toolboxProject.handleSessionExpiration(queryResponse)) {
                            Message message = toolboxProject.newMessage();
                            message.setTitle("Extract Failed");
                            String error = queryResponse.getResponseMessage();
                            if ((error == null || error.isEmpty()) && queryResponse.getErrors() != null && !queryResponse.getErrors().isEmpty()) {
                                error = queryResponse.getErrors().get(0).getMessage();
                            }
                            message.append(error != null && !error.isEmpty() ? error : "Failed to extract MDL.");
                            message.showError();
                        }
                    } else {
                        Message message = toolboxProject.newMessage();
                        message.setTitle("Extract Failed");
                        message.append("No response received from Vault.");
                        message.showError();
                    }
                    return false;
                }

                queryResponse.getData().forEach(queryResult -> {
                    try {
                        String componentName = queryResult.getString("component_name__v");
                        String componentType = queryResult.getString("component_type__v");
                        String mdlDefinition = queryResult.getString("mdl_definition__v");

                        if ("N/A".equals(mdlDefinition)) {
                            return;
                        }

                        File componentTypeDirectory = new File(mdlDirectory.getPath(), componentType);
                        FileIO.makeDirectories(componentTypeDirectory);

                        File componentRecordFile = new File(componentTypeDirectory, componentType + "." + componentName + ".mdl");
                        FileUtils.writeStringToFile(componentRecordFile, mdlDefinition, "UTF-8");
                        newFiles.add(componentRecordFile.getAbsolutePath());
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                });

                if (queryResponse.isPaginated() && queryResponse.getResponseDetails().getNextPage() != null) {
                    nextPage = queryResponse.getResponseDetails().getNextPage();
                    page++;
                } else {
                    nextPage = null;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        } while (nextPage != null);
        return true;
    }

    /**
     * Notifies completion and refreshes the extraction folder in the UI.
     */
    @Override
    public void onSuccess() {
        super.onSuccess();
        if (!success) {
            return;
        }
        try {
            if (toolboxProject != null) {
                Message message = toolboxProject.newMessage();
                message.setTitle("Vault MDL");
                message.append("Extract Completed");
                message.showInformation();

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (virtualFile != null) {
                        virtualFile.refresh(false, true);
                        selectInProjectView(virtualFile);
                    }
                });
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
