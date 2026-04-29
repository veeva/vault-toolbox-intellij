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

public class DeployMdlTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(DeployMdlTask.class);
	private final PsiFile psiFile;
	private VaultResponse vaultResponse;

	public DeployMdlTask(@Nullable Project project, @NotNull PsiFile psiFile) {
		super(project, "Deploying MDL");
		this.psiFile = psiFile;
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			String fileContent = psiFile.getText();
			vaultResponse = toolboxProject.getVaultClient().newRequest(MetaDataRequest.class)
					.setRequestString(fileContent)
					.executeMDLScript();
			if (vaultResponse != null && !vaultResponse.isFailure()) {
				String md5 = getMd5(fileContent);
				toolboxProject.includeFile(psiFile.getVirtualFile().getPath(), md5);
			}
			logger.debug("deployment results = " + vaultResponse.getResponseStatus());
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void onSuccess() {
		super.onSuccess();
		try {
			if (toolboxProject != null && vaultResponse != null) {
				Message message = toolboxProject.newMessage();
				message.setTitle("Deploy: " + psiFile.getName());
				Deploy deploy = new Deploy(toolboxProject);
				deploy.showResults(vaultResponse, message);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
