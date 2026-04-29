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

public class SdkRuntimeLog {
    private Logger logger = LoggerFactory.getLogger(SdkRuntimeLog.class);
    private final int BATCH_SIZE = 500;

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
                        logger.error("Failed to generate JSON for runtime log: " + ex.getMessage(), ex);
                    }
                }

                logDate = logDate.plusDays(1);
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void analyze(File dbFile, File outputFile) {
        try {
            Sqlite sqlDb = new Sqlite(dbFile);
            if (outputFile == null) {
                String defaultOutputFileName = new SimpleDateFormat("vault-log-analyzer-runtime-yyyyMMdd-HHmmssSSS")
                        .format(ZonedDateTime.now()) + ".csv";
                String defaultOutputFilePath = FileSystems.getDefault().getPath(defaultOutputFileName)
                        .normalize().toAbsolutePath().toString();
                outputFile = new File(defaultOutputFilePath);
            }

            // IMPORTANT: Ensure you create a "runtime/stats.sql" file in your resources folder!
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

                if (statRows.size() > 0) {
                    csvMetadataWriter.writeAllRows(addHeader, append, outputFile, statRows);
                }
            }

        }
        catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void importLogFiles(File dbFile, File logDirectory) {
        importLogFiles(dbFile, logDirectory, new String());
    }

    public void importLogFiles(File dbFile, File logDirectory, String vaultId) {
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
            for (File logFile : logFiles) {
                if (vaultId == null || vaultId.isEmpty()) {
                    String fileName = logFile.getName();
                    String numberPattern = "^\\d+";

                    Pattern pattern = Pattern.compile(numberPattern);
                    Matcher matcher = pattern.matcher(fileName);

                    if (matcher.find()) {
                        vaultId = matcher.group();
                    } else {
                        vaultId = "unknown";
                    }
                }

                fileCount++;

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
                        if (!percentMap.keySet().contains(percent)) {
                            percentMap.put(percent, true);
                            StringBuilder progressBuilder = new StringBuilder();
                            progressBuilder.append(logFile.getName());
                            for (int i = 0; i < percent; i++) {
                                progressBuilder.append("_");
                            }
                            logger.info(progressBuilder.toString());
                        }

                        List<VaultModel> logEntries = logReader.getRows(BATCH_SIZE);
                        if (logEntries != null && logEntries.size() > 0) {
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

        }
        catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private void loadToSql(Sqlite sqlDb, String tableName, List<VaultModel> logEntries) {
        try {
            if (logEntries != null && logEntries.size() > 0) {
                sqlDb.startInsertStatement(tableName, logEntries.get(0));
                sqlDb.startInsertStatement("runtime", logEntries.get(0));
                for (VaultModel logEntry : logEntries) {
                    sqlDb.addInsertValues(tableName, logEntry);
                    sqlDb.addInsertValues("runtime", logEntry);
                }
                sqlDb.flushBuilders();
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private void transform(List<VaultModel> logEntries, String vaultId) {
        try {
            for (VaultModel logEntry : logEntries) {
                logEntry.set("vault_id", vaultId);
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}