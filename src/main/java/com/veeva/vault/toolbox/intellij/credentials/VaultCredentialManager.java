package com.veeva.vault.toolbox.intellij.credentials;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

public class VaultCredentialManager {

    static private CredentialAttributes createCredentialAttributes(String key) {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("VaultToolbox", key)
        );
    }

    public static BasicAuth getUsernamePassword(String vaultDNS) {
        CredentialAttributes attributes = createCredentialAttributes(getBasicAuthKey(vaultDNS));
        PasswordSafe passwordSafe = PasswordSafe.getInstance();
        Credentials credentials = passwordSafe.get(attributes);
        if (credentials != null) {
            return new BasicAuth(
                    credentials.getUserName(),
                    credentials.getPasswordAsString()
            );
        }
        return null;
    }

    public static void setUsernamePassword(String vaultDNS, String username, String password) {
        CredentialAttributes credentialAttributes = createCredentialAttributes(getBasicAuthKey(vaultDNS));
        Credentials credentials = new Credentials(username, password);
        PasswordSafe.getInstance().set(credentialAttributes, credentials);
    }

    public static String getSessionId(String vaultDNS) {
        CredentialAttributes attributes = createCredentialAttributes(getSessionKey(vaultDNS));
        return PasswordSafe.getInstance().getPassword(attributes);
    }

    public static void setSessionId(String vaultDNS, String sessionId) {
        CredentialAttributes attributes = createCredentialAttributes(getSessionKey(vaultDNS));
        PasswordSafe.getInstance().setPassword(attributes, sessionId);
    }

    static private String getBasicAuthKey(String vaultDNS) {
        return vaultDNS + ".BasicAuth";
    }

    static private String getSessionKey(String vaultDNS) {
        return vaultDNS + ".SessionId";
    }
}