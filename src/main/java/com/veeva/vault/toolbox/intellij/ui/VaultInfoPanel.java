package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.veeva.vault.toolbox.intellij.credentials.VaultCredentialManager;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.settings.AppSettings;
import com.veeva.vault.toolbox.intellij.settings.SavedCredential;
import com.veeva.vault.vapil.api.model.common.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;

/**
 * Panel that displays information about the currently connected Vault.
 */
public class VaultInfoPanel extends ToolboxPanel {
    private static final Logger logger = LoggerFactory.getLogger(VaultInfoPanel.class);

    /** Label for the Vault Name field. */
    JBLabel vaultNameLabel = new JBLabel("Vault Name:");
    /** Text field for displaying the Vault Name. */
    ExtendableTextField vaultNameValue = new ExtendableTextField();
    
    /** Label for the Vault DNS field. */
    JBLabel vaultDnsLabel = new JBLabel("Vault DNS:");
    /** Text field for displaying the Vault DNS. */
    ExtendableTextField vaultDnsValue = new ExtendableTextField();
    
    /** Label for the Vault ID field. */
    JBLabel vaultIdLabel = new JBLabel("Vault ID:");
    /** Text field for displaying the Vault ID. */
    ExtendableTextField vaultIdValue = new ExtendableTextField();
    
    /** Label for the Vault Family field. */
    JBLabel vaultFamilyLabel = new JBLabel("Vault Family:");
    /** Text field for displaying the Vault Family. */
    ExtendableTextField vaultFamilyValue = new ExtendableTextField();
    
    /** Label for the Vault Application field. */
    JBLabel vaultApplicationLabel = new JBLabel("Vault Application:");
    /** Text field for displaying the Vault Application. */
    ExtendableTextField vaultApplicationValue = new ExtendableTextField();
    
    /** Label for the Domain Type field. */
    JBLabel domainTypeLabel = new JBLabel("Domain Type:");
    /** Text field for displaying the Domain Type. */
    ExtendableTextField domainTypeValue = new ExtendableTextField();
    
    /** Label for the User field. */
    JBLabel userLabel = new JBLabel("User:");
    /** Text field for displaying the User. */
    ExtendableTextField userValue = new ExtendableTextField();

    /**
     * Initializes the Vault Info Panel with the specified project.
     *
     * @param project The toolbox project context.
     */
    public VaultInfoPanel(ToolboxProject project) {
        super(project);
        init();
    }

    /**
     * Configures the layout and components of the panel.
     */
    private void init() {
        this.setLayout(new BorderLayout());

        vaultNameValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        vaultDnsValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        vaultIdValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        vaultFamilyValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        vaultApplicationValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        domainTypeValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        userValue.setDisabledTextColor(vaultDnsLabel.getForeground());

        JPanel formPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(vaultNameLabel, addInlineCopyIcon(vaultNameValue), 1, true)
                .addLabeledComponent(vaultDnsLabel, addInlineCopyIcon(vaultDnsValue), 1, true)
                .addLabeledComponent(vaultIdLabel, addInlineCopyIcon(vaultIdValue), 1, true)
                .addLabeledComponent(vaultFamilyLabel, addInlineCopyIcon(vaultFamilyValue), 1, true)
                .addLabeledComponent(vaultApplicationLabel, addInlineCopyIcon(vaultApplicationValue), 1, true)
                .addLabeledComponent(domainTypeLabel, addInlineCopyIcon(domainTypeValue), 1, true)
                .addLabeledComponent(userLabel, addInlineCopyIcon(userValue), 1, true)
                .getPanel();

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(JBUI.Borders.empty(20, 20, 10, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.05; gbc.weighty = 1.0;
        contentPanel.add(new JPanel(), gbc);
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.05; gbc.weighty = 1.0;
        contentPanel.add(new JPanel(), gbc);

        gbc.gridx = 1; gbc.weightx = 0.90; gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTH; gbc.insets = JBUI.emptyInsets();
        contentPanel.add(formPanel, gbc);

        this.add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Extension for ExtendableTextComponent that provides a copy-to-clipboard action.
     */
    private abstract static class CopyExtension implements ExtendableTextComponent.Extension {
        Icon currentIcon = AllIcons.Actions.Copy;
        String tooltip = "Copy to clipboard";

        private final Icon unhoveredIcon = new Icon() {
            /**
             * Paints the icon.
             *
             * @param c the component
             * @param g the graphics context
             * @param x the x coordinate
             * @param y the y coordinate
             */
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                currentIcon.paintIcon(c, g, x, y);
            }

            /**
             * Gets the icon width.
             *
             * @return the icon width
             */
            @Override
            public int getIconWidth() {
                return currentIcon.getIconWidth();
            }

            /**
             * Gets the icon height.
             *
             * @return the icon height
             */
            @Override
            public int getIconHeight() {
                return currentIcon.getIconHeight();
            }
        };

        private final Icon hoveredIcon = new Icon() {
            /**
             * Paints the icon.
             *
             * @param c the component
             * @param g the graphics context
             * @param x the x coordinate
             * @param y the y coordinate
             */
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new com.intellij.ui.JBColor(new java.awt.Color(0, 0, 0, 20), new java.awt.Color(255, 255, 255, 30)));
                g2.fillRoundRect(x - 2, y - 2, getIconWidth() + 4, getIconHeight() + 4, 4, 4);
                g2.dispose();
                currentIcon.paintIcon(c, g, x, y);
            }

            /**
             * Gets the icon width.
             *
             * @return the icon width
             */
            @Override
            public int getIconWidth() {
                return currentIcon.getIconWidth();
            }

            /**
             * Gets the icon height.
             *
             * @return the icon height
             */
            @Override
            public int getIconHeight() {
                return currentIcon.getIconHeight();
            }
        };

        /**
         * Gets the icon to display.
         *
         * @param hovered whether the mouse is hovering over the icon
         * @return the icon
         */
        @Override
        public Icon getIcon(boolean hovered) {
            return hovered ? hoveredIcon : unhoveredIcon;
        }

        /**
         * Gets the tooltip text for the icon.
         *
         * @return the tooltip text
         */
        @Override
        public String getTooltip() {
            return tooltip;
        }

        /**
         * Resets the icon and tooltip to their default state.
         */
        public void reset() {
            currentIcon = AllIcons.Actions.Copy;
            tooltip = "Copy to clipboard";
        }
    }

