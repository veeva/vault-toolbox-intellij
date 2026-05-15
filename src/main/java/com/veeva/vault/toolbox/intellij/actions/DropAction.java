package com.veeva.vault.toolbox.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.veeva.vault.toolbox.intellij.language.MdlFile;
import com.veeva.vault.toolbox.intellij.tasks.DropMdlTask;
import com.veeva.vault.toolbox.intellij.tasks.DropSdkTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drops a Vault component from the connected vault. Supports Vault Java SDK sources
 * and MDL scripts by queuing the appropriate drop task for the selected file.
 */
public class DropAction extends ToolboxAction {
	private static final Logger logger = LoggerFactory.getLogger(DropAction.class);

	/**
	 * Queues a drop task for the selected file once a vault session is prepared.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
		super.actionPerformed(anActionEvent);
		try {
			PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
			if (psiFile != null && toolboxProject != null && toolboxProject.prepareRequest()) {
				logger.debug("Dropping file: {}", psiFile.getVirtualFile().getPath());
				if (psiFile instanceof PsiJavaFile) {
					new DropSdkTask(psiFile.getProject(), psiFile).queue();
				}
				else if (psiFile instanceof MdlFile) {
					new DropMdlTask(psiFile.getProject(), psiFile).queue();
				}
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Enables the action when the selection is a Java source file or MDL file in a linked project.
	 *
	 * @param anActionEvent the action event provided by the IntelliJ platform
	 */
	@Override
	public void update(@NotNull AnActionEvent anActionEvent) {
		super.update(anActionEvent);
		try {
			PsiFile psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE);
			if (psiFile != null && toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				if (psiFile instanceof PsiJavaFile || psiFile instanceof MdlFile) {
					isEnabled = true;
					isVisible = true;
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
