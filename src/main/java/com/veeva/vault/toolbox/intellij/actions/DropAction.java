package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.veeva.vault.toolbox.intellij.tasks.DropSdkTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropAction extends ToolboxAction {
	private static final Logger logger = LoggerFactory.getLogger(DropAction.class);


	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		super.actionPerformed(anActionEvent);
		try {
			PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
			if (psiFile != null) {
				logger.debug("Deploying VPK file: " + psiFile.getVirtualFile().getPath());
				if (toolboxProject != null && toolboxProject.prepareRequest()) {
					DropSdkTask task = new DropSdkTask(psiFile.getProject(), psiFile);
					task.queue();
				}
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void update(@NotNull AnActionEvent anActionEvent) {
		super.update(anActionEvent);
		try {
			PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
			if (psiFile != null) {
				if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
					if (psiFile instanceof PsiJavaFile) {
						isEnabled = true;
						isVisible = true;
					}
				}
			}

			anActionEvent.getPresentation().setEnabled(isEnabled);
			anActionEvent.getPresentation().setVisible(isVisible);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
