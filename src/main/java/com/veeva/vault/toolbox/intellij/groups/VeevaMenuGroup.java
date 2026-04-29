package com.veeva.vault.toolbox.intellij.groups;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VeevaMenuGroup extends ToolboxMenuGroup {
	private static final Logger logger = LoggerFactory.getLogger(VeevaMenuGroup.class);

	@Override
	public void update(@NotNull AnActionEvent anActionEvent) {
		super.update(anActionEvent);
		try {
			if (toolboxProject != null && toolboxProject.isToolboxEnabled()) {
				isEnabled = true;
				isVisible = true;
			}

			anActionEvent.getPresentation().setEnabled(isEnabled);
			anActionEvent.getPresentation().setVisible(isVisible);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}