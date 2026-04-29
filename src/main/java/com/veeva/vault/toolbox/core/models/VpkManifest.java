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

@JacksonXmlRootElement(localName = "vaultpackage")
public class VpkManifest {
	@JacksonXmlProperty(isAttribute = true)
	private String xmlns = "https://veevavault.com/";

	@JsonIgnore
	public final static String VAULTPACKAGE_FILENAME = "vaultpackage.xml";

	@JacksonXmlProperty(localName = "name")
	private String name;

	@JsonGetter
	public String getName() {
		return name;
	}

	@JsonSetter
	public void setName(String name) {
		this.name = name;
	}

	@JacksonXmlProperty(localName = "source")
	private Source source;

	@JsonGetter
	public Source getSource() {
		return source;
	}

	@JsonSetter
	public void setSource(Source source) {
		this.source = source;
	}

	public enum PackageType {
		MIGRATION("migration__v"),
		TESTDATA("test_data__sys");

		String value;
		PackageType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	@JacksonXmlProperty(localName = "packagetype")
	private String packageType = "migration__v";

	@JsonGetter
	public String getPackageType() {
		return packageType;
	}

	@JsonAnySetter
	public void setPackageType(String packageType) {
		this.packageType = packageType;
	}

	@JsonIgnore
	public void setPackageType(PackageType packageType) {
		this.packageType = packageType.getValue();
	}

	@JacksonXmlProperty(localName = "summary")
	private String summary;

	@JsonGetter
	public String getSummary() {
		return summary;
	}

	@JsonSetter
	public void setSummary(String summary) {
		this.summary = summary;
	}

	@JacksonXmlProperty(localName = "description")
	private String description;

	@JsonGetter
	public String getDescription() {
		return description;
	}

	@JsonSetter
	public void setDescription(String description) {
		this.description = description;
	}

	@JacksonXmlProperty(localName = "javasdk")
	private JavaSdk javasdk;

	@JsonGetter
	public JavaSdk getJavasdk() {
		return javasdk;
	}

	@JsonSetter
	public void setJavasdk(JavaSdk javasdk) {
		this.javasdk = javasdk;
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


	@JsonIgnore
	public void save(File file ) {
		File outputDir = file.getParentFile();
		FileIO.makeDirectories(outputDir);

		try {
			XmlMapper xmlMapper = new XmlMapper();
			xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
			xmlMapper.writeValue(file, this);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
