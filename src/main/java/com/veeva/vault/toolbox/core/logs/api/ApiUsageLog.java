package com.veeva.vault.toolbox.core.logs.api;

import com.veeva.vault.toolbox.core.csv.CsvMetadataReader;
import com.veeva.vault.toolbox.core.csv.CsvMetadataWriter;
import com.veeva.vault.toolbox.core.sql.Sqlite;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.VaultModel;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.LogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiUsageLog {
	private static final Logger logger = LoggerFactory.getLogger(ApiUsageLog.class);

	private final int BATCH_SIZE = 500;

	public ApiUsageLog() {
	}

	public void analyze(File dbFile, File outputFile) {
		try {
			Sqlite sqlDb = new Sqlite(dbFile);
			if (outputFile == null) {
				String defaultOutputFileName = new SimpleDateFormat("vault-log-analyzer-api-yyyyMMdd-HHmmssSSS")
						.format(ZonedDateTime.now())
						+ ".csv";
				String defaultOutputFilePath = FileSystems
						.getDefault()
						.getPath(defaultOutputFileName)
						.normalize()
						.toAbsolutePath()
						.toString();
				outputFile = new File(defaultOutputFilePath);
			}
			String sql = new String(FileIO.getResourceContent("api/stats.sql", this.getClass().getClassLoader()));
			sqlDb.execute(sql);

			CsvMetadataWriter csvMetadataWriter = new CsvMetadataWriter();
			List<VaultModel> statRows = new ArrayList<>();

			ResultSet resultSet = sqlDb.query("SELECT * FROM stats");
			if (resultSet != null) {
				List<String> fieldNames = new ArrayList<>();

				boolean addHeader = true;
				boolean append = false;
				int rowCount = 0;
				while (resultSet.next()) {
					rowCount++;
					VaultModel model = new VaultModel();
					statRows.add(model);

					if (rowCount == 1) {
						ResultSetMetaData metaData = resultSet.getMetaData();
						for (int i = 1; i <= metaData.getColumnCount(); i++) {
							String fieldName = metaData.getColumnName(i);
							fieldNames.add(fieldName);
							csvMetadataWriter.addColumn(fieldName);
						}
					}

					for (String fieldName : fieldNames) {
						model.set(fieldName, resultSet.getString(fieldName));
					}

					if (statRows.size() == BATCH_SIZE) {
						csvMetadataWriter.writeAllRows(addHeader, append, outputFile, statRows);
						statRows.clear();
						addHeader = false;
						append = true;
					}
				}

				if (statRows.size() > 0) {
					csvMetadataWriter.writeAllRows(addHeader, append, outputFile, statRows);
				}
			}

		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void download(VaultClient vaultClient, LocalDate startDate, LocalDate endDate, File outputDirectory, Boolean unZipAfterDownload) {
		try {
			//default startdate to today when null
			if (startDate == null) {
				startDate = LocalDate.now(ZoneId.of("UTC"));
			}

			//default enddate to today when null
			if (endDate == null) {
				endDate = LocalDate.now(ZoneId.of("UTC"));
			}

			//default unzip to true when null
			if (unZipAfterDownload == null) {
				unZipAfterDownload = true;
			}

			LocalDate logDate = startDate;
			while (!logDate.isAfter(endDate)) {
				VaultResponse response = vaultClient.newRequest(LogRequest.class).retrieveDailyAPIUsage(logDate);

				if (response != null && !response.isFailure() && response.getBinaryContent() != null) {
					//build the file name since the API does not include the name in the response
					//this is the Vault default form:
					// 		{vaultId}-APIUsageLog-{YYYY-MM-DD}.zip			12345-APIUsageLog-2022-01-15.zip
					String dateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(logDate);
					String fileName = response.getHeaderVaultId() + "-APIUsageLog-" + dateStr + ".zip";

					File logFile = new File(outputDirectory, fileName);
					FileIO.makeDirectories(logFile.getParentFile());
					FileIO.writeFileContent(logFile, response.getBinaryContent());

					if (unZipAfterDownload) {
						FileIO.unzipFiles(logFile, logFile.getParentFile());
					}

					try {
						MessageDigest md = MessageDigest.getInstance("MD5");
						md.update(response.getBinaryContent());
						byte[] digest = md.digest();
						StringBuilder sb = new StringBuilder();
						for (byte b : digest) {
							sb.append(String.format("%02x", b));
						}
						String md5 = sb.toString();

						String jsonContent = "{\n" +
								"  \"log_date\": \"" + dateStr + "\",\n" +
								"  \"md5checksum\": \"" + md5 + "\",\n" +
								"  \"fileName\": \"" + fileName + "\"\n" +
								"}";
						File jsonFile = new File(outputDirectory, fileName.replace(".zip", ".json"));
						FileIO.writeFileContent(jsonFile, jsonContent.getBytes(StandardCharsets.UTF_8));
					} catch (Exception ex) {
						logger.error("Failed to generate JSON for api usage log: " + ex.getMessage(), ex);
					}
				}

				logDate = logDate.plusDays(1);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void importLogFiles(File dbFile,  File logDirectory) {
		importLogFiles(dbFile, logDirectory, new String());
	}

	public void importLogFiles(File dbFile,  File logDirectory, String vaultId) {
		try {
			List<File> logFiles = FileIO.getFiles(logDirectory, ".csv");
			importLogFiles(dbFile, logFiles, vaultId);
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void importLogFiles(File dbFile, List<File> logFiles) {
		importLogFiles(dbFile, logFiles, new String());
	}

	public void importLogFiles(File dbFile, List<File> logFiles, String vaultId) {
		try {
			Sqlite sqlDb = new Sqlite(dbFile);

			int fileCount = 0;
			for (File apiLogFile : logFiles) {
				if (vaultId == null || vaultId.isEmpty()) {
					String fileName = apiLogFile.getName();
					String numberPattern = "^\\d+";

					Pattern pattern = Pattern.compile(numberPattern);
					Matcher matcher = pattern.matcher(fileName);

					if (matcher.find()) {
						vaultId = matcher.group();
					} else {
						vaultId = "unknown";
					}
				}
//
				fileCount++;

				long numLines = Files.lines(apiLogFile.toPath()).count();
				if (numLines > 1) {
					long totalBatches = (numLines + BATCH_SIZE - 1) / BATCH_SIZE;
					CsvMetadataReader apiLogReader = new CsvMetadataReader(apiLogFile, VaultModel.class);
					String sqlTableName = "vaultApi" + apiLogFile.getName().replace("-", "").replace(".csv", "");

					boolean createdTable = false;
					int batchCount = 0;

					Map<Long, Boolean> percentMap = new HashMap<>();
					while (apiLogReader.hasNext()) {
						batchCount++;

						long percent = 100 - ((batchCount * 100) / totalBatches);
						if (!percentMap.keySet().contains(percent)) {
							percentMap.put(percent, true);

							StringBuilder progressBuilder = new StringBuilder();
							progressBuilder.append(apiLogFile.getName());
							for (int i = 0; i < percent; i++) {
								progressBuilder.append("_");
							}
							logger.info(progressBuilder.toString());
						}

						List<VaultModel> apiLogEntries = apiLogReader.getRows(BATCH_SIZE);
						if (apiLogEntries != null && apiLogEntries.size() > 0) {
							transform(apiLogEntries, vaultId);
							if (!createdTable) {
								sqlDb.createTable(sqlTableName, apiLogEntries.get(0).getFieldNames(), true);
								sqlDb.createTable("api", apiLogEntries.get(0).getFieldNames(), false);
								createdTable = true;
							}
							loadToSql(sqlDb, sqlTableName, apiLogEntries);
						}
					}
				}
			}

		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	private void loadToSql(Sqlite sqlDb, String tableName, List<VaultModel> apiLogEntries) {
		try {
			if (apiLogEntries != null && apiLogEntries.size() > 0) {
				sqlDb.startInsertStatement(tableName, apiLogEntries.get(0));
				sqlDb.startInsertStatement("api", apiLogEntries.get(0));
				for (VaultModel apiLogEntry : apiLogEntries) {
					sqlDb.addInsertValues(tableName, apiLogEntry);
					sqlDb.addInsertValues("api", apiLogEntry);
				}
				sqlDb.flushBuilders();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
	private void transform(List<VaultModel> apiLogEntries, String vaultId) {
		try {
			for (VaultModel apiLogEntry : apiLogEntries) {

				if (!apiLogEntry.getFieldNames().contains("connection")) {
					apiLogEntry.set("connection", "23R1-Feature");
				}
				if (!apiLogEntry.getFieldNames().contains("api_resource")) {
					apiLogEntry.set("api_resource", "23R1-Feature");
				}

				apiLogEntry.set("vault_id", vaultId);

				String apiEndpoint = apiLogEntry.getString("endpoint");
				if (apiEndpoint != null) {
					StringBuilder apiResourceBuilder = new StringBuilder();
					if (apiEndpoint.contains("/") && apiEndpoint.length() > 1) {
						List<String> parts = Arrays.asList(apiEndpoint.split("/"));

						int idCount = 0;
						String lastPart = null;
						for (String part : parts) {
							if (!part.isEmpty()) {
								String tempPart = part;
								if (lastPart != null && lastPart.equals("api") && part.startsWith("v")) {
									tempPart = "{version}";
								}
								else if (!part.contains("__") && part.matches(".*\\d.*")) {
									idCount++;
									tempPart = (idCount > 1) ? "{id" + idCount + "}" : "{id}";

								}
								apiResourceBuilder.append("/" + tempPart);
								lastPart = part;
							}
						}
						apiEndpoint = apiResourceBuilder.toString();
					}
				}
				apiLogEntry.set("api_endpoint", apiEndpoint);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
}
