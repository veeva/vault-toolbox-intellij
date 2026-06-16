package com.veeva.vault.toolbox.core.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Represents the step header manifest for a CSV data step in a Vault package.
 */
@JacksonXmlRootElement(localName = "stepheader")
public class CsvManifest extends StepManifest {

	@JacksonXmlProperty(localName = "datastepheader")
	private CsvDataStep csvDataStep;

	/**
	 * Returns the CSV data step configuration for this manifest.
	 *
	 * @return the CSV data step
	 */
	public CsvDataStep getCsvDataStep() {
		return csvDataStep;
	}

	/**
	 * Sets the CSV data step configuration for this manifest.
	 *
	 * @param csvDataStep the CSV data step
	 */
	public void setCsvDataStep(CsvDataStep csvDataStep) {
		this.csvDataStep = csvDataStep;
	}
}
