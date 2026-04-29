package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.veeva.vault.toolbox.intellij.language.MdlFile;
import com.veeva.vault.toolbox.intellij.services.Deploy;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropMdlTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(DropMdlTask.class);
	private final PsiFile psiFile;
	private VaultResponse vaultResponse;

	public DropMdlTask(@Nullable Project project, @NotNull String title, @NotNull PsiFile psiFile) {
		super(project, title);
		this.psiFile = psiFile;
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			ApplicationManager.getApplication().runReadAction(() -> {
				if (psiFile instanceof MdlFile mdlFile) {
					logger.debug("command = " + mdlFile.getContentElementType().toString());

					/*
					ApplicationManager.getApplication().invokeLater(() -> {
						Deploy deploy = new Deploy(toolboxProject);
						deploy.showResults(response, psiFile.getName());
					});
					 */
				}
				else {
					logger.error("not mdl file: " + psiFile);
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
