package com.veeva.vault.toolbox.intellij.credentials;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

/**
 * Manager for handling Vault credentials using IntelliJ's PasswordSafe.
 * This class provides methods to securely store and retrieve usernames, passwords, and session IDs.
 */
public class VaultCredentialManager {

    /**
     * Creates IntelliJ CredentialAttributes for a given key.
     *
     * @param key the key identifying the credential
     * @return the CredentialAttributes
     */
    private static CredentialAttributes createCredentialAttributes(String key) {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("VaultToolbox", key)
        );
    }

    /**
     * Retrieves username and password credentials for a saved credential by its ID.
     *
     * @param credentialId the unique ID of the saved credential
     * @return a BasicAuth object, or null if not found
     */
    public static BasicAuth getUsernamePasswordById(String credentialId) {
        CredentialAttributes attributes = createCredentialAttributes(getBasicAuthKeyById(credentialId));
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        if (credentials != null) {
            return new BasicAuth(credentials.getUserName(), credentials.getPasswordAsString());
        }
        return null;
    }

    /**
     * Stores username and password credentials for a saved credential by its ID.
     *
     * @param credentialId the unique ID of the saved credential
     * @param username     the Vault username
     * @param password     the Vault password
     */
    public static void setUsernamePasswordById(String credentialId, String username, String password) {
        CredentialAttributes attributes = createCredentialAttributes(getBasicAuthKeyById(credentialId));
        PasswordSafe.getInstance().set(attributes, new Credentials(username, password));
    }

    /**
     * Retrieves a stored session ID for a saved credential by its ID.
     *
     * @param credentialId the unique ID of the saved credential
     * @return the session ID, or null if not found
     */
    public static String getSessionIdById(String credentialId) {
        return PasswordSafe.getInstance().getPassword(
                createCredentialAttributes(getSessionKeyById(credentialId)));
    }

    /**
     * Stores a session ID for a saved credential by its ID.
     *
     * @param credentialId the unique ID of the saved credential
     * @param sessionId    the session ID to store
     */
    public static void setSessionIdById(String credentialId, String sessionId) {
        PasswordSafe.getInstance().setPassword(
                createCredentialAttributes(getSessionKeyById(credentialId)), sessionId);
    }

    /**
     * Removes all PasswordSafe entries associated with a saved credential ID.
     *
     * @param credentialId the unique ID of the saved credential
     */
    public static void deleteCredentialById(String credentialId) {
        PasswordSafe.getInstance().set(createCredentialAttributes(getBasicAuthKeyById(credentialId)), null);
        PasswordSafe.getInstance().set(createCredentialAttributes(getSessionKeyById(credentialId)), null);
    }

    /**
     * Generates the key used for basic authentication credentials by ID.
     *
     * @param credentialId the unique ID of the saved credential
     * @return the formatted key
     */
    private static String getBasicAuthKeyById(String credentialId) {
        return "cred." + credentialId + ".BasicAuth";
    }

    /**
     * Generates the key used for session ID storage by ID.
     *
     * @param credentialId the unique ID of the saved credential
     * @return the formatted key
     */
    private static String getSessionKeyById(String credentialId) {
        return "cred." + credentialId + ".SessionId";
    }
}
