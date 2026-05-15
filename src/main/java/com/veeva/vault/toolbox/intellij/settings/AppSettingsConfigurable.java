package com.veeva.vault.toolbox.intellij.settings;

import com.intellij.openapi.options.Configurable;
import com.veeva.vault.toolbox.intellij.ui.AppSettingsControl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * IntelliJ {@link Configurable} that exposes the Vault Toolbox application
 * settings page in the IDE Settings dialog and bridges UI state in
 * {@link AppSettingsControl} with persisted state in {@link AppSettings}.
 *
 * <p>This class is registered as an {@code applicationConfigurable} extension
 * and therefore must provide a public no-argument constructor (the implicit
 * default constructor satisfies that requirement).
 */
public final class AppSettingsConfigurable implements Configurable {

    private AppSettingsControl appSettingsComponent;

    /**
     * Gets the display name of the configurable in the settings dialog.
     *
     * @return the display name
     */
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Vault Toolbox";
    }

    /**
     * Gets the component that should receive focus when the configurable is opened.
     *
     * @return the preferred focused component
     */
    @Override
    public JComponent getPreferredFocusedComponent() {
        return appSettingsComponent.getPreferredFocusedComponent();
    }

    /**
     * Creates the UI component for the configurable.
     *
     * @return the created JComponent
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        appSettingsComponent = new AppSettingsControl();
        return appSettingsComponent.getPanel();
    }

    /**
     * Checks if the settings have been modified.
     *
     * @return true if modified, false otherwise
     */
    @Override
    public boolean isModified() {
        AppSettings.AppState state = Objects.requireNonNull(AppSettings.getInstance().getState());
        return appSettingsComponent.getAutoConnectField() != state.autoConnect
                || !appSettingsComponent.getVaultDns().equals(state.vaultDNS)
                || !appSettingsComponent.getAuthenticationType().equals(state.authenticationType)
                || !appSettingsComponent.getUsername().equals(state.username)
                || appSettingsComponent.getCsvMaxRows() != state.csvMaxRows
                || appSettingsComponent.getConnectionTimeout() != state.connectionTimeout
                || appSettingsComponent.getAllowAllCertificates() != state.allowAllCertificates
                || !appSettingsComponent.getSavedCredentials().equals(state.savedCredentials);
    }

    /**
     * Applies the modified settings.
     */
    @Override
    public void apply() {
        AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());

        if (appSettingsComponent.getAllowAllCertificates() != appState.allowAllCertificates) {
            AppSettings.requireRestart = true;
            JOptionPane.showMessageDialog(
                    appSettingsComponent.getPanel(),
                    "You have made changes that requires a restart of IntelliJ to take effect",
                    "Restart IntelliJ",
                    JOptionPane.WARNING_MESSAGE);
        }

        appState.autoConnect = appSettingsComponent.getAutoConnectField();
        appState.vaultDNS = appSettingsComponent.getVaultDns();
        appState.authenticationType = appSettingsComponent.getAuthenticationType();
        appState.username = appSettingsComponent.getUsername();
        appState.csvMaxRows = appSettingsComponent.getCsvMaxRows();
        appState.connectionTimeout = appSettingsComponent.getConnectionTimeout();
        appState.allowAllCertificates = appSettingsComponent.getAllowAllCertificates();
        appState.savedCredentials = new java.util.ArrayList<>(appSettingsComponent.getSavedCredentials());
    }

    /**
     * Resets the settings to their saved values.
     */
    @Override
    public void reset() {
        AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());
        appSettingsComponent.setAutoConnectField(appState.autoConnect);
        appSettingsComponent.setVaultDns(appState.vaultDNS);
        appSettingsComponent.setAuthenticationType(appState.authenticationType);
        appSettingsComponent.setUsername(appState.username);
        appSettingsComponent.setCsvMaxRows(appState.csvMaxRows);
        appSettingsComponent.setConnectionTimeout(appState.connectionTimeout);
        appSettingsComponent.setAllowAllCertificates(appState.allowAllCertificates);
        appSettingsComponent.setSavedCredentials(new java.util.ArrayList<>(appState.savedCredentials));
    }

    /**
     * Disposes of the UI resources.
     */
    @Override
    public void disposeUIResources() {
        appSettingsComponent = null;
    }
}
