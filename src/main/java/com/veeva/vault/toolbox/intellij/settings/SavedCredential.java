package com.veeva.vault.toolbox.intellij.settings;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a saved credential for authenticating with a Veeva Vault.
 */
public class SavedCredential {
    public String id = UUID.randomUUID().toString();
    public String label = "";
    public String vaultDNS = "";
    public String username = "";
    public Vault.AuthenticationType authenticationType = Vault.AuthenticationType.BASIC;
    public boolean saveSecret = false;
    public boolean isDefault = false;

    /**
     * Compares this credential to the specified object for equality.
     *
     * @param obj the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SavedCredential)) return false;
        SavedCredential other = (SavedCredential) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(label, other.label)
                && Objects.equals(vaultDNS, other.vaultDNS)
                && Objects.equals(username, other.username)
                && authenticationType == other.authenticationType
                && saveSecret == other.saveSecret
                && isDefault == other.isDefault;
    }

    /**
     * Returns a hash code value for this credential.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, label, vaultDNS, username, authenticationType, saveSecret, isDefault);
    }
}
