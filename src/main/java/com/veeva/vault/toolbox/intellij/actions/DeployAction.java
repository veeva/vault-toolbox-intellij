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

/**
 * Deploys the selected resource to the connected vault. Supports Vault Java SDK
 * Java sources, MDL scripts, and VPK packages. The current document is saved
 * before deployment so the in-memory edits are sent to the vault.
 */
public class DeployAction extends ToolboxAction {
	private static final Logger logger = LoggerFactory.getLogger(DeployAction.class);
	private static final String VPK_FILE_TYPE = "vpk";

	/**
	 * Saves the current document and queues the deployment task that matches the
	 * selected file type.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		super.actionPerformed(anActionEvent);
		try {
			PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
			if (psiFile == null || toolboxProject == null || !toolboxProject.prepareRequest()) {
				return;
			}
			VirtualFile virtualFile = psiFile.getVirtualFile();
			logger.debug("Deploying file: {}", virtualFile.getPath());
			FileDocumentManager.getInstance().saveDocument(psiFile.getViewProvider().getDocument());
			if (isVpkFileType(virtualFile)) {
				deployPackage(virtualFile);
			} else {
				deploySingleFile(psiFile);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Enables the action when the selection is a deployable file type
	 * (Java SDK, MDL, or VPK) in a linked project.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void update(@NotNull AnActionEvent anActionEvent) {
		super.update(anActionEvent);
		try {
			PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
			if (psiFile != null && toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				if (psiFile instanceof PsiJavaFile || psiFile instanceof MdlFile || psiFile instanceof VpkFile) {
					isEnabled = true;
					isVisible = true;
				} else if (isVpkFileType(psiFile.getVirtualFile())) {
					isEnabled = true;
					isVisible = true;
					logger.warn("VPK detected by file-type display name rather than language type.");
				}
			}
			anActionEvent.getPresentation().setEnabled(isEnabled);
			anActionEvent.getPresentation().setVisible(isVisible);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void deployPackage(VirtualFile virtualFile) {
		try {
			new DeployVpkTask(toolboxProject.getProject(), virtualFile).queue();
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void deploySingleFile(PsiFile psiFile) {
		try {
			if (psiFile instanceof PsiJavaFile) {
				new DeploySdkTask(psiFile.getProject(), psiFile).queue();
			}
			else if (psiFile instanceof MdlFile) {
				new DeployMdlTask(psiFile.getProject(), psiFile).queue();
			}
			else {
				logger.warn("Unsupported file type: {}", psiFile.getVirtualFile().getPresentableUrl());
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static boolean isVpkFileType(VirtualFile virtualFile) {
		return virtualFile != null
				&& VPK_FILE_TYPE.equalsIgnoreCase(virtualFile.getFileType().getDisplayName());
	}

}
