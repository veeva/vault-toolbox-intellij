package com.veeva.vault.toolbox.intellij.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class ToolboxTreeNodeRenderer extends DefaultTreeCellRenderer {
	private static final Logger logger = LoggerFactory.getLogger(ToolboxTreeNodeRenderer.class);

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
												  boolean isSelected, boolean expanded, boolean leaf, int row,
												  boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, hasFocus);

		ToolboxTreeNode iconNode = ((ToolboxTreeNode) value);
		setText(iconNode.getText());
		setIcon(iconNode.getIcon());
		setBackgroundNonSelectionColor(null);
		setBackground(null);
		return this;
	}
}