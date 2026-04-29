package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.veeva.vault.toolbox.intellij.language.MdlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class LinkFileTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(LinkFileTask.class);
	private final PsiFile psiFile;
	private boolean isLinked = false;

	public LinkFileTask(@Nullable Project project, @NotNull String title, @NotNull PsiFile psiFile, boolean isLinked) {
		super(project, title);
		this.psiFile = psiFile;
		this.isLinked = isLinked;
	}

	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			if (toolboxProject != null) {
				toggleLink(psiFile);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void toggleLink(PsiFile currentPsiFile) {
		if (currentPsiFile.isDirectory()) {
			if (isLinked) {
				toolboxProject.removeFile(currentPsiFile.getVirtualFile().getPath());
			}
			else {
				toolboxProject.includeFile(currentPsiFile.getVirtualFile().getPath());
			}

			Arrays.stream(currentPsiFile.getChildren()).forEach(child -> {
				toggleLink((PsiFile)child);
			});
		}
		else {
			if (isLinked) {
				toolboxProject.removeFile(currentPsiFile.getVirtualFile().getPath());
			}
			else {
				if (psiFile instanceof PsiJavaFile || psiFile instanceof MdlFile) {
					toolboxProject.includeFile(currentPsiFile.getVirtualFile().getPath());
				}
			}
		}
	}
}
