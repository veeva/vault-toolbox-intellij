package com.veeva.vault.toolbox.core.logs.sdk;

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
 * Manages the downloading, importing, and analysis of Vault SDK runtime logs.
 * SDK runtime logs are downloaded as ZIP files, imported into a SQLite database,
 * and analyzed to produce statistical reports in CSV format.
 */
public class SdkRuntimeLog {
    private static final Logger logger = LoggerFactory.getLogger(SdkRuntimeLog.class);
    private static final int BATCH_SIZE = 500;

    /**
     * Downloads daily SDK runtime logs for each date in the given range and saves them to the output directory.
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
                VaultResponse response = vaultClient.newRequest(LogRequest.class).downloadSdkRuntimeLog(logDate);

                if (response != null && !response.isFailure() && response.getBinaryContent() != null) {
                    String dateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(logDate);
                    String fileName = response.getHeaderVaultId() + "-SDKLog-" + dateStr + ".zip";
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
                        logger.error("Failed to generate JSON for runtime log: {}", ex.getMessage(), ex);
                    }
                }

                logDate = logDate.plusDays(1);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Executes the stats SQL against the given SQLite database and writes the results to the output CSV file.
     * If {@code outputFile} is {@code null}, a timestamped file is created in the current directory.
     *
     * @param dbFile     the SQLite database file containing imported SDK runtime log data
     * @param outputFile destination CSV file for the analysis results
     */
    public void analyze(File dbFile, File outputFile) {
        try {
            Sqlite sqlDb = new Sqlite(dbFile);
            if (outputFile == null) {
                String defaultOutputFileName = new SimpleDateFormat("vault-log-analyzer-runtime-yyyyMMdd-HHmmssSSS")
                        .format(ZonedDateTime.now()) + ".csv";
                outputFile = new File(FileSystems.getDefault()
                        .getPath(defaultOutputFileName).normalize().toAbsolutePath().toString());
            }

            String sql = new String(FileIO.getResourceContent("runtime/stats.sql", this.getClass().getClassLoader()));
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

            for (File logFile : logFiles) {
                if (vaultId == null || vaultId.isEmpty()) {
                    Pattern pattern = Pattern.compile("^\\d+");
                    Matcher matcher = pattern.matcher(logFile.getName());
                    vaultId = matcher.find() ? matcher.group() : "unknown";
                }

                long numLines = Files.lines(logFile.toPath()).count();
                if (numLines > 1) {
                    long totalBatches = (numLines + BATCH_SIZE - 1) / BATCH_SIZE;
                    CsvMetadataReader logReader = new CsvMetadataReader(logFile, VaultModel.class);
                    String sqlTableName = "vaultRuntime" + logFile.getName().replace("-", "").replace(".csv", "");

                    boolean createdTable = false;
                    int batchCount = 0;
                    Map<Long, Boolean> percentMap = new HashMap<>();

                    while (logReader.hasNext()) {
                        batchCount++;

                        long percent = 100 - ((batchCount * 100) / totalBatches);
                        if (!percentMap.containsKey(percent)) {
                            percentMap.put(percent, true);
                            StringBuilder progressBuilder = new StringBuilder();
                            progressBuilder.append(logFile.getName());
                            for (int i = 0; i < percent; i++) {
                                progressBuilder.append("_");
                            }
                            logger.info(progressBuilder.toString());
                        }

                        List<VaultModel> logEntries = logReader.getRows(BATCH_SIZE);
                        if (logEntries != null && !logEntries.isEmpty()) {
                            transform(logEntries, vaultId);
                            if (!createdTable) {
                                sqlDb.createTable(sqlTableName, logEntries.get(0).getFieldNames(), true);
                                sqlDb.createTable("runtime", logEntries.get(0).getFieldNames(), false);
                                createdTable = true;
                            }
                            loadToSql(sqlDb, sqlTableName, logEntries);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Loads a batch of SDK runtime log entries into the specified SQLite table and a global "runtime" table.
     *
     * @param sqlDb      the SQLite database connection
     * @param tableName  the specific table name for the log file
     * @param logEntries the batch of log entries to load
     */
    private void loadToSql(Sqlite sqlDb, String tableName, List<VaultModel> logEntries) {
        try {
            if (logEntries != null && !logEntries.isEmpty()) {
                sqlDb.startInsertStatement(tableName, logEntries.get(0));
                sqlDb.startInsertStatement("runtime", logEntries.get(0));
                for (VaultModel logEntry : logEntries) {
                    sqlDb.addInsertValues(tableName, logEntry);
                    sqlDb.addInsertValues("runtime", logEntry);
                }
                sqlDb.flushBuilders();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Transforms raw SDK runtime log entries by adding standard fields such as the vault ID.
     *
     * @param logEntries the batch of log entries to transform
     * @param vaultId    the vault ID associated with these entries
     */
    private void transform(List<VaultModel> logEntries, String vaultId) {
        try {
            for (VaultModel logEntry : logEntries) {
                logEntry.set("vault_id", vaultId);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
