package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

public class ToolboxValueLabel extends JPanel {
	JLabel label = new JLabel();
	public ToolboxValueLabel() {
		super();
		this.setLayout(new BorderLayout());
		this.setBorder(JBUI.Borders.empty(15));
		this.add(label, BorderLayout.WEST);
	}

	public void setText(String text) {
		this.label.setText(text);
	}
}
