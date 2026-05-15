package com.veeva.vault.toolbox.intellij.credentials;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.veeva.vault.vapil.api.model.VaultModel;

/**
 * Model class for Vault Basic Authentication credentials.
 * Extends {@link VaultModel} for compatibility with Vault API responses.
 */
public class BasicAuth extends VaultModel {

    /**
     * Default constructor.
     */
    public BasicAuth() {
    }

    /**
     * Constructs a BasicAuth object with the specified username and password.
     *
     * @param username the Vault username
     * @param password the Vault password
     */
    public BasicAuth(String username, String password) {
        setUsername(username);
        setPassword(password);
    }

    /**
     * Gets the Vault password.
     *
     * @return the password
     */
    @JsonProperty("password")
    @JsonAlias({"vault.password"})
    public String getPassword() {
        return this.getString("password");
    }

    /**
     * Sets the Vault password.
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.set("password", password);
    }

    /**
     * Gets the Vault username.
     *
     * @return the username
     */
    @JsonProperty("username")
    @JsonAlias("vault.username")
    public String getUsername() {
        return this.getString("username");
    }

    /**
     * Sets the Vault username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.set("username", username);
    }
}
