package com.veeva.vault.toolbox.intellij.credentials;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.veeva.vault.vapil.api.model.VaultModel;

public  class BasicAuth extends VaultModel {

	public BasicAuth() {

	}

	public BasicAuth(String username, String password) {
		setUsername(username);
		setPassword(password);
	}

	@JsonProperty("password")
	@JsonAlias({"vault.password"})
	public String getPassword() {
		return this.getString("password");
	}
	public void setPassword(String username) {
		this.set("password", username);
	}

	@JsonProperty("username")
	@JsonAlias("vault.username")
	public String getUsername() {
		return this.getString("username");
	}
	public void setUsername(String username) {
		this.set("username", username);
	}
}