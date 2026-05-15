/*---------------------------------------------------------------------
 *	Copyright (c) 2020 Veeva Systems Inc.  All Rights Reserved.
 *	This code is based on pre-existing content developed and
 *	owned by Veeva Systems Inc. and may only be used in connection
 *	with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.toolbox.core.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.veeva.vault.toolbox.core.utils.FileIO;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the {@code vaultpackage.xml} manifest of a Vault package (.vpk).
 */
@JacksonXmlRootElement(localName = "vaultpackage")
public class VpkManifest {

	/** The standard file name for a Vault package manifest. */
	@JsonIgnore
	public static final String VAULTPACKAGE_FILENAME = "vaultpackage.xml";

	@JacksonXmlProperty(isAttribute = true)
	private String xmlns = "https://veevavault.com/";

	@JacksonXmlProperty(localName = "name")
	private String name;

	@JacksonXmlProperty(localName = "source")
	private Source source;

	@JacksonXmlProperty(localName = "packagetype")
	private String packageType = "migration__v";

	@JacksonXmlProperty(localName = "summary")
	private String summary;

	@JacksonXmlProperty(localName = "description")
	private String description;

	@JacksonXmlProperty(localName = "javasdk")
	private JavaSdk javasdk;

	private final Map<String, Object> properties = new HashMap<>();

	/**
	 * Defines the type of a Vault package, controlling how its components are deployed.
	 */
	public enum PackageType {
		MIGRATION("migration__v"),
		TESTDATA("test_data__sys");

		private final String value;

		PackageType(String value) {
			this.value = value;
		}

		/**
		 * Returns the string value of this package type as used in the manifest XML.
		 *
		 * @return the manifest string value
		 */
		public String getValue() {
			return value;
		}
	}

	/**
	 * Returns the name of this Vault package.
	 *
	 * @return the package name
	 */
	@JsonGetter
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of this Vault package.
	 *
	 * @param name the package name
	 */
	@JsonSetter
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the source metadata of this Vault package.
	 *
	 * @return the source
	 */
	@JsonGetter
	public Source getSource() {
		return source;
	}

	/**
	 * Sets the source metadata of this Vault package.
	 *
	 * @param source the source
	 */
	@JsonSetter
	public void setSource(Source source) {
		this.source = source;
	}

	/**
	 * Returns the package type string value.
	 *
	 * @return the package type
	 */
	@JsonGetter
	public String getPackageType() {
		return packageType;
	}

	/**
	 * Sets the package type using its raw string value.
	 *
	 * @param packageType the package type string
	 */
	@JsonAnySetter
	public void setPackageType(String packageType) {
		this.packageType = packageType;
	}

	/**
	 * Sets the package type using the {@link PackageType} enum.
	 *
	 * @param packageType the package type enum value
	 */
	@JsonIgnore
	public void setPackageType(PackageType packageType) {
		this.packageType = packageType.getValue();
	}

	/**
	 * Returns the summary of this Vault package.
	 *
	 * @return the summary
	 */
	@JsonGetter
	public String getSummary() {
		return summary;
	}

	/**
	 * Sets the summary of this Vault package.
	 *
	 * @param summary the summary
	 */
	@JsonSetter
	public void setSummary(String summary) {
		this.summary = summary;
	}

	/**
	 * Returns the description of this Vault package.
	 *
	 * @return the description
	 */
	@JsonGetter
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description of this Vault package.
	 *
	 * @param description the description
	 */
	@JsonSetter
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Returns the Java SDK configuration for this Vault package.
	 *
	 * @return the Java SDK configuration
	 */
	@JsonGetter
	public JavaSdk getJavasdk() {
		return javasdk;
	}

	/**
	 * Sets the Java SDK configuration for this Vault package.
	 *
	 * @param javasdk the Java SDK configuration
	 */
	@JsonSetter
	public void setJavasdk(JavaSdk javasdk) {
		this.javasdk = javasdk;
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

	/**
	 * Serializes this manifest to the given file as indented XML.
	 * Parent directories are created if they do not exist.
	 *
	 * @param file the target file
	 */
	@JsonIgnore
	public void save(File file) {
		FileIO.makeDirectories(file.getParentFile());
		try {
			XmlMapper xmlMapper = new XmlMapper();
			xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
			xmlMapper.writeValue(file, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
