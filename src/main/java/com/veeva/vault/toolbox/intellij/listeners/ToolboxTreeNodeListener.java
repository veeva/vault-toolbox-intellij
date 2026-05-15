package com.veeva.vault.toolbox.intellij.listeners;

import com.veeva.vault.toolbox.intellij.ui.ToolboxTreeNode;

/**
 * Receives notifications about user interactions with a {@link ToolboxTreeNode}.
 */
public interface ToolboxTreeNodeListener {

	/**
	 * Invoked when the given node receives a single-click event.
	 *
	 * @param node the node that was clicked.
	 */
	void singleClick(ToolboxTreeNode node);

	/**
	 * Invoked when the given node receives a double-click event.
	 *
	 * @param node the node that was double-clicked.
	 */
	void doubleClick(ToolboxTreeNode node);
}
