package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.veeva.vault.toolbox.intellij.services.Deploy;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.SDKRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class DropSdkTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(DropSdkTask.class);
	private final PsiFile psiFile;
	private VaultResponse vaultResponse;

	public DropSdkTask(@Nullable Project project, @NotNull PsiFile psiFile) {
		super(project, "Dropping SDK");
		this.psiFile = psiFile;
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			ApplicationManager.getApplication().runReadAction(() -> {
				if (psiFile instanceof PsiJavaFile psiJavaFile) {
					String className = psiJavaFile.getPackageName() + "." + psiJavaFile.getName().replace(".java", "");
					vaultResponse = toolboxProject.getVaultClient().newRequest(SDKRequest.class)
							.setBinaryFile(psiFile.getName(), psiFile.getText().getBytes(StandardCharsets.UTF_8))
							.deleteSingleSourceCodeFile(className);
					if (vaultResponse != null && !vaultResponse.isFailure()) {
						toolboxProject.removeFile(psiFile.getVirtualFile().getPath());
					}
					logger.debug("deployment results = " + vaultResponse.getResponseStatus());
				}
			});
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
				message.setTitle("Drop: " + psiFile.getName());
				Deploy deploy = new Deploy(toolboxProject);
				deploy.showResults(vaultResponse, message);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
