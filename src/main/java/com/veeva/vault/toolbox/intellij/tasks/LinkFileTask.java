package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.veeva.vault.toolbox.intellij.language.MdlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Toggles whether a file (or directory of files) is tracked by the toolbox. When
 * tracking is enabled, only Java SDK and MDL files are added; directories are
 * always traversed recursively.
 */
public class LinkFileTask extends ToolboxTask {
	private static final Logger logger = LoggerFactory.getLogger(LinkFileTask.class);

	private final PsiFile psiFile;
	private final boolean isLinked;

	/**
	 * @param project  the IntelliJ project, may be {@code null}
	 * @param title    the progress title shown to the user
	 * @param psiFile  the file or directory whose link state should be toggled
	 * @param isLinked the current link state; when {@code true} the file is unlinked,
	 *                 when {@code false} it is linked
	 */
	public LinkFileTask(@Nullable Project project, @NotNull String title, @NotNull PsiFile psiFile, boolean isLinked) {
		super(project, title);
		this.psiFile = psiFile;
		this.isLinked = isLinked;
	}

	/**
	 * Executes the link toggling logic in a background thread.
	 *
	 * @param indicator the progress indicator for the background task
	 */
	@Override
	public void run(@NotNull ProgressIndicator indicator) {
		try {
			if (toolboxProject != null) {
				List<String> toRemove = new ArrayList<>();
				List<String> toInclude = new ArrayList<>();
				ApplicationManager.getApplication().runReadAction(() -> collectPaths(psiFile, toRemove, toInclude));
				toRemove.forEach(toolboxProject::removeFile);
				toInclude.forEach(toolboxProject::includeFile);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Collects the paths to include or remove based on the link state.
	 * Must be called inside a read action.
	 *
	 * @param currentPsiFile the file or directory to process
	 * @param toRemove       paths to unlink
	 * @param toInclude      paths to link
	 */
	private void collectPaths(PsiFile currentPsiFile, List<String> toRemove, List<String> toInclude) {
		if (currentPsiFile.isDirectory()) {
			String path = currentPsiFile.getVirtualFile().getPath();
			if (isLinked) {
				toRemove.add(path);
			} else {
				toInclude.add(path);
			}
			Arrays.stream(currentPsiFile.getChildren()).forEach(child -> collectPaths((PsiFile) child, toRemove, toInclude));
			return;
		}

		String path = currentPsiFile.getVirtualFile().getPath();
		if (isLinked) {
			toRemove.add(path);
		} else if (psiFile instanceof PsiJavaFile || psiFile instanceof MdlFile) {
			toInclude.add(path);
		}
	}
}
