package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.veeva.vault.toolbox.intellij.services.Deploy;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.MetaDataRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Drops an MDL component referenced by an MDL file. Parses the file's RECREATE/ALTER
 * command to derive the component type and name, then executes the corresponding DROP
 * statement. On success, the file is removed from the project's tracked files.
 */
public class DropMdlTask extends ToolboxTask {
	/**
	 * Logger for this class.
	 */
	private static final Logger logger = LoggerFactory.getLogger(DropMdlTask.class);

	/**
	 * Pattern to match component type statements.
	 */
	private static final Pattern COMPONENT_TYPE_PATTERN = Pattern.compile(
			"(?:RECREATE|ALTER|CREATE)\\s+Componenttype\\s+([A-Z][a-z0-9]*)",
			Pattern.CASE_INSENSITIVE
	);

	/**
	 * Pattern to match record statements.
	 */
	private static final Pattern RECORD_PATTERN = Pattern.compile(
			"(?:RECREATE|ALTER|CREATE)\\s+([A-Z][a-z0-9]*)\\s+(?:IF\\s+(?:NOT\\s+)?EXISTS\\s+)?([a-z][a-z0-9_.]*(?:__[a-z]+)+)"
	);

	/**
	 * The PSI file corresponding to the MDL script.
	 */
	private final PsiFile psiFile;

	/**
	 * The response received from Vault.
	 */
	private VaultResponse vaultResponse;

	/**
	 * Constructs a new task to drop an MDL component.
	 *
	 * @param project the IntelliJ project, may be {@code null}
	 * @param psiFile the MDL file describing the component to drop
	 */
	public DropMdlTask(@Nullable Project project, @NotNull PsiFile psiFile) {
		super(project, "Dropping MDL");
		this.psiFile = psiFile;
	}

	/**
	 * Parses the MDL file text to build a DROP statement, executes it, and removes the
	 * file from the project's tracked files on success.
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
				return;
			}

			String fileContent = psiFile.getText();
			String dropStatement = buildDropStatement(fileContent);

			if (dropStatement == null) {
				Message message = toolboxProject.newMessage();
				message.append("Could not determine component to drop from: " + psiFile.getName());
				message.showError();
				return;
			}

			logger.debug("drop statement = " + dropStatement);
			vaultResponse = toolboxProject.getVaultClient().newRequest(MetaDataRequest.class)
					.setRequestString(dropStatement)
					.executeMDLScript();

			if (vaultResponse != null && vaultResponse.isFailure()) {
				if (toolboxProject.handleSessionExpiration(vaultResponse)) {
					vaultResponse = null;
					return;
				}
			}

			if (vaultResponse != null && !vaultResponse.isFailure()) {
				String localPath = psiFile.getVirtualFile().getPath();
				toolboxProject.removeFile(localPath);
				File localFile = new File(localPath);
				if (localFile.exists()) {
					localFile.delete();
				}
			}
			if (vaultResponse != null) {
				logger.debug("drop results = " + vaultResponse.getResponseStatus());
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Builds a DROP MDL statement by parsing the first change command in the file text.
	 * <ul>
	 *   <li>{@code RECREATE Componenttype Object (...)} → {@code DROP Componenttype Object;}</li>
	 *   <li>{@code RECREATE Picklist foo__c (...)} → {@code DROP Picklist foo__c;}</li>
	 * </ul>
	 *
	 * @param fileContent the raw MDL file text
	 * @return the DROP statement, or {@code null} if no recognisable change command is found
	 */
	@Nullable
	private String buildDropStatement(String fileContent) {
		Matcher componentMatcher = COMPONENT_TYPE_PATTERN.matcher(fileContent);
		if (componentMatcher.find()) {
			return "DROP Componenttype " + componentMatcher.group(1) + ";";
		}

		Matcher recordMatcher = RECORD_PATTERN.matcher(fileContent);
		if (recordMatcher.find()) {
			return "DROP " + recordMatcher.group(1) + " " + recordMatcher.group(2) + ";";
		}

		logger.warn("Could not extract component info from MDL file: {}", psiFile.getName());
		return null;
	}

	/**
	 * Displays the results of the drop operation in a UI message on the EDT.
	 */
	@Override
	public void onSuccess() {
		super.onSuccess();
		try {
			if (toolboxProject != null && vaultResponse != null) {
				Message message = toolboxProject.newMessage();
				message.setTitle("Drop: " + psiFile.getName());
				Deploy.showResults(vaultResponse, message);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
