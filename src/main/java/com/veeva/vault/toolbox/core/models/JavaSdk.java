/*---------------------------------------------------------------------
 *	Copyright (c) 2020 Veeva Systems Inc.  All Rights Reserved.
 *	This code is based on pre-existing content developed and
 *	owned by Veeva Systems Inc. and may only be used in connection
 *	with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.toolbox.core.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.HashMap;
import java.util.Map;

public class JavaSdk {
	public enum DeploymentOption {
		NONE("none"),
		DELETE_ALL("delete_all"),
		INCREMENTAL("incremental"),
		REPLACE_ALL("replace_all");

		String value;
		DeploymentOption(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	@JacksonXmlProperty(localName = "deployment_option")
	private String deploymentOption;

	@JsonGetter
	public String getDeploymentOption() {
		return deploymentOption;
	}

	@JsonAnySetter
	public JavaSdk setDeploymentOption(String deploymentOption) {
		this.deploymentOption = deploymentOption;
		return this;
	}

	@JsonIgnore
	public void setDeploymentOption(DeploymentOption deploymentOption) {
		this.deploymentOption = deploymentOption.getValue();
	}

	@JsonAnySetter
	private Map<String, Object> properties = new HashMap<String, Object>();

	@JsonAnyGetter
	public Map<String, Object> getProperties() {
		return properties;
	}

	@JsonAnySetter
	public void setProperties(String name, Object value) {
		this.properties.put(name, value);
	}
}
