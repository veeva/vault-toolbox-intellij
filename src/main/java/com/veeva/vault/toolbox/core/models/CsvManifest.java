/*---------------------------------------------------------------------
 *	Copyright (c) 2020 Veeva Systems Inc.  All Rights Reserved.
 *	This code is based on pre-existing content developed and
 *	owned by Veeva Systems Inc. and may only be used in connection
 *	with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.toolbox.core.models;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;


@JacksonXmlRootElement(localName = "stepheader")
public class CsvManifest extends StepManifest {
	@JacksonXmlProperty(localName = "datastepheader")
	private CsvDataStep csvDataStep;

	public CsvDataStep getCsvDataStep() {
		return csvDataStep;
	}

	public void setCsvDataStep(CsvDataStep csvDataStep) {
		this.csvDataStep = csvDataStep;
	}
}
