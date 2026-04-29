package com.veeva.vault.toolbox.intellij.tasks;

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

import static com.veeva.vault.toolbox.core.utils.Checksum.getMd5;

public class ExtractSdkTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(ExtractSdkTask.class);
	private final VirtualFile virtualFile;
	private final List<String> oldFiles = new ArrayList<>();
	private final List<String> newFiles = new ArrayList<>();

	public ExtractSdkTask(@Nullable Project project) {
		super(project, "Extracting MDL from Vault");
		File extractDirectory = toolboxProject.getMdlDirectory();

		FileIO.makeDirectories(extractDirectory);
		this.virtualFile = VfsUtil.findFileByIoFile(extractDirectory, true);
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			toolboxProject.includeFile(virtualFile.getPath());

			loadOldFiles(virtualFile);
			downloadAllMdl(virtualFile, null);
			deleteMissingFiles();
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

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

	private void loadOldFiles(VirtualFile virtualFile) {
		if (virtualFile.getPath().endsWith(".mdl")) {
			oldFiles.add(virtualFile.getPath());
		}
		for (VirtualFile child : virtualFile.getChildren()) {
			loadOldFiles(child);
		}
	}

	private void downloadAllMdl(VirtualFile mdlDirectory, String nextPage) {
		try {
			ComponentQueryResponse queryResponse = null;
			if (nextPage == null) {
				String query = "SELECT label__v,component_name__v, component_type__v, status__v, mdl_definition__v FROM vault_component__v";
				queryResponse = toolboxProject.getVaultClient().newRequest(ConfigurationMigrationRequest.class).componentDefinitionQuery(query);
			}
			else {
				queryResponse = toolboxProject.getVaultClient().newRequest(ConfigurationMigrationRequest.class).componentDefinitionQueryByPage(nextPage);
			}

			if (queryResponse != null && !queryResponse.isFailure()) {
				queryResponse.getData().forEach(queryResult -> {
					try {
						String componentName = queryResult.getString("component_name__v");
						String componentType = queryResult.getString("component_type__v");
						String mdlDefinition = queryResult.getString("mdl_definition__v");

						File componentTypeDirectory = new File(mdlDirectory.getPath(), componentType);
						toolboxProject.includeFile(componentTypeDirectory.getPath());
						FileIO.makeDirectories(componentTypeDirectory);

						File componentRecordFile = new File(componentTypeDirectory, componentType + "." + componentName + ".mdl");
						FileUtils.writeStringToFile(componentRecordFile, mdlDefinition, "UTF-8");
						toolboxProject.includeFile(componentRecordFile.getAbsolutePath(), getMd5(mdlDefinition));
						newFiles.add(componentRecordFile.getAbsolutePath());
						//logger.debug("new file: " + componentRecordFile.getAbsolutePath());

					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				});

				if (queryResponse.isPaginated()) {
					downloadAllMdl(mdlDirectory, queryResponse.getResponseDetails().getNextPage());
				}
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void onSuccess() {
		super.onSuccess();
		try {
			if (toolboxProject != null) {
				Message message = toolboxProject.newMessage();
				message.setTitle("Vault MDL");
				message.append("Refresh Completed");
				message.showInformation();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
