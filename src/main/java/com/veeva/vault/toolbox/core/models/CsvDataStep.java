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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the data step header configuration for a CSV import step in a Vault package.
 */
public class CsvDataStep {

	@JacksonXmlProperty(localName = "object")
	private String object;

	@JacksonXmlProperty(localName = "idparam")
	private String idParam;

	@JacksonXmlProperty(localName = "datatype")
	private String dataType;

	@JacksonXmlProperty(localName = "action")
	private String action;

	@JacksonXmlProperty(localName = "recordmigrationmode")
	private Boolean recordMigrationMode;

	@JacksonXmlProperty(localName = "recordcount")
	private Integer recordCount;

	private final Map<String, Object> properties = new HashMap<>();

	/**
	 * Returns the Vault object name targeted by this CSV step.
	 *
	 * @return the object name
	 */
	public String getObject() {
		return object;
	}

	/**
	 * Sets the Vault object name targeted by this CSV step.
	 *
	 * @param object the object name
	 */
	public void setObject(String object) {
		this.object = object;
	}

	/**
	 * Returns the ID parameter field name used to match records.
	 *
	 * @return the ID parameter field name
	 */
	public String getIdParam() {
		return idParam;
	}

	/**
	 * Sets the ID parameter field name used to match records.
	 *
	 * @param idParam the ID parameter field name
	 */
	public void setIdParam(String idParam) {
		this.idParam = idParam;
	}

	/**
	 * Returns the data type for this CSV step.
	 *
	 * @return the data type
	 */
	public String getDataType() {
		return dataType;
	}

	/**
	 * Sets the data type for this CSV step.
	 *
	 * @param dataType the data type
	 */
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	/**
	 * Returns the action to perform for records in this CSV step (e.g., upsert, delete).
	 *
	 * @return the action string
	 */
	public String getAction() {
		return action;
	}

	/**
	 * Sets the action to perform for records in this CSV step.
	 *
	 * @param action the action string
	 */
	public void setAction(String action) {
		this.action = action;
	}

	/**
	 * Returns whether record migration mode is enabled for this CSV step.
	 *
	 * @return {@code true} if record migration mode is enabled, {@code false} otherwise
	 */
	public Boolean getRecordMigrationMode() {
		return recordMigrationMode;
	}

	/**
	 * Sets whether record migration mode is enabled for this CSV step.
	 *
	 * @param recordMigrationMode {@code true} to enable record migration mode
	 */
	public void setRecordMigrationMode(Boolean recordMigrationMode) {
		this.recordMigrationMode = recordMigrationMode;
	}

	/**
	 * Returns the expected number of records in this CSV step.
	 *
	 * @return the record count
	 */
	public Integer getRecordCount() {
		return recordCount;
	}

	/**
	 * Sets the expected number of records in this CSV step.
	 *
	 * @param recordCount the record count
	 */
	public void setRecordCount(Integer recordCount) {
		this.recordCount = recordCount;
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
