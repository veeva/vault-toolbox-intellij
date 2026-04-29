package com.veeva.vault.toolbox.intellij.listeners;

import com.veeva.vault.toolbox.intellij.ui.ToolboxTreeNode;

public interface ToolboxTreeNodeListener {
	void singleClick(ToolboxTreeNode node);
	void doubleClick(ToolboxTreeNode node);
}
