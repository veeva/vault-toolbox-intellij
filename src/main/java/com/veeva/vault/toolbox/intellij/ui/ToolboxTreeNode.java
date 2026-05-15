package com.veeva.vault.toolbox.intellij.ui;

import com.veeva.vault.toolbox.intellij.listeners.ToolboxTreeNodeListener;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * A custom tree node for the Vault Toolbox that supports icons and specialized click listeners.
 */
public class ToolboxTreeNode extends DefaultMutableTreeNode {

	protected Icon icon;
	protected ToolboxTreeNodeListener toolboxTreeNodeListener;
	private String text;

	/**
	 * Creates an empty tree node.
	 */
	public ToolboxTreeNode() {
		super();
	}

	/**
	 * Creates a tree node with a user object.
	 *
	 * @param userObject The data object to store in the node.
	 */
	public ToolboxTreeNode(Object userObject) {
		this(userObject, true, null, null);
	}

	/**
	 * Creates a tree node with a user object, children flag, and icon.
	 *
	 * @param userObject     The data object to store.
	 * @param allowsChildren true if children are allowed.
	 * @param icon           The icon to display.
	 */
	public ToolboxTreeNode(Object userObject, boolean allowsChildren, Icon icon) {
		this(userObject, allowsChildren, icon, null);
	}

	/**
	 * Creates a tree node with full configuration.
	 *
	 * @param userObject              The data object to store.
	 * @param allowsChildren          true if children are allowed.
	 * @param icon                    The icon to display.
	 * @param toolboxTreeNodeListener The listener for click events.
	 */
	public ToolboxTreeNode(Object userObject, boolean allowsChildren, Icon icon, ToolboxTreeNodeListener toolboxTreeNodeListener) {
		super(userObject, allowsChildren);
		this.setText(userObject != null ? userObject.toString() : "");
		this.icon = icon;
		this.toolboxTreeNodeListener = toolboxTreeNodeListener;
	}

	/**
	 * @param icon The icon to display for this node.
	 */
	public void setIcon(Icon icon) {
		this.icon = icon;
	}

	/**
	 * @return The icon associated with this node.
	 */
	public Icon getIcon() {
		return icon;
	}

	/**
	 * @param text The display text for this node.
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * @return The display text for this node.
	 */
	public String getText() {
		return text;
	}

	@Override
	public String toString() {
		return text;
	}

	/**
	 * Triggers the single-click action if a listener is registered.
	 */
	public void singleClick() {
		if (toolboxTreeNodeListener != null) {
			toolboxTreeNodeListener.singleClick(this);
		}
	}

	/**
	 * Triggers the double-click action if a listener is registered.
	 */
	public void doubleClick() {
		if (toolboxTreeNodeListener != null) {
			toolboxTreeNodeListener.doubleClick(this);
		}
	}
}
