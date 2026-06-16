package com.veeva.vault.toolbox.core.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Represents the source metadata of a Vault package, identifying the originating
 * Vault and the package author.
 */
public class Source {

	@JacksonXmlProperty(localName = "vault")
	private String vault;

	@JacksonXmlProperty(localName = "author")
	private String author;

	/**
	 * Returns the Vault identifier from which this package originated.
	 *
	 * @return the Vault identifier
	 */
	@JsonGetter
	public String getVault() {
		return vault;
	}

	/**
	 * Sets the Vault identifier from which this package originated.
	 *
	 * @param vault the Vault identifier
	 */
	@JsonSetter
	public void setVault(String vault) {
		this.vault = vault;
	}

	/**
	 * Returns the author of this Vault package.
	 *
	 * @return the author
	 */
	@JsonGetter
	public String getAuthor() {
		return author;
	}

	/**
	 * Sets the author of this Vault package.
	 *
	 * @param author the author
	 */
	@JsonSetter
	public void setAuthor(String author) {
		this.author = author;
	}
}
