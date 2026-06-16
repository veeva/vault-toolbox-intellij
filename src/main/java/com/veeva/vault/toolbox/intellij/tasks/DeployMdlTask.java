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

import static com.veeva.vault.toolbox.core.utils.Checksum.getMd5;

/**
 * Deploys an MDL file to the connected vault by executing it as an MDL script and,
 * on success, recording its checksum so the project tracks it as deployed.
 */
public class DeployMdlTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(DeployMdlTask.class);

	private final PsiFile psiFile;
	private VaultResponse vaultResponse;

	/**
	 * @param project the IntelliJ project, may be {@code null}
	 * @param psiFile the MDL file to deploy
	 */
	public DeployMdlTask(@Nullable Project project, @NotNull PsiFile psiFile) {
		super(project, "Deploying MDL");
		this.psiFile = psiFile;
	}

	/**
	 * Executes the MDL script in a background thread and records the file checksum on success.
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
			vaultResponse = toolboxProject.getVaultClient().newRequest(MetaDataRequest.class)
					.setRequestString(fileContent)
					.executeMDLScript();
			if (vaultResponse != null && vaultResponse.isFailure()) {
				if (toolboxProject.handleSessionExpiration(vaultResponse)) {
					vaultResponse = null;
					return;
				}
			}
			if (vaultResponse != null && !vaultResponse.isFailure()) {
				String md5 = getMd5(fileContent);
				toolboxProject.includeFile(psiFile.getVirtualFile().getPath(), md5);
			}
			if (vaultResponse != null) {
				logger.debug("deployment results = " + vaultResponse.getResponseStatus());
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Displays the deployment results in a UI message on the EDT.
	 */
	@Override
	public void onSuccess() {
		super.onSuccess();
		try {
			if (toolboxProject != null && vaultResponse != null) {
				Message message = toolboxProject.newMessage();
				message.setTitle("Deploy: " + psiFile.getName());
				Deploy.showResults(vaultResponse, message);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
