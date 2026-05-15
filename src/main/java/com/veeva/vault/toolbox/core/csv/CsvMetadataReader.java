/*---------------------------------------------------------------------
 *	Copyright (c) 2020 Veeva Systems Inc.  All Rights Reserved.
 *	This code is based on pre-existing content developed and
 *	owned by Veeva Systems Inc. and may only be used in connection
 *	with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.toolbox.core.csv;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.veeva.vault.vapil.api.model.VaultModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Streaming reader for CSV files whose rows map onto a {@link VaultModel}.
 * The header row is parsed eagerly so column names are available immediately;
 * data rows are pulled lazily and may be consumed in full via {@link #getAllRows()}
 * or in batches via {@link #getRows(Integer)}.
 *
 * @param <T> row type used for Jackson deserialization
 */
public class CsvMetadataReader<T> {
	private static final Logger logger = LoggerFactory.getLogger(CsvMetadataReader.class);
	private static final Pattern TIMESTAMP_PREFIX = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}[T ]");
	private static final String VAULT_ID_COLUMN = "vault_id";
	private static final String TIMESTAMP_COLUMN = "timestamp";
	private static final String REFERENCE_ID_COLUMN = "reference_id";
	private static final String API_ERROR_COLUMN = "api_response_error_message";

	private final Set<String> fieldNames = new LinkedHashSet<>();
	private final MappingIterator<?> rowIterator;

	/**
	 * Opens {@code inputFile} for streaming reads and prepares the schema from its header row.
	 *
	 * @param inputFile CSV file to read
	 * @param rowClass row type to deserialize each record into
	 * @throws IOException if the file cannot be opened or its header cannot be parsed
	 */
	public CsvMetadataReader(File inputFile, Class<T> rowClass) throws IOException {
		Map<String, String> headerRow = readHeaderRow(inputFile);
		headerRow.put(VAULT_ID_COLUMN, "");
		fieldNames.addAll(headerRow.keySet());

		CsvMapper mapper = new CsvMapper();
		mapper.enable(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE);
		rowIterator = mapper.readerFor(rowClass)
				.with(buildSchema(fieldNames))
				.readValues(inputFile);
	}

	/**
	 * Reads every remaining row from the file.
	 *
	 * @return all remaining rows, or {@code null} if reading fails
	 */
	public List<VaultModel> getAllRows() {
		return getRows(null);
	}

	/**
	 * Reads up to {@code batchLimit} rows from the file. Pass {@code null} or {@code 0} to read
	 * every remaining row.
	 *
	 * <p>Rows whose timestamp is missing or malformed are skipped: these originate from log lines
	 * whose stack traces spilled across multiple physical lines (DEV-691878). When such a row is
	 * skipped, the {@code reference_id} of the previous valid row is cleared so that the orphaned
	 * trace is not attributed to an unrelated request.
	 *
	 * @param batchLimit maximum number of rows to return, or {@code null}/{@code 0} for unlimited
	 * @return rows read in this batch, or {@code null} if reading fails
	 */
	public List<VaultModel> getRows(Integer batchLimit) {
		try {
			List<VaultModel> result = new ArrayList<>();
			boolean previousRowInvalid = false;
			while (rowIterator.hasNext() && !batchFull(result.size(), batchLimit)) {
				VaultModel row = (VaultModel) rowIterator.next();

				if (!isValidRow(row)) {
					if (!previousRowInvalid && !result.isEmpty()) {
						result.get(result.size() - 1).set(REFERENCE_ID_COLUMN, "");
					}
					previousRowInvalid = true;
					continue;
				}

				String errorMessage = (String) row.get(API_ERROR_COLUMN);
				if (errorMessage != null && !errorMessage.isEmpty()) {
					row.set(API_ERROR_COLUMN, errorMessage.replace("\"", ""));
				}
				previousRowInvalid = false;
				result.add(row);
			}
			return result;
		} catch (Exception e) {
			logger.error("Error reading CSV rows: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * @return {@code true} if at least one more row is available to read
	 */
	public boolean hasNext() {
		return rowIterator != null && rowIterator.hasNext();
	}

	/**
	 * @return the column names discovered from the header (including the appended {@code vault_id})
	 */
	public Set<String> getFieldNames() {
		return fieldNames;
	}

	/**
	 * Reads the header row from a CSV file to determine column names.
	 *
	 * @param inputFile the CSV file to read
	 * @return a map representing the header row
	 * @throws IOException if the file cannot be read
	 */
	private static Map<String, String> readHeaderRow(File inputFile) throws IOException {
		CsvMapper headerMapper = new CsvMapper();
		CsvSchema headerSchema = CsvSchema.emptySchema().withHeader();
		try (MappingIterator<Map<String, String>> headerIterator = headerMapper.readerFor(Map.class)
				.with(headerSchema)
				.readValues(inputFile)) {
			return headerIterator.next();
		}
	}

	/**
	 * Builds a {@link CsvSchema} based on the specified set of columns.
	 *
	 * @param columns the set of column names
	 * @return the constructed CsvSchema
	 */
	private static CsvSchema buildSchema(Set<String> columns) {
		CsvSchema.Builder builder = new CsvSchema.Builder().setUseHeader(true);
		for (String column : columns) {
			builder.addColumn(column);
		}
		return builder.build();
	}

	/**
	 * Determines if a batch has reached its specified limit.
	 *
	 * @param currentSize the current number of items in the batch
	 * @param batchLimit  the maximum allowed size, or {@code null}/{@code 0} for unlimited
	 * @return {@code true} if the batch is full; {@code false} otherwise
	 */
	private static boolean batchFull(int currentSize, Integer batchLimit) {
		return batchLimit != null && batchLimit > 0 && currentSize >= batchLimit;
	}

	/**
	 * Validates if a row has a valid timestamp. Rows with missing or malformed timestamps
	 * are typically incomplete log lines and should be skipped.
	 *
	 * @param row the row to validate
	 * @return {@code true} if the row is valid; {@code false} otherwise
	 */
	private static boolean isValidRow(VaultModel row) {
		String timestamp = (String) row.get(TIMESTAMP_COLUMN);
		return timestamp != null && !timestamp.isEmpty() && TIMESTAMP_PREFIX.matcher(timestamp).find();
	}
}
