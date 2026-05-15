package com.veeva.vault.toolbox.core.logs.sdk;

import com.veeva.vault.toolbox.core.csv.CsvMetadataWriter;
import com.veeva.vault.toolbox.core.models.SdkDebugLogEntry;
import com.veeva.vault.toolbox.core.utils.Date;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.LogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages the downloading and analysis of Vault SDK debug logs.
 * Debug logs are parsed from text files, transformed into structured entries,
 * and exported to CSV format.
 */
public class SdkDebugLog {
    private static final Logger logger = LoggerFactory.getLogger(SdkDebugLog.class);
    private static final int BATCH_SIZE = 500;

    /** Tracks SYSDATA entries to associate request attributes with subsequent log lines. */
    private List<SdkDebugLogEntry> sysdataEntries = new ArrayList<>();
    /** The index of the current entry point being processed. */
    private int numEntryPoints = 0;

    /**
     * Downloads a Vault SDK debug log file for the given debug log ID.
     *
     * @param vaultClient authenticated Vault client
     * @param debugLogId  the ID of the debug log to download
     * @param outputFile  destination file for the downloaded log
     * @param unzip       if {@code true}, the downloaded ZIP is extracted to the same directory
     */
    public void downloadSdkLogs(VaultClient vaultClient, String debugLogId, File outputFile, boolean unzip) {
        VaultResponse response = vaultClient.newRequest(LogRequest.class)
                .setOutputPath(outputFile.getAbsolutePath())
                .downloadDebugLogFiles(debugLogId);
        if (response != null && !response.isFailure() && outputFile.exists() && unzip) {
            FileIO.unzipFiles(outputFile, outputFile.getParentFile());
        }
    }

