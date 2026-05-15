package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBUI;
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

    JLabel vaultNameLabel = new JLabel("Vault Name:");
    JTextField vaultNameValue = new JTextField(30);
    JLabel vaultDnsLabel = new JLabel("Vault DNS:");
    JTextField vaultDnsValue = new JTextField(30);
    JLabel vaultIdLabel = new JLabel("Vault ID:");
    JTextField vaultIdValue = new JTextField(30);
    JLabel vaultFamilyLabel = new JLabel("Vault Family:");
    JTextField vaultFamilyValue = new JTextField(30);
    JLabel vaultApplicationLabel = new JLabel("Vault Application:");
    JTextField vaultApplicationValue = new JTextField(30);
    JLabel domainTypeLabel = new JLabel("Domain Type:");
    JTextField domainTypeValue = new JTextField(30);
    JLabel userLabel = new JLabel("User:");
    JTextField userValue = new JTextField(30);

    private JLabel activeCopyIcon = null;
    private javax.swing.Timer revertTimer = null;

    private JPanel saveBanner;
    private JPanel bannerCards;
    private JLabel bannerMessageLabel;
    private JTextField bannerLabelField;
    private LoginPanel.PendingCredentialSave pendingSave;

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
        this.setSize(400, 300);
        this.setPreferredSize(new Dimension(400, 300));
        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel formPanel = new JPanel(new GridBagLayout());

        vaultNameValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel vaultNamePanel = new JPanel(new GridLayout(2, 1));
        vaultNamePanel.add(vaultNameLabel);
        vaultNamePanel.add(addInlineCopyIcon(vaultNameValue));

        vaultDnsValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel vaultDnsPanel = new JPanel(new GridLayout(2, 1));
        vaultDnsPanel.add(vaultDnsLabel);
        vaultDnsPanel.add(addInlineCopyIcon(vaultDnsValue));

        vaultIdValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel vaultIdPanel = new JPanel(new GridLayout(2, 1));
        vaultIdPanel.add(vaultIdLabel);
        vaultIdPanel.add(addInlineCopyIcon(vaultIdValue));

        vaultFamilyValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel vaultFamilyPanel = new JPanel(new GridLayout(2, 1));
        vaultFamilyPanel.add(vaultFamilyLabel);
        vaultFamilyPanel.add(addInlineCopyIcon(vaultFamilyValue));

        vaultApplicationValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel vaultApplicationPanel = new JPanel(new GridLayout(2, 1));
        vaultApplicationPanel.add(vaultApplicationLabel);
        vaultApplicationPanel.add(addInlineCopyIcon(vaultApplicationValue));

        domainTypeValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel domainTypePanel = new JPanel(new GridLayout(2, 1));
        domainTypePanel.add(domainTypeLabel);
        domainTypePanel.add(addInlineCopyIcon(domainTypeValue));

        userValue.setDisabledTextColor(vaultDnsLabel.getForeground());
        JPanel userPanel = new JPanel(new GridLayout(2, 1));
        userPanel.add(userLabel);
        userPanel.add(addInlineCopyIcon(userValue));

        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.fill = GridBagConstraints.HORIZONTAL;
        formGbc.weightx = 1.0;
        formGbc.gridx = 0;

        formGbc.gridy = 0; formPanel.add(vaultNamePanel, formGbc);
        formGbc.gridy = 1; formPanel.add(vaultDnsPanel, formGbc);
        formGbc.gridy = 2; formPanel.add(vaultIdPanel, formGbc);
        formGbc.gridy = 3; formPanel.add(vaultFamilyPanel, formGbc);
        formGbc.gridy = 4; formPanel.add(vaultApplicationPanel, formGbc);
        formGbc.gridy = 5; formPanel.add(domainTypePanel, formGbc);
        formGbc.gridy = 6; formPanel.add(userPanel, formGbc);

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

        saveBanner = buildSaveBanner();
        saveBanner.setVisible(false);
        this.add(saveBanner, BorderLayout.SOUTH);
    }

    /**
     * Builds the banner for saving or updating credentials.
     *
     * @return The banner JPanel.
     */
    private JPanel buildSaveBanner() {
        bannerMessageLabel = new JLabel();
        bannerLabelField = new JTextField();

        JButton yesButton = new JButton("Yes");
        JButton noButton = new JButton("No");
        yesButton.addActionListener(e -> onBannerYes());
        noButton.addActionListener(e -> hideSaveBanner());

        JPanel askButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        askButtons.setOpaque(false);
        askButtons.add(yesButton);
        askButtons.add(noButton);

        JPanel askCard = new JPanel(new BorderLayout(8, 0));
        askCard.setOpaque(false);
        askCard.add(bannerMessageLabel, BorderLayout.CENTER);
        askCard.add(askButtons, BorderLayout.EAST);

        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        saveButton.addActionListener(e -> onBannerSave());
        cancelButton.addActionListener(e -> hideSaveBanner());

        JPanel labelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        labelButtons.setOpaque(false);
        labelButtons.add(saveButton);
        labelButtons.add(cancelButton);

        JPanel labelLeft = new JPanel(new BorderLayout(6, 0));
        labelLeft.setOpaque(false);
        labelLeft.add(new JLabel("Credential label:"), BorderLayout.WEST);
        labelLeft.add(bannerLabelField, BorderLayout.CENTER);

        JPanel labelCard = new JPanel(new BorderLayout(8, 0));
        labelCard.setOpaque(false);
        labelCard.add(labelLeft, BorderLayout.CENTER);
        labelCard.add(labelButtons, BorderLayout.EAST);

        bannerCards = new JPanel(new CardLayout());
        bannerCards.setOpaque(false);
        bannerCards.add(askCard, "ask");
        bannerCards.add(labelCard, "label");

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(JBUI.Borders.empty(8, 20, 12, 20));
        content.add(bannerCards, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(new JSeparator(), BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Shows the inline save/update credential banner after a successful login.
     *
     * @param pending The credential context returned by {@link LoginPanel}.
     */
    public void showSaveCredentialPrompt(LoginPanel.PendingCredentialSave pending) {
        this.pendingSave = pending;

        if (pending.matchedCred != null) {
            String label = (pending.matchedCred.label != null && !pending.matchedCred.label.isEmpty())
                    ? pending.matchedCred.label : pending.matchedCred.vaultDNS;
            boolean dnsChanged = !pending.vaultDns.equalsIgnoreCase(pending.matchedCred.vaultDNS);
            boolean usernameChanged = pending.isBasic && !pending.username.equals(pending.matchedCred.username);
            if (dnsChanged || usernameChanged) {
                bannerMessageLabel.setText("Update saved credential \"" + label + "\" with these changes?");
            } else {
                bannerMessageLabel.setText("Update saved " + (pending.isBasic ? "password" : "session ID")
                        + " for \"" + label + "\"?");
            }
        } else {
            bannerMessageLabel.setText("Save these credentials?");
            bannerLabelField.setText(pending.vaultDns);
        }

        ((CardLayout) bannerCards.getLayout()).show(bannerCards, "ask");
        saveBanner.setVisible(true);
        revalidate();
    }

    /**
     * Handles the user clicking "Yes" on the save credentials banner.
     */
    private void onBannerYes() {
        if (pendingSave == null) return;

        if (pendingSave.matchedCred != null) {
            final LoginPanel.PendingCredentialSave save = pendingSave;
            save.matchedCred.vaultDNS = save.vaultDns;
            if (save.isBasic) save.matchedCred.username = save.username;
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                if (save.isBasic) {
                    VaultCredentialManager.setUsernamePasswordById(save.matchedCred.id, save.username, save.password);
                } else {
                    VaultCredentialManager.setSessionIdById(save.matchedCred.id, save.sessionId);
                }
            });
            hideSaveBanner();
        } else {
            ((CardLayout) bannerCards.getLayout()).show(bannerCards, "label");
        }
    }

    /**
     * Handles the user clicking "Save" on the credential label input banner.
     */
    private void onBannerSave() {
        if (pendingSave == null) return;

        String name = bannerLabelField.getText().trim();
        if (name.isEmpty()) name = pendingSave.vaultDns;

        AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());

        final String finalName = name;
        SavedCredential duplicate = appState.savedCredentials.stream()
                .filter(c -> finalName.equalsIgnoreCase(c.label))
                .findFirst().orElse(null);

        if (duplicate != null) {
            int choice = javax.swing.JOptionPane.showConfirmDialog(
                    this,
                    "A credential with label \"" + name + "\" already exists. Overwrite it?",
                    "Duplicate Label",
                    javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            if (choice != javax.swing.JOptionPane.YES_OPTION) return;

            final LoginPanel.PendingCredentialSave save = pendingSave;
            duplicate.vaultDNS = save.vaultDns;
            duplicate.authenticationType = save.isBasic
                    ? com.veeva.vault.toolbox.intellij.settings.Vault.AuthenticationType.BASIC
                    : com.veeva.vault.toolbox.intellij.settings.Vault.AuthenticationType.SESSION_ID;
            if (save.isBasic) duplicate.username = save.username;
            final SavedCredential finalDuplicate = duplicate;
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                if (save.isBasic) {
                    VaultCredentialManager.setUsernamePasswordById(finalDuplicate.id, save.username, save.password);
                } else {
                    VaultCredentialManager.setSessionIdById(finalDuplicate.id, save.sessionId);
                }
            });
            hideSaveBanner();
            return;
        }

        SavedCredential newCred = new SavedCredential();
        newCred.label = name;
        newCred.vaultDNS = pendingSave.vaultDns;
        newCred.authenticationType = pendingSave.isBasic
                ? com.veeva.vault.toolbox.intellij.settings.Vault.AuthenticationType.BASIC
                : com.veeva.vault.toolbox.intellij.settings.Vault.AuthenticationType.SESSION_ID;
        if (pendingSave.isBasic) newCred.username = pendingSave.username;

        appState.savedCredentials.add(newCred);

        final LoginPanel.PendingCredentialSave save = pendingSave;
        final SavedCredential finalNewCred = newCred;
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (save.isBasic) {
                VaultCredentialManager.setUsernamePasswordById(finalNewCred.id, save.username, save.password);
            } else {
                VaultCredentialManager.setSessionIdById(finalNewCred.id, save.sessionId);
            }
        });

        hideSaveBanner();
    }

    /**
     * Hides the save credentials banner.
     */
    private void hideSaveBanner() {
        pendingSave = null;
        saveBanner.setVisible(false);
        revalidate();
    }

    /**
     * Adds a clipboard copy icon to the end of a text field.
     *
     * @param field The text field to decorate.
     * @return The decorated text field.
     */
    private JTextField addInlineCopyIcon(JTextField field) {
        field.setEditable(false);
        field.setBackground(com.intellij.util.ui.UIUtil.getPanelBackground());
        field.setForeground(vaultDnsLabel.getForeground());

        JLabel copyIcon = new JLabel(AllIcons.Actions.Copy);
        copyIcon.setToolTipText("Copy to clipboard");
        copyIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));

        copyIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                String text = field.getText();
                if (text != null && !text.isEmpty()) {
                    StringSelection selection = new StringSelection(text);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

                    if (activeCopyIcon != null && activeCopyIcon != copyIcon) {
                        activeCopyIcon.setIcon(AllIcons.Actions.Copy);
                        activeCopyIcon.setToolTipText("Copy to clipboard");
                    }

                    copyIcon.setIcon(AllIcons.Actions.Checked);
                    copyIcon.setToolTipText("Copied!");
                    activeCopyIcon = copyIcon;

                    if (revertTimer != null && revertTimer.isRunning()) {
                        revertTimer.stop();
                    }
                    revertTimer = new javax.swing.Timer(3000, ev -> {
                        if (activeCopyIcon != null) {
                            activeCopyIcon.setIcon(AllIcons.Actions.Copy);
                            activeCopyIcon.setToolTipText("Copy to clipboard");
                            activeCopyIcon = null;
                        }
                    });
                    revertTimer.setRepeats(false);
                    revertTimer.start();
                }
            }
        });

        Border currentBorder = field.getBorder();
        field.setBorder(BorderFactory.createCompoundBorder(currentBorder, JBUI.Borders.emptyRight(28)));

        field.setLayout(null);
        field.add(copyIcon);

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

    /**
     * Refreshes the vault information displayed in the panel using the current project context.
     */
    public void refreshVaultInfo() {
        vaultNameValue.setText("");
        vaultDnsValue.setText("");
        vaultIdValue.setText("");
        vaultFamilyValue.setText("");
        vaultApplicationValue.setText("");
        domainTypeValue.setText("");
        userValue.setText("");

        if (activeCopyIcon != null) {
            activeCopyIcon.setIcon(AllIcons.Actions.Copy);
            activeCopyIcon.setToolTipText("Copy to clipboard");
            activeCopyIcon = null;
        }

        hideSaveBanner();

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
