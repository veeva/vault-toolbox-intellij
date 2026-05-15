package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.util.ui.JBUI;
import javax.swing.*;
import java.awt.*;

/**
 * A specialized panel that displays a single text label with consistent padding.
 */
public class ToolboxValueLabel extends JPanel {
	private final JLabel label = new JLabel();

	/**
	 * Initializes the value label panel with default layout and padding.
	 */
	public ToolboxValueLabel() {
		super(new BorderLayout());
		this.setBorder(JBUI.Borders.empty(15));
		this.add(label, BorderLayout.WEST);
	}

	/**
	 * Sets the text content of the displayed label.
	 *
	 * @param text The text to display.
	 */
	public void setText(String text) {
		this.label.setText(text);
	}
}
