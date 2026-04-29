package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.vapil.api.model.common.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class VaultInfoPanel extends ToolboxPanel {
    private static final Logger logger = LoggerFactory.getLogger(VaultInfoPanel.class);

    JLabel vaultNameLabel = new JLabel("Vault Name:");
    JTextField vaultNameValue = new JTextField();
    JLabel vaultDnsLabel = new JLabel("Vault DNS:");
    JTextField vaultDnsValue = new JTextField();
    JLabel vaultIdLabel = new JLabel("Vault ID:");
    JTextField vaultIdValue = new JTextField();
    JLabel vaultFamilyLabel = new JLabel("Vault Family:");
    JTextField vaultFamilyValue = new JTextField();
    JLabel vaultApplicationLabel = new JLabel("Vault Application:");
    JTextField vaultApplicationValue = new JTextField();
    JLabel domainTypeLabel = new JLabel("Domain Type:");
    JTextField domainTypeValue = new JTextField();
    JLabel userLabel = new JLabel("User:");
    JTextField userValue = new JTextField();

    // Tracker to reset the previously clicked copy icon
    private JLabel activeCopyIcon = null;

    public VaultInfoPanel(ToolboxProject project) {
        super(project);
        init();
    }

    private void init() {
        this.setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Vault Name
        vaultNameValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel vaultNamePanel = new JPanel(new GridLayout(2, 1));
        vaultNamePanel.setBorder(JBUI.Borders.empty(2, 10));
        vaultNamePanel.add(vaultNameLabel);
        vaultNamePanel.add(addInlineCopyIcon(vaultNameValue));

        // Vault DNS
        vaultDnsValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel vaultDnsPanel = new JPanel(new GridLayout(2, 1));
        vaultDnsPanel.setBorder(JBUI.Borders.empty(2, 10));
        vaultDnsPanel.add(vaultDnsLabel);
        vaultDnsPanel.add(addInlineCopyIcon(vaultDnsValue));

        // Vault ID
        vaultIdValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel vaultIdPanel = new JPanel(new GridLayout(2, 1));
        vaultIdPanel.setBorder(JBUI.Borders.empty(2, 10));
        vaultIdPanel.add(vaultIdLabel);
        vaultIdPanel.add(addInlineCopyIcon(vaultIdValue));

        // Vault Family
        vaultFamilyValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel vaultFamilyPanel = new JPanel(new GridLayout(2, 1));
        vaultFamilyPanel.setBorder(JBUI.Borders.empty(2, 10));
        vaultFamilyPanel.add(vaultFamilyLabel);
        vaultFamilyPanel.add(addInlineCopyIcon(vaultFamilyValue));

        // Vault Application
        vaultApplicationValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel vaultApplicationPanel = new JPanel(new GridLayout(2, 1));
        vaultApplicationPanel.setBorder(JBUI.Borders.empty(2, 10));
        vaultApplicationPanel.add(vaultApplicationLabel);
        vaultApplicationPanel.add(addInlineCopyIcon(vaultApplicationValue));

        // Domain Type
        domainTypeValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel domainTypePanel = new JPanel(new GridLayout(2, 1));
        domainTypePanel.setBorder(JBUI.Borders.empty(2, 10));
        domainTypePanel.add(domainTypeLabel);
        domainTypePanel.add(addInlineCopyIcon(domainTypeValue));

        // User
        userValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel userPanel = new JPanel(new GridLayout(2, 1));
        userPanel.setBorder(JBUI.Borders.empty(2, 10));
        userPanel.add(userLabel);
        userPanel.add(addInlineCopyIcon(userValue));

        // Manual BorderLayout nesting stack
        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.add(vaultNamePanel, BorderLayout.NORTH);
        topWrapper.add(vaultDnsPanel, BorderLayout.CENTER);
        topWrapper.add(vaultIdPanel, BorderLayout.SOUTH);

        JPanel middleWrapper = new JPanel(new BorderLayout());
        middleWrapper.add(vaultFamilyPanel, BorderLayout.NORTH);
        middleWrapper.add(vaultApplicationPanel, BorderLayout.CENTER);
        middleWrapper.add(domainTypePanel, BorderLayout.SOUTH);

        mainPanel.add(topWrapper, BorderLayout.NORTH);
        mainPanel.add(middleWrapper, BorderLayout.CENTER);
        mainPanel.add(userPanel, BorderLayout.SOUTH);

        this.add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Injects a copy icon directly INSIDE the JTextField.
     * Uses absolute positioning for a clean, flush look.
     */
    private JTextField addInlineCopyIcon(JTextField field) {
        field.setEditable(false);
        field.setBackground(com.intellij.util.ui.UIUtil.getPanelBackground());
        field.setForeground(vaultDnsLabel.getForeground());

        // 1. Use a JLabel to bypass hidden margins
        JLabel copyIcon = new JLabel(AllIcons.Actions.Copy);
        copyIcon.setToolTipText("Copy to clipboard");
        copyIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));

        copyIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                String text = field.getText();
                if (text != null && !text.isEmpty()) {
                    // 1. Copy to clipboard
                    StringSelection selection = new StringSelection(text);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

                    // 2. Reset the previous icon if it exists and isn't this one
                    if (activeCopyIcon != null && activeCopyIcon != copyIcon) {
                        activeCopyIcon.setIcon(AllIcons.Actions.Copy);
                        activeCopyIcon.setToolTipText("Copy to clipboard");
                    }

                    // 3. Set this icon to the checkmark state
                    copyIcon.setIcon(AllIcons.Actions.Checked);
                    copyIcon.setToolTipText("Copied!");

                    // 4. Update the tracker
                    activeCopyIcon = copyIcon;
                }
            }
        });

        // 2. Pad the right side of the text field so long text doesn't hide underneath the icon
        Border currentBorder = field.getBorder();
        field.setBorder(BorderFactory.createCompoundBorder(currentBorder, JBUI.Borders.emptyRight(28)));

        // 3. Break away from layout managers and inject the icon
        field.setLayout(null);
        field.add(copyIcon);

        // 4. Pin the icon 10px from the right edge, matching the Login panel
        field.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int height = field.getHeight();
                int width = field.getWidth();

                Dimension prefSize = copyIcon.getPreferredSize();
                int iconW = prefSize.width > 0 ? prefSize.width : 16;
                int iconH = prefSize.height > 0 ? prefSize.height : 16;

                copyIcon.setBounds(width - iconW - 10, (height - iconH) / 2, iconW, iconH);
            }
        });

        return field;
    }

    public void refreshVaultInfo() {
        vaultNameValue.setText("");
        vaultDnsValue.setText("");
        vaultIdValue.setText("");
        vaultFamilyValue.setText("");
        vaultApplicationValue.setText("");
        domainTypeValue.setText("");
        userValue.setText("");

        // Reset copy icons when refreshing connection
        if (activeCopyIcon != null) {
            activeCopyIcon.setIcon(AllIcons.Actions.Copy);
            activeCopyIcon.setToolTipText("Copy to clipboard");
            activeCopyIcon = null;
        }

        if (toolboxProject != null && toolboxProject.isConnected()) {
            vaultNameValue.setText(toolboxProject.getVaultName());
            vaultDnsValue.setText(toolboxProject.getVaultDNS());
            vaultIdValue.setText(toolboxProject.getVaultId().toString());
            vaultFamilyValue.setText(toolboxProject.getVaultFamily());
            vaultApplicationValue.setText(toolboxProject.getVaultApplication());
            domainTypeValue.setText(toolboxProject.getDomainType());

            User user = toolboxProject.getVaultUser();
            if (user != null) {
                userValue.setText(user.getUserName());
            }
        }
    }
}