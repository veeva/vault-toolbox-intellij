// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import com.veeva.vault.toolbox.intellij.settings.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class AppSettingsControl {

    private static final Logger logger = LoggerFactory.getLogger(AppSettingsControl.class);
    private final JPanel myMainPanel;
    private final JBCheckBox autoConnectField = new JBCheckBox("Auto Connect to Vault");
    private final JBTextField vaultDnsField = new JBTextField();
    private final JBTextField usernameField = new JBTextField();
    private final JBTextField csvMaxRowsField = new JBTextField();

    // Added the JSpinner for Connection Timeout (Default: 15, Min: 1, Max: 300, Step: 1)
    private final JSpinner connectionTimeoutField = new JSpinner(new SpinnerNumberModel(15, 1, 300, 1));

    private final JBRadioButton basicAuthField = new JBRadioButton("Basic");
    private final JBRadioButton sessionIdField = new JBRadioButton("Session");
    private final JBCheckBox saveSecretField = new JBCheckBox("Save Password");
    private final JBPanel authenticationTypePanel = new JBPanel();
    private final ButtonGroup authGroup = new ButtonGroup();
    private final JBCheckBox allowAllCertificatesField = new JBCheckBox("Allow All Certificates");

    public AppSettingsControl() {
        authGroup.add(basicAuthField);
        authGroup.add(sessionIdField);

        authenticationTypePanel.setLayout(new GridLayout(1, 2));
        authenticationTypePanel.add(basicAuthField);
        authenticationTypePanel.add(sessionIdField);

        basicAuthField.addActionListener(e -> changeAuthenticationType());
        sessionIdField.addActionListener(e -> changeAuthenticationType());

        myMainPanel = FormBuilder.createFormBuilder()
                .addComponent(autoConnectField, 1)
                .addComponent(allowAllCertificatesField, 1)
                .addLabeledComponent(new JBLabel("Default Vault DNS"), vaultDnsField, 1, false)
                .addLabeledComponent(new JBLabel("Default Authentication Type"), authenticationTypePanel )
                .addLabeledComponent(new JBLabel("Default Username"), usernameField, 1, false )
                .addLabeledComponent(new JBLabel("CSV Max Rows"), csvMaxRowsField, 1, false )
                // Injected the new Connection Timeout field into the layout
                .addLabeledComponent(new JBLabel("Connection Timeout (seconds)"), connectionTimeoutField, 1, false )
                .addComponent(saveSecretField, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return myMainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return vaultDnsField;
    }

    public boolean getSaveSecret() {
        return saveSecretField.isSelected();
    }

    public void setSaveSecret(boolean saveSecret) {saveSecretField.setSelected(saveSecret); }

    public String getUsername() { return usernameField.getText(); }

    public void settUsername(String username) {usernameField.setText(username);}

    public int getCsvMaxRows() {
        try {
            return Integer.parseInt(csvMaxRowsField.getText());
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    public void setCsvMaxRows(int csvMaxRows) {
        csvMaxRowsField.setText(String.valueOf(csvMaxRows));
    }

    // --- New Getter and Setter for Connection Timeout ---
    public int getConnectionTimeout() {
        return (Integer) connectionTimeoutField.getValue();
    }

    public void setConnectionTimeout(int connectionTimeout) {
        connectionTimeoutField.setValue(connectionTimeout);
    }
    // ----------------------------------------------------

    public String getVaultDns() { return vaultDnsField.getText(); }

    public void setVaultDns(String vaultDNS) {
        vaultDnsField.setText(vaultDNS);
    }


    public boolean getAutoConnectField() { return autoConnectField.isSelected(); }

    public void setAutoConnectField(boolean autuoConnect) { autoConnectField.setSelected(autuoConnect); }

    public boolean getAllowAllCertificates() { return allowAllCertificatesField.isSelected(); }

    public void setAllowAllCertificates(boolean allowAllCertificates) { allowAllCertificatesField.setSelected(allowAllCertificates); }


    public Vault.AuthenticationType getAuthenticationType() {
        if (basicAuthField.isSelected()) {
            return Vault.AuthenticationType.BASIC;
        }
        else if (sessionIdField.isSelected()) {
            return Vault.AuthenticationType.SESSION_ID;
        }
        return Vault.AuthenticationType.BASIC;
    }

    public void setAuthenticationType(Vault.AuthenticationType authenticationType) {
        if (authenticationType == null || authenticationType == Vault.AuthenticationType.BASIC) {
            basicAuthField.setSelected(true);
        }
        else if (authenticationType == Vault.AuthenticationType.SESSION_ID) {
            sessionIdField.setSelected(true);
        }
        changeAuthenticationType();
    }

    private void changeAuthenticationType() {
        if (basicAuthField.isSelected()) {
            usernameField.setEnabled(true);
            usernameField.setVisible(true);
            saveSecretField.setEnabled(true);
            saveSecretField.setText("Save Password");
        }
        else if (sessionIdField.isSelected()) {
            usernameField.setEnabled(false);
            usernameField.setText("");
            usernameField.setVisible(false);
            saveSecretField.setEnabled(true);
            saveSecretField.setText("Save Session ID");
        }
    }
}