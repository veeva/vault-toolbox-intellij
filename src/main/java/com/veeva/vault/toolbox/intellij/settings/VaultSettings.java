package com.veeva.vault.toolbox.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Project-level persistent state component that tracks the set of {@link Vault}
 * instances configured for the project, keyed by their Vault DNS.
 */
@State(
		name = "com.veeva.vault.toolbox.intellij.settings.VaultSettings",
		storages = @Storage("veeva-vault.xml")
)
public final class VaultSettings implements PersistentStateComponent<VaultSettings> {

	private Map<String, Vault> vaults = new HashMap<>();

	/**
	 * Returns the {@link VaultSettings} service instance for the given project.
	 *
	 * @param project the project whose settings are requested
	 * @return the project-scoped {@link VaultSettings} service
	 */
	@Nullable
	public static VaultSettings getInstance(Project project) {
		return project.getService(VaultSettings.class);
	}

	/**
	 * @return the map of configured vaults keyed by Vault DNS
	 */
	public Map<String, Vault> getVaults() {
		return vaults;
	}

	/**
	 * Replaces the configured vaults map.
	 *
	 * @param vaults the new map of vaults keyed by Vault DNS
	 */
	public void setVaults(Map<String, Vault> vaults) {
		this.vaults = vaults;
	}

	/**
	 * @return the currently active vault, or {@code null} if no vault is marked active
	 */
	public Vault getActiveVault() {
		return vaults.values().stream()
				.filter(v -> v != null && v.isActive())
				.findFirst()
				.orElse(null);
	}

	/**
	 * Adds or updates a vault entry. If the supplied vault is active, any other
	 * vaults previously marked active are deactivated to enforce a single active vault.
	 *
	 * @param vault the vault to add or update
	 */
	public void addVault(Vault vault) {
		if (vault.isActive()) {
			vaults.values().stream()
					.filter(v -> v != null && v.isActive() && !v.getVaultDNS().equals(vault.getVaultDNS()))
					.forEach(v -> v.setActive(false));
		}
		vaults.put(vault.getVaultDNS(), vault);
	}

	/**
	 * @param vaultDNS the Vault DNS to look up
	 * @return {@code true} if a vault with the given DNS is configured
	 */
	public boolean containsVault(String vaultDNS) {
		return vaults.containsKey(vaultDNS);
	}

	/**
	 * @param vaultDNS the Vault DNS to look up
	 * @return the vault associated with the given DNS, or {@code null} if not found
	 */
	public Vault getVault(String vaultDNS) {
		return vaultDNS != null ? vaults.get(vaultDNS) : null;
	}

	/**
	 * Removes the given vault from the configured set.
	 *
	 * @param vault the vault to remove
	 */
	public void removeVault(Vault vault) {
		vaults.remove(vault.getVaultDNS());
	}

	@Nullable
	@Override
	public VaultSettings getState() {
		return this;
	}

	@Override
	public void loadState(VaultSettings vaultSettings) {
		XmlSerializerUtil.copyBean(vaultSettings, this);
	}
}
