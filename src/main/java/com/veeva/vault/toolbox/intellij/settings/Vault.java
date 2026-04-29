package com.veeva.vault.toolbox.intellij.settings;

public class Vault {
		AuthenticationType authenticationType;
		boolean active;
		boolean saveSecret;
		String vaultDNS;

		public enum AuthenticationType {
			BASIC("BASIC"),
			SESSION_ID("SESSION_ID");

			String typeName;

			AuthenticationType(String typeName) {
				this.typeName = typeName;
			}

			public String getTypeName() {
				return typeName;
			}
		}

		public Vault() {

		}

		public Vault(AuthenticationType authenticationType, String vaultDNS, boolean saveSecret, boolean active) {
			this.vaultDNS = vaultDNS;
			this.authenticationType = authenticationType;
			this.saveSecret = saveSecret;
			this.active = active;
		}

		public boolean isActive() { return active; }
		public void setActive(boolean active) { this.active = active; }

		public boolean getSaveSecret() { return saveSecret; }
		public void setSaveSecret(boolean saveSecret) { this.saveSecret = saveSecret; }

		public AuthenticationType getAuthenticationType() { return authenticationType; }

		public void setAuthenticationType(AuthenticationType authenticationType) {
			this.authenticationType = authenticationType;
		}

		public String getVaultDNS() {
			return vaultDNS;
		}
		public void setVaultDNS(String vaultDNS) {
			this.vaultDNS = vaultDNS;
		}
	}