package com.veeva.vault.toolbox.core.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the Java SDK configuration section of a Vault package manifest.
 */
public class JavaSdk {

	/**
	 * Defines the deployment strategy for Java SDK components within a Vault package.
	 */
	public enum DeploymentOption {
		NONE("none"),
		DELETE_ALL("delete_all"),
		INCREMENTAL("incremental"),
		REPLACE_ALL("replace_all");

		private final String value;

		DeploymentOption(String value) {
			this.value = value;
		}

		/**
		 * Returns the string value of this deployment option as used in the manifest XML.
		 *
		 * @return the manifest string value
		 */
		public String getValue() {
			return value;
		}
	}

	@JacksonXmlProperty(localName = "deployment_option")
	private String deploymentOption;

	/**
	 * Returns the deployment option string value.
	 *
	 * @return the deployment option
	 */
	@JsonGetter
	public String getDeploymentOption() {
		return deploymentOption;
	}

	/**
	 * Sets the deployment option using its raw string value.
	 *
	 * @param deploymentOption the deployment option string
	 * @return this instance for chaining
	 */
	@JsonAnySetter
	public JavaSdk setDeploymentOption(String deploymentOption) {
		this.deploymentOption = deploymentOption;
		return this;
	}

	/**
	 * Sets the deployment option using the {@link DeploymentOption} enum.
	 *
	 * @param deploymentOption the deployment option enum value
	 */
	@JsonIgnore
	public void setDeploymentOption(DeploymentOption deploymentOption) {
		this.deploymentOption = deploymentOption.getValue();
	}

	private final Map<String, Object> properties = new HashMap<>();

	/**
	 * Returns additional properties not explicitly mapped in this model.
	 *
	 * @return a map of additional properties
	 */
	@JsonAnyGetter
	public Map<String, Object> getProperties() {
		return properties;
	}

	/**
	 * Sets an additional property not explicitly mapped in this model.
	 *
	 * @param name  the property name
	 * @param value the property value
	 */
	@JsonAnySetter
	public void setProperties(String name, Object value) {
		this.properties.put(name, value);
	}
}
