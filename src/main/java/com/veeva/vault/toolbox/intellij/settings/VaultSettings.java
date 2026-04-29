// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.veeva.vault.toolbox.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/*
 * Supports storing the application settings in a persistent way.
 * The {@link com.intellij.openapi.components.State State} and {@link Storage}
 * annotations define the name of the data and the filename where these persistent
 * application settings are stored.
 */

@State(
		name = "com.veeva.vault.toolbox.intellij.settings.VaultSettings",
		storages = @Storage("veeva-vault.xml")
)
public final class VaultSettings implements PersistentStateComponent<VaultSettings> {
	private static final Logger log = LoggerFactory.getLogger(VaultSettings.class);
	private String currentVaultDNS;

	public Map<String, Vault> getVaults() {
		return vaults;
	}

	public void setVaults(Map<String, Vault> vaults) {
		this.vaults = vaults;
	}

	private Map<String, Vault> vaults = new HashMap<>();


	public Vault getActiveVault() {
		Map.Entry<String, Vault> vaultEntry = vaults.entrySet().stream().filter(e -> e.getValue() != null && e.getValue().isActive())
				.findFirst().orElse(null);
		if (vaultEntry != null) {
			return vaultEntry.getValue();
		}
		return null;
	}

	public void addVault(Vault vault) {
		if (vault.isActive()) {
			vaults.entrySet().stream()
					.filter(e -> e.getValue() != null && e.getValue().isActive() && !e.getValue().getVaultDNS().equals(vault.getVaultDNS()))
					.forEach(e ->
			{
				e.getValue().setActive(false);
			});
		}
		this.vaults.put(vault.getVaultDNS(), vault);
	}

	public boolean containsVault(String vaultDNS) {
		return this.vaults.containsKey(vaultDNS);
	}

	public Vault getVault(String vaultDNS) {
		if (vaultDNS != null) {
			return this.vaults.get(vaultDNS);
		}
		else return null;
	}

	public void removeVault(Vault vault) {
		this.vaults.remove(vault.getVaultDNS());
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
	@Nullable
	public static VaultSettings getInstance(Project project) {
		return project.getService(VaultSettings.class);
	}
}