    /** The currently active copy extension. */
    private static CopyExtension activeExtension = null;
    
    /** The currently active extendable text field. */
    private static ExtendableTextField activeField = null;
    
    /** Timer to revert the copy icon to its default state. */
    private static final javax.swing.Timer revertTimer = new javax.swing.Timer(2000, ev -> {
        if (activeExtension != null) {
            activeExtension.reset();
            if (activeField != null) {
                activeField.repaint();
            }
            activeExtension = null;
            activeField = null;
        }
    });
    static {
        revertTimer.setRepeats(false);
    }

    /**
     * Adds an inline copy icon to the end of a text field.
     *
     * @param field The text field to decorate.
     * @return The decorated text field wrapped in a JPanel.
     */
    private JPanel addInlineCopyIcon(ExtendableTextField field) {
        field.setEditable(false);
        field.setBackground(com.intellij.util.ui.UIUtil.getPanelBackground());
        field.setForeground(vaultDnsLabel.getForeground());

        CopyExtension extension = new CopyExtension() {
            @Override
            public Runnable getActionOnClick() {
                return () -> {
                    String text = field.getText();
                    if (text != null && !text.isEmpty()) {
                        StringSelection selection = new StringSelection(text);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

                        if (activeExtension != null && activeExtension != this) {
                            activeExtension.reset();
                            if (activeField != null) {
                                activeField.repaint();
                            }
                        }

                        currentIcon = AllIcons.Actions.Checked;
                        tooltip = "Copied!";
                        field.repaint();

                        JBPopupFactory.getInstance()
                                .createHtmlTextBalloonBuilder("Copied to clipboard", MessageType.INFO, null)
                                .setFadeoutTime(1000)
                                .setAnimationCycle(200)
                                .createBalloon()
                                .show(new RelativePoint(field, new Point(field.getWidth() - 12, field.getHeight() / 2)), Balloon.Position.above);

                        activeExtension = this;
                        activeField = field;

                        revertTimer.restart();
                    }
                };
            }
        };

        field.addExtension(extension);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(field, BorderLayout.CENTER);

        return wrapper;
    }

    /**
     * Refreshes the vault information displayed in the panel using the current project context.
     * Network-bound calls are dispatched to a pooled thread to avoid blocking the EDT.
     */
    public void refreshVaultInfo() {
        vaultNameValue.setText("");
        vaultDnsValue.setText("");
        vaultIdValue.setText("");
        vaultFamilyValue.setText("");
        vaultApplicationValue.setText("");
        domainTypeValue.setText("");
        userValue.setText("");

        if (activeExtension != null) {
            activeExtension.reset();
            if (activeField != null) {
                activeField.repaint();
            }
            activeExtension = null;
            activeField = null;
        }

        if (toolboxProject == null || !toolboxProject.isConnected()) return;

        vaultDnsValue.setText(toolboxProject.getVaultDNS());

        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String vaultName    = toolboxProject.getVaultName();
            Integer vaultIdVal  = toolboxProject.getVaultId();
            String vaultFamily  = toolboxProject.getVaultFamily();
            String vaultApp     = toolboxProject.getVaultApplication();
            String domainType   = toolboxProject.getDomainType();
            User user           = toolboxProject.getVaultUser();

            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                vaultNameValue.setText(vaultName != null ? vaultName : "");
                vaultIdValue.setText(vaultIdVal != null ? vaultIdVal.toString() : "");
                vaultFamilyValue.setText(vaultFamily != null ? vaultFamily : "");
                vaultApplicationValue.setText(vaultApp != null ? vaultApp : "");
                domainTypeValue.setText(domainType != null ? domainType : "");
                if (user != null) {
                    userValue.setText(user.getUserName());
                }
            });
        });
    }
}
