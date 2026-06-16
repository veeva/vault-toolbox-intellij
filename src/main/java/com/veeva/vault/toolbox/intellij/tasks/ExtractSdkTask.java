package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.utils.FileIO;
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
import java.util.ArrayList;
import java.util.List;

import static com.veeva.vault.toolbox.core.utils.Checksum.getMd5;

/**
 * Extracts all Java SDK source files from the connected Vault into the project's
 * toolbox SDK folder. Local files that no longer have a remote counterpart are
 * deleted so the local view mirrors what is in the vault.
 */
public class ExtractSdkTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(ExtractSdkTask.class);
	private static final String SDK_PACKAGE_PREFIX = "com.veeva.vault.custom";
	private static final String COMPONENT_QUERY =
			"SELECT component_name__v FROM vault_component__v" +
			" WHERE component_name__v LIKE '" + SDK_PACKAGE_PREFIX + "%'";

	private final File sdkDirectory;
	private final VirtualFile virtualFile;
	private final List<String> oldFiles = new ArrayList<>();
	private final List<String> newFiles = new ArrayList<>();
	private boolean success = true;

	/**
	 * @param project the IntelliJ project, may be {@code null}
	 */
	public ExtractSdkTask(@Nullable Project project) {
		super(project, "Extracting SDK from Vault", true);
		this.sdkDirectory = new File(new File(toolboxProject.getToolboxDirectory(), "sdk"), String.valueOf(toolboxProject.getVaultId()));
		FileIO.makeDirectories(sdkDirectory);
		this.virtualFile = VfsUtil.findFileByIoFile(sdkDirectory, true);
	}

	/**
	 * Orchestrates the full SDK extraction process in a background thread.
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
				logger.error("Could not resolve virtual file for SDK directory: " + sdkDirectory);
				success = false;
				return;
			}
			toolboxProject.includeFile(virtualFile.getPath());
			loadOldFiles(virtualFile);
			
			List<String> componentNames = discoverSdkComponents(indicator);
			if (componentNames == null) {
				success = false;
				return;
			}
			downloadSdkFiles(componentNames, indicator);
			if (!success) {
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
	 * Scans the SDK extraction directory recursively to build a list of existing Java source files.
	 *
	 * @param vf the directory or file to scan
	 */
	private void loadOldFiles(VirtualFile vf) {
		if (vf.getPath().endsWith(".java")) {
			oldFiles.add(vf.getPath());
		}
		for (VirtualFile child : vf.getChildren()) {
			loadOldFiles(child);
		}
	}

	/**
	 * Removes local SDK source files that were not found in the recent vault extraction.
	 */
	private void deleteMissingFiles() {
		for (String oldFile : oldFiles) {
			if (!newFiles.contains(oldFile)) {
				File file = new File(oldFile);
				if (file.exists()) {
					file.delete();
				}
			}
		}
	}

	/**
	 * Pages through SDK component definitions to retrieve a list of all SDK source file components.
	 *
	 * @param indicator the progress indicator
	 * @return a list of component names, or null if an error occurred
	 * @throws Exception if an error occurs during API requests
	 */
	private List<String> discoverSdkComponents(ProgressIndicator indicator) throws Exception {
		indicator.setIndeterminate(true);
		indicator.setText("Discovering SDK components...");
		List<String> componentNames = new ArrayList<>();
		String nextPage = null;
		do {
			indicator.checkCanceled();
			ComponentQueryResponse queryResponse;
			if (nextPage == null) {
				queryResponse = toolboxProject.getVaultClient().newRequest(ConfigurationMigrationRequest.class)
						.componentDefinitionQuery(COMPONENT_QUERY);
			} else {
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
						message.append(error != null && !error.isEmpty() ? error : "Failed to extract SDK.");
						message.showError();
					}
				} else {
					Message message = toolboxProject.newMessage();
					message.setTitle("Extract Failed");
					message.append("No response received from Vault.");
					message.showError();
				}
				return null;
			}

			queryResponse.getData().forEach(queryResult -> {
				componentNames.add(queryResult.getString("component_name__v"));
			});

			if (queryResponse.isPaginated() && queryResponse.getResponseDetails().getNextPage() != null) {
				nextPage = queryResponse.getResponseDetails().getNextPage();
			} else {
				nextPage = null;
			}
		} while (nextPage != null);
		return componentNames;
	}

	/**
	 * Retrieves each source file, writes it to the local Java source tree,
	 * and registers its checksum with the project. Updates the progress indicator deterministically.
	 *
	 * @param componentNames the list of component names to download
	 * @param indicator the progress indicator
	 */
	private void downloadSdkFiles(List<String> componentNames, ProgressIndicator indicator) {
		indicator.setIndeterminate(false);
		int total = componentNames.size();
		for (int i = 0; i < total; i++) {
			indicator.checkCanceled();
			String componentName = componentNames.get(i);
			indicator.setFraction((double) i / total);
			indicator.setText("Downloading " + componentName + " (" + (i + 1) + "/" + total + ")");

			try {
				String codePath = componentName.replace(".", "/");
				File localFile = new File(sdkDirectory, codePath + ".java");

				SDKResponse sdkResponse = toolboxProject.getVaultClient().newRequest(SDKRequest.class)
						.retrieveSingleSourceCodeFile(componentName);
				if (sdkResponse == null || sdkResponse.isFailure()) {
					if (sdkResponse != null) {
						if (!toolboxProject.handleSessionExpiration(sdkResponse)) {
							Message message = toolboxProject.newMessage();
							message.setTitle("Extract Failed");
							String error = sdkResponse.getResponseMessage();
							if ((error == null || error.isEmpty()) && sdkResponse.getErrors() != null && !sdkResponse.getErrors().isEmpty()) {
								error = sdkResponse.getErrors().get(0).getMessage();
							}
							message.append(error != null && !error.isEmpty() ? error : "Failed to extract SDK component: " + componentName);
							message.showError();
						}
					}
					success = false;
					return;
				}
				if (sdkResponse.getBinaryContent() == null) {
					continue;
				}

				String fileContent = new String(sdkResponse.getBinaryContent());
				FileIO.makeDirectories(localFile.getParentFile());
				toolboxProject.includeFile(localFile.getParentFile().getAbsolutePath());
				FileUtils.writeStringToFile(localFile, fileContent, "UTF-8");
				toolboxProject.includeFile(localFile.getAbsolutePath(), getMd5(fileContent));
				newFiles.add(localFile.getAbsolutePath());
			} catch (Exception e) {
				logger.error("Failed to download SDK file: " + componentName, e);
				success = false;
				return;
			}
		}
		indicator.setFraction(1.0);
	}

	/**
	 * Notifies completion and refreshes the source root in the UI.
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
				message.setTitle("Vault SDK");
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
