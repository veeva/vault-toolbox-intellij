package com.veeva.vault.toolbox.intellij.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Mouse listener for the toolbox tree that distinguishes between single and double clicks.
 * Uses a timer to delay single-click processing to check for potential double-clicks.
 */
public class ToolboxTreeNodeMouseListener extends MouseAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ToolboxTreeNodeMouseListener.class);

    private final JTree tree;
    private boolean singleClick = true;
    private static final int DOUBLE_CLICK_DELAY = 300;
    private final Timer timer;

    /**
     * Initializes the mouse listener for the specified tree.
     *
     * @param tree The JTree to monitor.
     */
    public ToolboxTreeNodeMouseListener(JTree tree) {
        this.tree = tree;
        ActionListener actionListener = e -> {
			if (singleClick) {
				singleClickHandler(e);
			} else {
				doubleClickHandler(e);
			}
		};
        timer = new Timer(DOUBLE_CLICK_DELAY, actionListener);
        timer.setRepeats(false);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 1) {
            singleClick = true;
            timer.start();
        } else {
            singleClick = false;
        }
    }

    /**
     * Handles the single-click event for a tree node.
     *
     * @param e The triggering action event.
     */
     private void singleClickHandler(ActionEvent e) {
         Object node = tree.getLastSelectedPathComponent();
         if (node instanceof ToolboxTreeNode iconTreeNode) {
             iconTreeNode.singleClick();
         }
    }

    /**
     * Handles the double-click event for a tree node.
     *
     * @param e The triggering action event.
     */
     private void doubleClickHandler(ActionEvent e) {
         Object node = tree.getLastSelectedPathComponent();
         if (node instanceof ToolboxTreeNode iconTreeNode) {
             iconTreeNode.doubleClick();
         }
     }
}
