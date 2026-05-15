package com.veeva.vault.toolbox.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Application-level persistent state component that stores user preferences
 * for the Vault Toolbox plugin (default Vault DNS, authentication type,
 * timeouts, and similar global settings).
 */
@State(
        name = "com.veeva.vault.toolbox.intellij.settings.AppSettings",
        storages = @Storage("VaultToolbox.xml")
)
public final class AppSettings implements PersistentStateComponent<AppSettings.AppState> {

    /**
     * Indicates that a setting requiring an IDE restart has been changed during
     * the current session. Consumers may surface a restart prompt to the user.
     */
    public static boolean requireRestart = false;

    /**
     * Serializable container for the application-level settings.
     */
    public static class AppState {
        public boolean autoConnect = false;
        public String vaultDNS = "";
        public String username = "";
        public boolean allowAllCertificates = false;
        public int csvMaxRows = 100;
        public Vault.AuthenticationType authenticationType = Vault.AuthenticationType.BASIC;
        public int connectionTimeout = 15;
        public List<SavedCredential> savedCredentials = new ArrayList<>();
    }

    private AppState state = new AppState();

    /**
     * @return the application-wide {@link AppSettings} service instance
     */
    public static AppSettings getInstance() {
        return ApplicationManager.getApplication().getService(AppSettings.class);
    }

    /**
     * Gets the current state of the application settings.
     *
     * @return the current AppState
     */
    @Override
    public AppState getState() {
        return state;
    }

    /**
     * Loads the given state into the application settings.
     *
     * @param state the state to load
     */
    @Override
    public void loadState(@NotNull AppState state) {
        this.state = state;
    }
}
