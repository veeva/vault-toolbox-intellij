package com.veeva.vault.toolbox.intellij.ui;

import javax.swing.*;
import java.awt.*;

class CustomPanel extends JPanel
{
    public CustomPanel(Color backGroundColour)
    {
        setOpaque(true);
        setBackground(backGroundColour);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return (new Dimension(200, 20000));
    }
}