    /**
     * Analyzes a list of SDK debug log files and writes the parsed results to the output CSV file.
     * If {@code outputFile} is {@code null}, a timestamped file is created in the current directory.
     *
     * @param inputFiles list of log files to analyze
     * @param outputFile destination CSV file for the analysis results
     */
    public void analyze(List<File> inputFiles, File outputFile) {
        try {
            if (outputFile == null) {
                String defaultOutputFileName = new SimpleDateFormat("vault-log-analyzer-debug-yyyyMMdd-HHmmssSSS")
                        .format(ZonedDateTime.now()) + ".csv";
                outputFile = new File(FileSystems.getDefault()
                        .getPath(defaultOutputFileName).normalize().toAbsolutePath().toString());
            }

            if (outputFile.exists()) {
                logger.warn("Deleting existing file [{}]", outputFile);
                outputFile.delete();
            }

            if (inputFiles != null) {
                for (File logFile : inputFiles) {
                    analyzeLogFile(logFile, outputFile);
                    numEntryPoints = 0;
                    sysdataEntries.clear();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Analyzes an SDK debug log file or directory of log files and writes the parsed results to the output CSV file.
     * If {@code inputFile} is {@code null}, defaults to {@code /logs}.
     * If {@code outputFile} is {@code null}, a timestamped file is created in the current directory.
     *
     * @param inputFile  a single log file or a directory containing {@code .txt} log files to analyze
     * @param outputFile destination CSV file for the analysis results
     */
    public void analyze(File inputFile, File outputFile) {
        try {
            if (inputFile == null) {
                inputFile = new File(FileSystems.getDefault()
                        .getPath("/logs").normalize().toAbsolutePath().toString());
            }

            if (outputFile == null) {
                String defaultOutputFileName = new SimpleDateFormat("vault-log-analyzer-debug-yyyyMMdd-HHmmssSSS")
                        .format(ZonedDateTime.now()) + ".csv";
                outputFile = new File(FileSystems.getDefault()
                        .getPath(defaultOutputFileName).normalize().toAbsolutePath().toString());
            }

            if (outputFile.exists()) {
                logger.warn("Deleting existing file [{}]", outputFile);
                outputFile.delete();
            }

            if (inputFile.isDirectory()) {
                for (File logFile : FileIO.getFiles(inputFile, ".txt")) {
                    analyzeLogFile(logFile, outputFile);
                    numEntryPoints = 0;
                    sysdataEntries.clear();
                }
            } else {
                analyzeLogFile(inputFile, outputFile);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Parses a single SDK debug log file and writes the structured entries to the output CSV.
     *
     * @param logFile    the source text log file
     * @param outputFile the destination CSV file
     */
    private void analyzeLogFile(File logFile, File outputFile) {
        try {
            logger.info("Parsing {}", logFile.getAbsolutePath());

            CsvMetadataWriter csvMetadataWriter = new CsvMetadataWriter();
            csvMetadataWriter.addColumn("timestamp");
            csvMetadataWriter.addColumn("execution_id");
            csvMetadataWriter.addColumn("vault_id");
            csvMetadataWriter.addColumn("user_id");
            csvMetadataWriter.addColumn("transaction_id");
            csvMetadataWriter.addColumn("log_file");
            csvMetadataWriter.addColumn("type");
            csvMetadataWriter.addColumn("category");
            csvMetadataWriter.addColumn("class_name");
            csvMetadataWriter.addColumn("service_method");
            csvMetadataWriter.addColumn("service_name");
            csvMetadataWriter.addColumn("method_name");
            csvMetadataWriter.addColumn("elapsed_time_ms");
            csvMetadataWriter.addColumn("elapsed_time_seconds");
            csvMetadataWriter.addColumn("cpu_time_ns");
            csvMetadataWriter.addColumn("cpu_time_seconds");
            csvMetadataWriter.addColumn("memory");
            csvMetadataWriter.addColumn("memory_mb");
            csvMetadataWriter.addColumn("gross_memory");
            csvMetadataWriter.addColumn("gross_memory_mb");
            csvMetadataWriter.addColumn("invocation_count");
            csvMetadataWriter.addColumn("message");

            List<SdkDebugLogEntry> sdkDebugLogEntries = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            String lineBuffer = reader.readLine();
            SdkDebugLogEntry lastSdkDebugLogEntry = null;

            if (lineBuffer != null) {
                while (lineBuffer != null) {
                    if (lineBuffer.length() > 23 && Date.isDateTime(lineBuffer.substring(0, 23))) {
                        if (sdkDebugLogEntries.size() == BATCH_SIZE) {
                            csvMetadataWriter.writeAllRows(!outputFile.exists(), outputFile.exists(), outputFile, sdkDebugLogEntries);
                            sdkDebugLogEntries.clear();
                        }
                        SdkDebugLogEntry sdkDebugLogEntry = transformLine(lineBuffer, reader);
                        sdkDebugLogEntry.setLogFile(logFile.getName());
                        sdkDebugLogEntries.add(sdkDebugLogEntry);
                        lastSdkDebugLogEntry = sdkDebugLogEntry;
                    } else if (lastSdkDebugLogEntry != null) {
                        lastSdkDebugLogEntry.setMessage(lastSdkDebugLogEntry.getMessage() + " " + lineBuffer);
                    }

                    lineBuffer = reader.readLine();
                }

                if (!sdkDebugLogEntries.isEmpty()) {
                    csvMetadataWriter.writeAllRows(!outputFile.exists(), outputFile.exists(), outputFile, sdkDebugLogEntries);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Transforms a raw log line into a structured {@link SdkDebugLogEntry}.
     * Handles different log types such as SYSDATA, SYSERR, SYSWRN, SYSINFO, and PERF.
     *
     * @param lineBuffer the raw log line starting with a timestamp
     * @param reader     the reader for accessing subsequent lines (e.g., for stack traces)
     * @return the parsed log entry
     */
    private SdkDebugLogEntry transformLine(String lineBuffer, BufferedReader reader) {
        try {
            SdkDebugLogEntry sdkDebugLogEntry = new SdkDebugLogEntry();

            sdkDebugLogEntry.setTimestamp(lineBuffer.substring(0, 23));

            String tempBuffer = lineBuffer.substring(24);
            sdkDebugLogEntry.setClassName(tempBuffer.substring(0, tempBuffer.indexOf(" ")));

            tempBuffer = tempBuffer.substring(tempBuffer.indexOf(" ") + 1);
            sdkDebugLogEntry.setType(tempBuffer.substring(0, tempBuffer.indexOf(" ")));

            tempBuffer = tempBuffer.substring(tempBuffer.indexOf(" ") + 1).trim();

            switch (sdkDebugLogEntry.getType()) {
                case "SYSDATA":
                    sdkDebugLogEntry.setCategory("REQUEST");
                    tempBuffer = tempBuffer.substring(tempBuffer.indexOf("{\"executionId\":\"") + 16);
                    sdkDebugLogEntry.setExecutionId(tempBuffer.substring(0, tempBuffer.indexOf("\"")));

                    tempBuffer = tempBuffer.substring(tempBuffer.indexOf("\",\"vaultId\":") + 12);
                    sdkDebugLogEntry.setVaultId(tempBuffer.substring(0, tempBuffer.indexOf(",")));

                    tempBuffer = tempBuffer.substring(tempBuffer.indexOf(",\"userId\":") + 10);
                    sdkDebugLogEntry.setUserId(tempBuffer.substring(0, tempBuffer.indexOf(",")));

                    tempBuffer = tempBuffer.substring(tempBuffer.indexOf(",\"transactionId\":\"") + 18);
                    sdkDebugLogEntry.setTransactionId(tempBuffer.substring(0, tempBuffer.indexOf("\"")));

                    sysdataEntries.add(sdkDebugLogEntry);
                    numEntryPoints++;
                    break;
                case "SYSERR":
                    sdkDebugLogEntry.setCategory("EXCEPTION");
                    StringBuilder exceptionBuilder = new StringBuilder();
                    String errorBuffer = tempBuffer;
                    while (errorBuffer != null) {
                        exceptionBuilder.append(errorBuffer).append(" ");
                        errorBuffer = reader.readLine();
                    }
                    sdkDebugLogEntry.setMessage(exceptionBuilder.toString());
                    setRequestAttributes(sdkDebugLogEntry);
                    break;
                case "SYSWRN":
                    sdkDebugLogEntry.setCategory("ALERT");
                    StringBuilder warningBuilder = new StringBuilder();
                    String warningBuffer = tempBuffer;
                    while (warningBuffer != null) {
                        warningBuilder.append(warningBuffer).append("\n");
                        warningBuffer = reader.readLine();
                    }
                    sdkDebugLogEntry.setMessage(warningBuilder.toString());
                    setRequestAttributes(sdkDebugLogEntry);
                    break;
                case "SYSINFO":
                    if (tempBuffer.startsWith("*****Start Execution")) {
                        sdkDebugLogEntry.setCategory("ENTRY_POINT_START");
                        sdkDebugLogEntry.setClassName(lineBuffer.substring(lineBuffer.indexOf("[") + 1, lineBuffer.indexOf("]")));
                    } else if (tempBuffer.startsWith("*****End Execution")) {
                        sdkDebugLogEntry.setCategory("ENTRY_POINT_END");
                        sdkDebugLogEntry.setClassName(lineBuffer.substring(lineBuffer.indexOf("[") + 1, lineBuffer.indexOf("]")));
                    } else if (tempBuffer.startsWith("HttpRequest:") || tempBuffer.startsWith("HttpResponse:")) {
                        sdkDebugLogEntry.setCategory("HTTPSERVICE");
                        sdkDebugLogEntry.setMessage(tempBuffer);
                    } else {
                        sdkDebugLogEntry.setCategory("SERVICE");
                        if (tempBuffer.startsWith("com.veeva.vault")) {
                            sdkDebugLogEntry.setServiceMethod(tempBuffer.substring(0, tempBuffer.indexOf(" - [")));

                            String tempPerf = tempBuffer.substring(tempBuffer.indexOf(" - [") + 4, tempBuffer.length() - 1);
                            List<String> metrics = Arrays.asList(tempPerf.split(", "));
                            for (String metric : metrics) {
                                if (metric.startsWith("count")) {
                                    sdkDebugLogEntry.setInvocationCount(Long.valueOf(metric.substring(6)));
                                } else if (metric.startsWith("elapsed")) {
                                    sdkDebugLogEntry.setElapsedTime(Long.valueOf(metric.substring(12)));
                                } else if (metric.startsWith("CPU")) {
                                    sdkDebugLogEntry.setCpuTime(Long.valueOf(metric.substring(8)));
                                } else if (metric.startsWith("memory")) {
                                    sdkDebugLogEntry.setMemory(Long.valueOf(metric.substring(10)));
                                } else if (metric.startsWith("grossMemory")) {
                                    sdkDebugLogEntry.setGrossMemory(Long.valueOf(metric.substring(15)));
                                }
                            }
                        } else {
                            sdkDebugLogEntry.setMessage(tempBuffer);
                        }
                    }
                    setRequestAttributes(sdkDebugLogEntry);
                    break;
                case "PERF":
                    if (lineBuffer.contains("\"")) {
                        sdkDebugLogEntry.setCategory("LOGSERVICE");
                        sdkDebugLogEntry.setMessage(tempBuffer.substring(1, tempBuffer.indexOf("\":")));
                        tempBuffer = tempBuffer.substring(tempBuffer.indexOf("\":") + 2);
                        setRequestAttributes(sdkDebugLogEntry);
                    } else {
                        sdkDebugLogEntry.setCategory("SYSPERF");
                        SdkDebugLogEntry matchingEntry = sysdataEntries.stream()
                                .filter(entry -> sdkDebugLogEntry.getClassName().equals(entry.getClassName()))
                                .findFirst()
                                .get();
                        sdkDebugLogEntry.setExecutionId(matchingEntry.getExecutionId());
                        sdkDebugLogEntry.setVaultId(matchingEntry.getVaultId());
                        sdkDebugLogEntry.setUserId(matchingEntry.getUserId());
                        sdkDebugLogEntry.setTransactionId(matchingEntry.getTransactionId());
                    }

                    List<String> metrics = Arrays.asList(tempBuffer.split(" "));
                    for (String metric : metrics) {
                        if (metric.startsWith("elapsed")) {
                            sdkDebugLogEntry.setElapsedTime(Long.valueOf(metric.substring(12)));
                        } else if (metric.startsWith("CPU")) {
                            sdkDebugLogEntry.setCpuTime(Long.valueOf(metric.substring(8)));
                        } else if (metric.startsWith("memory")) {
                            sdkDebugLogEntry.setMemory(Long.valueOf(metric.substring(10)));
                        }
                    }
                    break;
                case "DEBUG":
                case "ERROR":
                case "INFO":
                case "WARN":
                    sdkDebugLogEntry.setCategory("LOGSERVICE");
                    sdkDebugLogEntry.setMessage(tempBuffer);
                    setRequestAttributes(sdkDebugLogEntry);
                    break;
            }

            return sdkDebugLogEntry;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Copies request-level attributes (execution ID, vault ID, etc.) from the most recent SYSDATA entry.
     *
     * @param sdkDebugLogEntry the entry to update
     * @return the updated entry
     */
    private SdkDebugLogEntry setRequestAttributes(SdkDebugLogEntry sdkDebugLogEntry) {
        SdkDebugLogEntry sysDataEntry = sysdataEntries.get(numEntryPoints - 1);
        sdkDebugLogEntry.setExecutionId(sysDataEntry.getExecutionId());
        sdkDebugLogEntry.setVaultId(sysDataEntry.getVaultId());
        sdkDebugLogEntry.setUserId(sysDataEntry.getUserId());
        sdkDebugLogEntry.setTransactionId(sysDataEntry.getTransactionId());
        return sdkDebugLogEntry;
    }
}
