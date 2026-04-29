/*---------------------------------------------------------------------
 *	Copyright (c) 2020 Veeva Systems Inc.  All Rights Reserved.
 *	This code is based on pre-existing content developed and
 *	owned by Veeva Systems Inc. and may only be used in connection
 *	with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.toolbox.core.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Source {

	@JacksonXmlProperty(localName = "vault")
	private String vault;

	@JacksonXmlProperty(localName = "author")
	private String author;

	@JsonGetter
	public String getVault() {
		return vault;
	}

	@JsonSetter
	public void setVault(String vault) {
		this.vault = vault;
	}

	@JsonGetter
	public String getAuthor() {
		return author;
	}

	@JsonSetter
	public void setAuthor(String author) {
		this.author = author;
	}

}
