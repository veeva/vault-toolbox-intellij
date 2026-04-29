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
import java.util.*;
import java.util.regex.Pattern;

public class CsvMetadataReader<T> {
	private static final Logger logger = LoggerFactory.getLogger(CsvMetadataReader.class);

	Set<String> fieldNames = null;
	Map<String, String> headerRow;
	CsvSchema readerSchema = null;
	MappingIterator<?> rowIterator = null;
	File inputFile = null;
	Class<T> rowClass;

	public CsvMetadataReader(File inputFile, Class<T> rowClass) throws Exception {
		this.inputFile = inputFile;

		CsvMapper mapper = new CsvMapper();
		mapper.enable(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE);

		CsvMapper sourceMapper = new CsvMapper();
		CsvSchema schema = CsvSchema.emptySchema().withHeader();
		MappingIterator<Map<String, String>> headerIterator = sourceMapper.readerFor(Map.class)
				.with(schema)
				.readValues(inputFile);

		headerRow = headerIterator.next();

		headerRow.put("vault_id", "");

		this.rowClass = rowClass;
		rowIterator = mapper.readerFor(rowClass)
				.with(getSchema())
				.readValues(inputFile);
	}

	public List<VaultModel> getAllRows() {
		return getRows(null);
	}

    public List<VaultModel> getRows(Integer batchLimit) {
        try {
            List<VaultModel> result = new ArrayList<>();
            boolean previousRowInvalid = false;
            while (rowIterator.hasNext()
                    && ((batchLimit == null)
                    || (batchLimit == 0)
                    || (result.size() < batchLimit))) {

                VaultModel row = (VaultModel)rowIterator.next();

                // Temp fix for DEV-691878. Api Error Messages printing on multiple lines
                if (!checkValidRow(row)) {
                    // FIXED: We must ensure 'result' is not empty before trying to grab the last item!
                    // If it is empty, it means the stack trace spilled over from the previous batch,
                    // so we can safely just ignore it and continue.
                    if (!previousRowInvalid && !result.isEmpty()) {
                        result.get(result.size() - 1).set("reference_id", "");
                    }
                    previousRowInvalid = true;
                    continue;
                }

                String errorMessage = (String) row.get("api_response_error_message");
                if (errorMessage != null && errorMessage.length() > 0) {
                    errorMessage = errorMessage.replace("\"", "");
                    row.set("api_response_error_message", errorMessage);
                }
                previousRowInvalid = false;
                result.add(row);
            }

            return result;
        }
        catch (Exception e) {
            logger.error("Error reading CSV rows: " + e.getMessage(), e);
            return null;
        }
    }

	public CsvSchema getSchema() {
		if (readerSchema == null) {
			CsvSchema.Builder builder = new CsvSchema.Builder();
			builder.setUseHeader(true);

			fieldNames = new LinkedHashSet<>();
			for (String fieldname : headerRow.keySet()) {
				builder.addColumn(fieldname);
				fieldNames.add(fieldname);
			}
			readerSchema = builder.build();
		}

		return readerSchema;
	}

	public boolean hasNext() {
		if (rowIterator != null)
			return rowIterator.hasNext();
		else
			return false;
	}

	public Set<String> getFieldNames() {
		return fieldNames;
	}

    private boolean checkValidRow(VaultModel row) {
        String timestamp = (String) row.get("timestamp");
        if (timestamp == null || timestamp.isEmpty()) {
            return false;
        }

        String pattern = "^\\d{4}-\\d{2}-\\d{2}[T ]";
        Pattern regex = Pattern.compile(pattern);
        return regex.matcher(timestamp).find();
    }
}
