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

/**
 * Re-syncs SDK source files for the supplied package between the connected vault and
 * the local workspace. New files are created locally and every file's checksum is
 * registered with the toolbox project so it can detect drift afterwards.
 */
public class ResyncProjectTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(ResyncProjectTask.class);
	private static final String DEFAULT_PACKAGE = "com.veeva.vault.custom";

	private final VirtualFile virtualFile;

	/**
	 * @param project     the IntelliJ project, may be {@code null}
	 * @param virtualFile the SDK source root or file selected for resync
	 */
	public ResyncProjectTask(@Nullable Project project, @NotNull VirtualFile virtualFile) {
		super(project, "Resyncing Project");
		this.virtualFile = virtualFile;
	}

	/**
	 * Orchestrates the project re-sync process in a background thread.
	 *
	 * @param indicator the progress indicator for the background task
	 */
	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			String codePackageName = DEFAULT_PACKAGE;
			if (virtualFile != null) {
				codePackageName = virtualFile.getPath()
						.replace(".javax", "")
						.replace("/", ".")
						.replace("\\", ".");
				codePackageName = codePackageName.substring(codePackageName.lastIndexOf(DEFAULT_PACKAGE));
			}
			syncSdk(codePackageName, null);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Recursively pages through SDK component definitions whose names start with the
	 * given package, retrieves each source file, and writes it locally if it doesn't
	 * already exist.
	 *
	 * @param codePackageName the package prefix to match against {@code component_name__v}
	 * @param nextPage        the API pagination token, or {@code null} for the first page
	 */
	public void syncSdk(String codePackageName, String nextPage) {
		try {
			ComponentQueryResponse queryResponse;
			if (nextPage == null) {
				String query = "SELECT label__v,component_name__v, component_type__v, status__v, mdl_definition__v FROM vault_component__v" +
						" WHERE component_name__v LIKE '" + codePackageName + "%'";
				queryResponse = toolboxProject.getVaultClient().newRequest(ConfigurationMigrationRequest.class)
						.componentDefinitionQuery(query);
			}
			else {
				queryResponse = toolboxProject.getVaultClient().newRequest(ConfigurationMigrationRequest.class)
						.componentDefinitionQueryByPage(nextPage);
			}

			if (queryResponse == null || queryResponse.getData() == null) {
				return;
			}

			queryResponse.getData().forEach(queryResult -> {
				try {
					String componentName = queryResult.getString("component_name__v");
					String codepath = componentName.replace(".", "/");
					File localFile = new File(toolboxProject.getProject().getBasePath(), "/src/main/java/" + codepath + ".java");

					SDKResponse sdkResponse = toolboxProject.getVaultClient().newRequest(SDKRequest.class)
							.retrieveSingleSourceCodeFile(componentName);
					if (sdkResponse == null) {
						return;
					}
					String fileContent = new String(sdkResponse.getBinaryContent());
					String remoteMd5 = getMd5(fileContent);
					if (!localFile.exists()) {
						FileUtils.writeStringToFile(localFile, fileContent, "UTF-8");
					}
					toolboxProject.includeFile(localFile.getAbsolutePath(), remoteMd5);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			});

			if (queryResponse.isPaginated()) {
				syncSdk(codePackageName, queryResponse.getResponseDetails().getNextPage());
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Displays a success message in the UI on the EDT.
	 */
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
