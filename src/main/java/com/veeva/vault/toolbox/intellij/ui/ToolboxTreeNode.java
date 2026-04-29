package com.veeva.vault.toolbox.intellij.ui;

import com.veeva.vault.toolbox.intellij.listeners.ToolboxTreeNodeListener;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class ToolboxTreeNode extends DefaultMutableTreeNode {

	protected Icon icon;
	protected ToolboxTreeNodeListener toolboxTreeNodeListener;

	private String text;

	public ToolboxTreeNode() {
		super();
	}

	public ToolboxTreeNode(Object userObject) {
		this(userObject, true, null, null);
	}

	public ToolboxTreeNode(Object userObject, boolean allowsChildren, Icon icon) {
		this(userObject, allowsChildren, icon, null);
	}

	public ToolboxTreeNode(Object userObject, boolean allowsChildren, Icon icon, ToolboxTreeNodeListener toolboxTreeNodeListener) {
		super(userObject, allowsChildren);
		this.setText(userObject.toString());
		this.icon = icon;
		this.toolboxTreeNodeListener = toolboxTreeNodeListener;
	}

	public void setIcon(Icon icon) {
	this.icon = icon;
	}
	public Icon getIcon() {
	return icon;
	}
	public void setText(String text){
	this.text=text;
	}
	public String getText(){
	return text;
	}
	public String toString(){
		return text;
	}

	public void singleClick() {
		if (toolboxTreeNodeListener != null) {
			toolboxTreeNodeListener.singleClick(this);
		}
	}

	public void doubleClick() {
		if (toolboxTreeNodeListener != null) {
			toolboxTreeNodeListener.doubleClick(this);
		}
	}

}