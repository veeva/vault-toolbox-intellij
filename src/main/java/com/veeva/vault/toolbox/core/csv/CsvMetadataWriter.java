/*---------------------------------------------------------------------
 *	Copyright (c) 2020 Veeva Systems Inc.  All Rights Reserved.
 *	This code is based on pre-existing content developed and
 *	owned by Veeva Systems Inc. and may only be used in connection
 *	with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.toolbox.core.csv;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.veeva.vault.toolbox.core.utils.FileIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Writes rows of CSV data using a schema built up at runtime. Callers register columns with
 * {@link #addColumn(String)} in the desired output order, then invoke {@link #writeAllRows}
 * to serialize the rows to disk.
 */
public class CsvMetadataWriter {
	private static final Logger logger = LoggerFactory.getLogger(CsvMetadataWriter.class);
	private static final int OUTPUT_BUFFER_SIZE = 1024;

	private final CsvSchema.Builder outputSchemaBuilder = CsvSchema.builder();

	/**
	 * Appends a column to the output schema. Columns are written in the order they are added.
	 *
	 * @param fieldName name of the column to add
	 */
	public void addColumn(String fieldName) {
		outputSchemaBuilder.addColumn(fieldName);
	}

	/**
	 * Serializes {@code outputRows} to {@code outputFile} using the schema built via
	 * {@link #addColumn(String)}. Missing parent directories are created automatically and
	 * {@code null} values are written as empty strings. Errors are logged rather than propagated.
	 *
	 * @param useHeader {@code true} to emit a header row before the data rows
	 * @param appendToFile {@code true} to append to an existing file, {@code false} to overwrite
	 * @param outputFile destination file
	 * @param outputRows rows to serialize, typically a {@code Collection} of POJOs or maps
	 */
	public void writeAllRows(boolean useHeader, boolean appendToFile, File outputFile, Object outputRows) {
		try {
			CsvMapper outputMapper = new CsvMapper();
			outputMapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
			outputMapper.configure(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS, true);

			outputSchemaBuilder.setUseHeader(useHeader);
			outputSchemaBuilder.setNullValue("");
			FileIO.makeDirectories(outputFile.getParentFile());

			ObjectWriter resultsWriter = outputMapper.writer(outputSchemaBuilder.build());
			try (FileOutputStream fileStream = new FileOutputStream(outputFile, appendToFile);
				 BufferedOutputStream bufferedStream = new BufferedOutputStream(fileStream, OUTPUT_BUFFER_SIZE);
				 OutputStreamWriter writer = new OutputStreamWriter(bufferedStream, StandardCharsets.UTF_8)) {
				resultsWriter.writeValue(writer, outputRows);
			}
		} catch (Exception e) {
			logger.error("Error writing CSV rows to {}: {}", outputFile, e.getMessage(), e);
		}
	}
}
