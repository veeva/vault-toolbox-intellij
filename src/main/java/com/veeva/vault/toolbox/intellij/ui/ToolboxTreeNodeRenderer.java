package com.veeva.vault.toolbox.intellij.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Custom renderer for {@link ToolboxTreeNode} objects.
 * Sets the display text and icon based on the node's properties.
 */
public class ToolboxTreeNodeRenderer extends DefaultTreeCellRenderer {
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTreeNodeRenderer.class);

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
												  boolean isSelected, boolean expanded, boolean leaf, int row,
												  boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, hasFocus);

		if (value instanceof ToolboxTreeNode iconNode) {
			setText(iconNode.getText());
			setIcon(iconNode.getIcon());
		}
		
		setBackgroundNonSelectionColor(null);
		setBackground(null);
		return this;
	}
}
