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

/**
 * Manages the downloading, importing, and analysis of Vault API Usage logs.
 * API usage logs are downloaded as ZIP files, imported into a SQLite database,
 * and analyzed to produce statistical reports in CSV format.
 */
public class ApiUsageLog {
    private static final Logger logger = LoggerFactory.getLogger(ApiUsageLog.class);
    private static final int BATCH_SIZE = 500;

    /**
     * Executes the stats SQL against the given SQLite database and writes the results to the output CSV file.
     * If {@code outputFile} is {@code null}, a timestamped file is created in the current directory.
     *
     * @param dbFile     the SQLite database file containing imported API usage log data
     * @param outputFile destination CSV file for the analysis results
     */
    public void analyze(File dbFile, File outputFile) {
        try {
            Sqlite sqlDb = new Sqlite(dbFile);
            if (outputFile == null) {
                String defaultOutputFileName = new SimpleDateFormat("vault-log-analyzer-api-yyyyMMdd-HHmmssSSS")
                        .format(ZonedDateTime.now()) + ".csv";
                outputFile = new File(FileSystems.getDefault()
                        .getPath(defaultOutputFileName).normalize().toAbsolutePath().toString());
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

                if (!statRows.isEmpty()) {
                    csvMetadataWriter.writeAllRows(addHeader, append, outputFile, statRows);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Downloads daily API usage logs for each date in the given range and saves them to the output directory.
     * Each downloaded file is accompanied by a companion JSON file containing the date, MD5 checksum, and filename.
     * Null date parameters default to today (UTC). Null {@code unZipAfterDownload} defaults to {@code true}.
     *
     * @param vaultClient        authenticated Vault client
     * @param startDate          first date to download (inclusive); defaults to today if {@code null}
     * @param endDate            last date to download (inclusive); defaults to today if {@code null}
     * @param outputDirectory    directory where log files will be saved
     * @param unZipAfterDownload if {@code true}, ZIP files are extracted after download
     */
    public void download(VaultClient vaultClient, LocalDate startDate, LocalDate endDate, File outputDirectory, Boolean unZipAfterDownload) {
        try {
            if (startDate == null) {
                startDate = LocalDate.now(ZoneId.of("UTC"));
            }
            if (endDate == null) {
                endDate = LocalDate.now(ZoneId.of("UTC"));
            }
            if (unZipAfterDownload == null) {
                unZipAfterDownload = true;
            }

            LocalDate logDate = startDate;
            while (!logDate.isAfter(endDate)) {
                VaultResponse response = vaultClient.newRequest(LogRequest.class).retrieveDailyAPIUsage(logDate);

                if (response != null && !response.isFailure() && response.getBinaryContent() != null) {
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
                        logger.error("Failed to generate JSON for api usage log: {}", ex.getMessage(), ex);
                    }
                }

                logDate = logDate.plusDays(1);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Imports all CSV log files in the given directory into the SQLite database.
     * The vault ID is extracted from each filename.
     *
     * @param dbFile       the SQLite database file
     * @param logDirectory directory containing CSV log files to import
     */
    public void importLogFiles(File dbFile, File logDirectory) {
        importLogFiles(dbFile, logDirectory, "");
    }

    /**
     * Imports all CSV log files in the given directory into the SQLite database,
     * associating each entry with the given vault ID.
     *
     * @param dbFile       the SQLite database file
     * @param logDirectory directory containing CSV log files to import
     * @param vaultId      vault ID to associate with each log entry; if empty, extracted from the filename
     */
    public void importLogFiles(File dbFile, File logDirectory, String vaultId) {
        try {
            List<File> logFiles = FileIO.getFiles(logDirectory, ".csv");
            importLogFiles(dbFile, logFiles, vaultId);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Imports the given list of CSV log files into the SQLite database.
     * The vault ID is extracted from each filename.
     *
     * @param dbFile    the SQLite database file
     * @param logFiles  list of CSV log files to import
     */
    public void importLogFiles(File dbFile, List<File> logFiles) {
        importLogFiles(dbFile, logFiles, "");
    }

    /**
     * Imports the given list of CSV log files into the SQLite database,
     * associating each entry with the given vault ID.
     *
     * @param dbFile    the SQLite database file
     * @param logFiles  list of CSV log files to import
     * @param vaultId   vault ID to associate with each log entry; if empty, extracted from the filename
     */
    public void importLogFiles(File dbFile, List<File> logFiles, String vaultId) {
        try {
            Sqlite sqlDb = new Sqlite(dbFile);

            for (File apiLogFile : logFiles) {
                if (vaultId == null || vaultId.isEmpty()) {
                    Pattern pattern = Pattern.compile("^\\d+");
                    Matcher matcher = pattern.matcher(apiLogFile.getName());
                    vaultId = matcher.find() ? matcher.group() : "unknown";
                }

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
                        if (!percentMap.containsKey(percent)) {
                            percentMap.put(percent, true);
                            StringBuilder progressBuilder = new StringBuilder();
                            progressBuilder.append(apiLogFile.getName());
                            for (int i = 0; i < percent; i++) {
                                progressBuilder.append("_");
                            }
                            logger.info(progressBuilder.toString());
                        }

                        List<VaultModel> apiLogEntries = apiLogReader.getRows(BATCH_SIZE);
                        if (apiLogEntries != null && !apiLogEntries.isEmpty()) {
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
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Loads a batch of API log entries into the specified SQLite table and a global "api" table.
     *
     * @param sqlDb         the SQLite database connection
     * @param tableName     the specific table name for the log file
     * @param apiLogEntries the batch of log entries to load
     */
    private void loadToSql(Sqlite sqlDb, String tableName, List<VaultModel> apiLogEntries) {
        try {
            if (apiLogEntries != null && !apiLogEntries.isEmpty()) {
                sqlDb.startInsertStatement(tableName, apiLogEntries.get(0));
                sqlDb.startInsertStatement("api", apiLogEntries.get(0));
                for (VaultModel apiLogEntry : apiLogEntries) {
                    sqlDb.addInsertValues(tableName, apiLogEntry);
                    sqlDb.addInsertValues("api", apiLogEntry);
                }
                sqlDb.flushBuilders();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Transforms raw API log entries by adding standard fields and normalizing the API endpoint.
     *
     * @param apiLogEntries the batch of log entries to transform
     * @param vaultId       the vault ID associated with these entries
     */
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
                                } else if (!part.contains("__") && part.matches(".*\\d.*")) {
                                    idCount++;
                                    tempPart = (idCount > 1) ? "{id" + idCount + "}" : "{id}";
                                }
                                apiResourceBuilder.append("/").append(tempPart);
                                lastPart = part;
                            }
                        }
                        apiEndpoint = apiResourceBuilder.toString();
                    }
                }
                apiLogEntry.set("api_endpoint", apiEndpoint);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
