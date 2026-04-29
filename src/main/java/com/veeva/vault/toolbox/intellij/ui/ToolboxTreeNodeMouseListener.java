package com.veeva.vault.toolbox.intellij.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;

public class ToolboxTreeNodeMouseListener extends MouseAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ToolboxTreeNodeMouseListener.class);

    private final JTree tree;
    private boolean singleClick = true;
    private final int doubleClickDelay = 300;
    private Timer timer;

    public ToolboxTreeNodeMouseListener(JTree tree)
    {
        this.tree = tree;
        ActionListener actionListener = e -> {
			timer.stop();
			if (singleClick) {
				singleClickHandler(e);
			} else {
				try {
					doubleClickHandler(e);
				} catch (ParseException ex) {
					logger.error(ex.getMessage(), ex);
				}
			}
		};
        timer = new javax.swing.Timer(doubleClickDelay, actionListener);
        timer.setRepeats(false);
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 1) {
            singleClick = true;
            timer.start();
        } else {
            singleClick = false;
        }
    }

     private void singleClickHandler(ActionEvent e) {
         Object node = tree.getLastSelectedPathComponent();
         if (node != null && node instanceof ToolboxTreeNode iconTreeNode) {
			 logger.debug("Single click " + iconTreeNode.getText());
             iconTreeNode.singleClick();
         }
    }

     private void doubleClickHandler(ActionEvent e) throws ParseException {
         Object node = tree.getLastSelectedPathComponent();
         if (node != null && node instanceof ToolboxTreeNode iconTreeNode) {
			 logger.debug("Double click " + iconTreeNode.getText());
             iconTreeNode.doubleClick();
         }
     }
}