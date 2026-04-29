package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.veeva.vault.toolbox.intellij.services.Deploy;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.SDKRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static com.veeva.vault.toolbox.core.utils.Checksum.getMd5;

public class DeploySdkTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(DeploySdkTask.class);
	private final PsiFile psiFile;
	private VaultResponse vaultResponse;

	public DeploySdkTask(@Nullable Project project, @NotNull PsiFile psiFile) {
		super(project, "Deploying SDK");
		this.psiFile = psiFile;
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			String fileContent = psiFile.getText();
			vaultResponse = toolboxProject.getVaultClient().newRequest(SDKRequest.class)
					.setBinaryFile(psiFile.getName(), fileContent.getBytes(StandardCharsets.UTF_8))
					.addOrReplaceSingleSourceCodeFile();
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
				ApplicationManager.getApplication().executeOnPooledThread(() -> {
					ApplicationManager.getApplication().runReadAction(() -> {
						Message message = toolboxProject.newMessage();
						message.setTitle("Deploy: " + psiFile.getName());
						Deploy deploy = new Deploy(toolboxProject);
						deploy.showResults(vaultResponse, message);
					});
				});
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
