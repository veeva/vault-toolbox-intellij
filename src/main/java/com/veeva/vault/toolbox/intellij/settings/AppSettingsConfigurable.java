// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.veeva.vault.toolbox.intellij.settings;

import com.intellij.openapi.options.Configurable;
import com.veeva.vault.toolbox.intellij.ui.AppSettingsControl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * Provides controller functionality for application settings.
 */
final class AppSettingsConfigurable implements Configurable {

    private AppSettingsControl appSettingsComponent;

    // A default constructor with no arguments is required because
    // this implementation is registered as an applicationConfigurable

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Vault Toolbox";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return appSettingsComponent.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        appSettingsComponent = new AppSettingsControl();
        return appSettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        AppSettings.AppState state =
                Objects.requireNonNull(AppSettings.getInstance().getState());
        return appSettingsComponent.getAutoConnectField() != state.autoConnect
                || !appSettingsComponent.getVaultDns().equals(state.vaultDNS)
                || !appSettingsComponent.getAuthenticationType().equals(state.authenticationType)
                || !appSettingsComponent.getUsername().equals(state.username)
                || appSettingsComponent.getCsvMaxRows() != state.csvMaxRows
                || appSettingsComponent.getConnectionTimeout() != state.connectionTimeout
                || appSettingsComponent.getSaveSecret() != state.saveSecret
                || appSettingsComponent.getAllowAllCertificates() != state.allowAllCertificates;
    }

    @Override
    public void apply() {
        AppSettings.AppState appState =
                Objects.requireNonNull(AppSettings.getInstance().getState());

        if (appSettingsComponent.getAllowAllCertificates() != appState.allowAllCertificates) {
            AppSettings.requireRestart = true;
            JOptionPane.showMessageDialog(appSettingsComponent.getPanel(), "You have made changes that requires a restart of IntelliJ to take effect", "Restart IntelliJ", JOptionPane.WARNING_MESSAGE);
        }

        appState.autoConnect = appSettingsComponent.getAutoConnectField();
        appState.vaultDNS = appSettingsComponent.getVaultDns();
        appState.authenticationType = appSettingsComponent.getAuthenticationType();
        appState.username = appSettingsComponent.getUsername();
        appState.csvMaxRows = appSettingsComponent.getCsvMaxRows();
        appState.connectionTimeout = appSettingsComponent.getConnectionTimeout();
        appState.saveSecret = appSettingsComponent.getSaveSecret();
        appState.allowAllCertificates = appSettingsComponent.getAllowAllCertificates();
    }

    @Override
    public void reset() {
        AppSettings.AppState appState =
                Objects.requireNonNull(AppSettings.getInstance().getState());
        appSettingsComponent.setAutoConnectField(appState.autoConnect);
        appSettingsComponent.setVaultDns(appState.vaultDNS);
        appSettingsComponent.setAuthenticationType(appState.authenticationType);
        appSettingsComponent.settUsername(appState.username);
        appSettingsComponent.setCsvMaxRows(appState.csvMaxRows);
        appSettingsComponent.setConnectionTimeout(appState.connectionTimeout);
        appSettingsComponent.setSaveSecret(appState.saveSecret);
        appSettingsComponent.setAllowAllCertificates(appState.allowAllCertificates);
    }

    @Override
    public void disposeUIResources() {
        appSettingsComponent = null;
    }
}