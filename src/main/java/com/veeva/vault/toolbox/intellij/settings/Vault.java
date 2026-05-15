package com.veeva.vault.toolbox.intellij.settings;

/**
 * Represents a single Vault connection profile, including its DNS, the
 * authentication mechanism to use, and whether the profile is currently active.
 */
public class Vault {

	private AuthenticationType authenticationType;
	private boolean active;
	private boolean saveSecret;
	private String vaultDNS;

	/**
	 * Authentication mechanisms supported when connecting to a Vault instance.
	 */
	public enum AuthenticationType {
		BASIC("BASIC"),
		SESSION_ID("SESSION_ID");

		private final String typeName;

		AuthenticationType(String typeName) {
			this.typeName = typeName;
		}

		/**
		 * @return the canonical string representation of this authentication type
		 */
		public String getTypeName() {
			return typeName;
		}
	}

	public Vault() {
	}

	/**
	 * Creates a fully populated Vault profile.
	 *
	 * @param authenticationType the authentication mechanism for this profile
	 * @param vaultDNS           the Vault DNS this profile connects to
	 * @param saveSecret         whether the credential secret should be persisted
	 * @param active             whether this profile is the currently active one
	 */
	public Vault(AuthenticationType authenticationType, String vaultDNS, boolean saveSecret, boolean active) {
		this.vaultDNS = vaultDNS;
		this.authenticationType = authenticationType;
		this.saveSecret = saveSecret;
		this.active = active;
	}

	/**
	 * @return {@code true} if this is the currently active Vault profile
	 */
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @return {@code true} if the credential secret should be persisted for this profile
	 */
	public boolean getSaveSecret() {
		return saveSecret;
	}

	public void setSaveSecret(boolean saveSecret) {
		this.saveSecret = saveSecret;
	}

	/**
	 * @return the authentication mechanism configured for this profile
	 */
	public AuthenticationType getAuthenticationType() {
		return authenticationType;
	}

	public void setAuthenticationType(AuthenticationType authenticationType) {
		this.authenticationType = authenticationType;
	}

	/**
	 * @return the Vault DNS this profile connects to
	 */
	public String getVaultDNS() {
		return vaultDNS;
	}

	public void setVaultDNS(String vaultDNS) {
		this.vaultDNS = vaultDNS;
	}
}
