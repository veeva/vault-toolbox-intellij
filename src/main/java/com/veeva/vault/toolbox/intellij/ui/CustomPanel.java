package com.veeva.vault.toolbox.intellij.ui;

import javax.swing.*;
import java.awt.*;

/**
 * A specialized JPanel with a fixed preferred height, primarily used for layout padding or spacers.
 */
class CustomPanel extends JPanel {

    /**
     * Initializes the panel with a specific background color.
     *
     * @param backGroundColour The background color to apply.
     */
    public CustomPanel(Color backGroundColour) {
        setOpaque(true);
        setBackground(backGroundColour);
    }

    /**
     * Returns a fixed dimension for this panel.
     *
     * @return A Dimension of 200x20000.
     */
    @Override
    public Dimension getPreferredSize() {
        return (new Dimension(200, 20000));
    }
}
