package com.veeva.vault.toolbox.core.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the header of a Vault package step, containing common step metadata
 * such as a label, required flag, and checksum.
 */
@JacksonXmlRootElement(localName = "stepheader")
public class StepManifest {

	@JacksonXmlProperty(localName = "label")
	private String label;

	@JacksonXmlProperty(localName = "steprequired")
	private Boolean stepRequired = false;

	@JacksonXmlProperty(localName = "checksum")
	private String checksum;

	private final Map<String, Object> properties = new HashMap<>();

	/**
	 * Returns the display label for this step.
	 *
	 * @return the step label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the display label for this step.
	 *
	 * @param label the step label
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Returns whether this step is required.
	 *
	 * @return {@code true} if the step is required, {@code false} otherwise
	 */
	public Boolean getStepRequired() {
		return stepRequired;
	}

	/**
	 * Sets whether this step is required.
	 *
	 * @param stepRequired {@code true} if the step is required
	 */
	public void setStepRequired(Boolean stepRequired) {
		this.stepRequired = stepRequired;
	}

	/**
	 * Returns the checksum for this step.
	 *
	 * @return the checksum
	 */
	public String getChecksum() {
		return checksum;
	}

	/**
	 * Sets the checksum for this step.
	 *
	 * @param checksum the checksum
	 */
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

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
