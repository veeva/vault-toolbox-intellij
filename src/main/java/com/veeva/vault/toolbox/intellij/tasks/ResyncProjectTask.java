package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.ui.Message;
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

import static com.veeva.vault.toolbox.core.utils.Checksum.getMd5;

public class ResyncProjectTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(ResyncProjectTask.class);
	private final VirtualFile virtualFile;

	public ResyncProjectTask(@Nullable Project project,
							 @NotNull VirtualFile virtualFile) {
		super(project, "Resyncing Project");
		this.virtualFile = virtualFile;
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			String codePackageName = "com.veeva.vault.custom";
			if (virtualFile != null) {
				codePackageName = virtualFile.getPath()
						.replace(".javax", "")
						.replace("/", ".")
						.replace("\\", ".");
				codePackageName = codePackageName.substring(codePackageName.lastIndexOf("com.veeva.vault.custom"));
			}
			syncSdk(codePackageName, null);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void syncSdk(String codePackageName, String nextPage) {
		try {
			ComponentQueryResponse queryResponse = null;
			if (nextPage == null) {
				String query = "SELECT label__v,component_name__v, component_type__v, status__v, mdl_definition__v FROM vault_component__v" +
						" WHERE component_name__v LIKE '" + codePackageName + "%'";
				queryResponse = toolboxProject.getVaultClient().newRequest(ConfigurationMigrationRequest.class).componentDefinitionQuery(query);
			}
			else {
				queryResponse = toolboxProject.getVaultClient().newRequest(ConfigurationMigrationRequest.class).componentDefinitionQueryByPage(nextPage);
			}

			if (queryResponse != null && queryResponse.getData() != null) {
				queryResponse.getData().forEach(queryResult -> {
					{
						try {
							String componentName = queryResult.getString("component_name__v");

							String codepath = componentName.replace(".", "/");
							File localFile = new File(toolboxProject.getProject().getBasePath(), "/src/main/java/" + codepath + ".java");

							SDKResponse sdkResponse = toolboxProject.getVaultClient().newRequest(SDKRequest.class)
									.retrieveSingleSourceCodeFile(componentName);
							if (sdkResponse != null) {
								String fileContent = new String(sdkResponse.getBinaryContent());
								String remoteMd5 = getMd5(fileContent);
								if (!localFile.exists()) {
									FileUtils.writeStringToFile(localFile, fileContent, "UTF-8");
								}
								toolboxProject.includeFile(localFile.getAbsolutePath(), remoteMd5);
							}
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
						}
					}
				});

				if (queryResponse.isPaginated()) {
					syncSdk(codePackageName, queryResponse.getResponseDetails().getNextPage());
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
				message.setTitle("Resync");
				message.append("Resync Completed");
				message.showInformation();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
