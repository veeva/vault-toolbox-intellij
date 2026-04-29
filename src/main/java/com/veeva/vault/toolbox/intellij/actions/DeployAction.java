package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.veeva.vault.toolbox.intellij.language.MdlFile;
import com.veeva.vault.toolbox.intellij.language.VpkFile;
import com.veeva.vault.toolbox.intellij.tasks.DeployMdlTask;
import com.veeva.vault.toolbox.intellij.tasks.DeploySdkTask;
import com.veeva.vault.toolbox.intellij.tasks.DeployVpkTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployAction extends ToolboxAction {
	private static final Logger logger = LoggerFactory.getLogger(DeployAction.class);

	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		super.actionPerformed(anActionEvent);
		try {
			PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
			if (psiFile != null) {
				if (toolboxProject != null && toolboxProject.prepareRequest()) {
					logger.debug("Deploying VPK file: " + psiFile.getVirtualFile().getPath());
					FileDocumentManager.getInstance().saveDocument(psiFile.getViewProvider().getDocument());
					if (psiFile.getVirtualFile().getFileType().getDisplayName().equalsIgnoreCase("vpk")) {
						deployPackage(psiFile.getVirtualFile());
					} else {
						deploySingleFile(psiFile);
					}
				}
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void deployPackage(VirtualFile virtualFile) {
		try {
			DeployVpkTask task = new DeployVpkTask(toolboxProject.getProject(), virtualFile);
			task.queue();
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void deploySingleFile(PsiFile psiFile) {
		try {
			if (psiFile instanceof PsiJavaFile) {
				DeploySdkTask task = new DeploySdkTask(psiFile.getProject(), psiFile);
				task.queue();
			}
			else if (psiFile instanceof MdlFile) {
				DeployMdlTask task = new DeployMdlTask(psiFile.getProject(), psiFile);
				task.queue();
			}
			else {
				logger.warn("unsupported file type " + psiFile.getVirtualFile().getPresentableUrl());
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
					if (psiFile instanceof PsiJavaFile || psiFile instanceof MdlFile || psiFile instanceof VpkFile) {
						isEnabled = true;
						isVisible = true;
					} else if (psiFile.getVirtualFile().getFileType().getDisplayName().equalsIgnoreCase("vpk")) {
						isEnabled = true;
						isVisible = true;
						logger.warn("vpk detected by name not type");
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
