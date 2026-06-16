package com.veeva.vault.toolbox.core.logs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.veeva.vault.toolbox.core.utils.FileIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core utility for importing and structuring developer log archives.
 */
public class LogArchiveImporter {
    private static final Logger logger = LoggerFactory.getLogger(LogArchiveImporter.class);
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern SESSION_PATTERN = Pattern.compile("([^.]+)\\.([^.]+)");

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".csv", ".log", ".txt", ".json");

    /**
     * Peeks into the zip central directory (no extraction) to validate the archive contains
     * supported log files and no unsupported file types.
     *
     * @param archiveFile The zip archive to check.
     * @return null if the archive is valid, or a human-readable error message if not.
     */
    public static String validateLogArchive(File archiveFile) {
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(archiveFile)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();
            boolean hasValidFile = false;
            List<String> unsupportedExtensions = new ArrayList<>();

            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;

                String name = entry.getName().toLowerCase();
                int dotIndex = name.lastIndexOf('.');
                String ext = dotIndex >= 0 ? name.substring(dotIndex) : "";

                if (SUPPORTED_EXTENSIONS.contains(ext)) {
                    hasValidFile = true;
                } else if (!ext.isEmpty() && !unsupportedExtensions.contains(ext)) {
                    unsupportedExtensions.add(ext);
                }
            }

            if (!unsupportedExtensions.isEmpty() || !hasValidFile) {
                return "The selected file is not a valid Vault log archive.";
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to validate log archive: " + archiveFile.getName(), e);
            return "The selected file is not a valid Vault log archive.";
        }
    }

    /**
     * Imports the specified zip archive.
     *
     * @param archiveFile     The zip archive to import.
     * @param vaultId         The confirmed Vault ID.
     * @param logType         The type of log being imported ("API_USAGE", "SDK_DEBUG", "SDK_PROFILER", "SDK_RUNTIME").
     * @param targetDirectory The base target directory for this log type (e.g., toolbox/logs/api/{vaultId}).
     * @return true if successful, false otherwise.
     */
    public boolean importArchive(File archiveFile, String vaultId, String logType, File targetDirectory) {
        try {
            FileIO.makeDirectories(targetDirectory);
            ObjectMapper mapper = new ObjectMapper();
            String archiveName = archiveFile.getName().toLowerCase();
            if (archiveName.endsWith(".zip")) {
                return importZip(archiveFile, logType, targetDirectory, mapper);
            } else {
                return processFile(archiveFile, archiveFile, logType, targetDirectory, mapper);
            }
        } catch (Exception e) {
            logger.error("Error importing log archive", e);
            return false;
        }
    }

    private boolean importZip(File archiveFile, String logType, File targetDirectory, ObjectMapper mapper) {
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("vault-toolbox-import-").toFile();
            FileIO.unzipFiles(archiveFile, tempDir);

            File[] extracted = tempDir.listFiles();
            if (extracted == null || extracted.length == 0) {
                logger.warn("No files found in archive: " + archiveFile.getName());
                return false;
            }

            boolean anyProcessed = false;
            for (File file : extracted) {
                if (file.isDirectory()) {
                    List<File> nestedFiles = FileIO.getFiles(file, "");
                    if (nestedFiles != null) {
                        for (File nested : nestedFiles) {
                            if (!nested.isDirectory() && processFile(nested, archiveFile, logType, targetDirectory, mapper)) {
                                anyProcessed = true;
                            }
                        }
                    }
                } else if (processFile(file, archiveFile, logType, targetDirectory, mapper)) {
                    anyProcessed = true;
                }
            }
            return anyProcessed;
        } catch (Exception e) {
            logger.error("Error extracting zip archive", e);
            return false;
        } finally {
            if (tempDir != null) {
                try {
                    org.apache.commons.io.FileUtils.deleteDirectory(tempDir);
                } catch (Exception ex) {
                    logger.error("Failed to delete temp dir", ex);
                }
            }
        }
    }

    private boolean processFile(File file, File archiveFile, String logType, File targetDirectory, ObjectMapper mapper) throws Exception {
        String fileName = file.getName();

        if ("API_USAGE".equals(logType) || "SDK_RUNTIME".equals(logType)) {
            Matcher dateMatcher = DATE_PATTERN.matcher(fileName);
            if (dateMatcher.find()) {
                String dateStr = dateMatcher.group(1);
                File dateDir = new File(targetDirectory, dateStr);
                FileIO.makeDirectories(dateDir);

                File destFile = new File(dateDir, fileName);
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                String md5 = calculateMd5(destFile);
                ObjectNode jsonNode = mapper.createObjectNode();
                jsonNode.put("log_date", dateStr);
                jsonNode.put("md5checksum", md5);
                jsonNode.put("fileName", fileName);

                File jsonFile = new File(dateDir, fileName.substring(0, fileName.lastIndexOf('.')) + ".json");
                FileIO.writeFileContent(jsonFile, jsonNode.toPrettyString().getBytes());
                return true;
            }
        } else if ("SDK_DEBUG".equals(logType) || "SDK_PROFILER".equals(logType)) {
            String baseName = archiveFile.getName();
            int dotIndex = baseName.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = baseName.substring(0, dotIndex);
            }

            String sessionName = baseName;
            String sessionId = "imported";

            Matcher sessionMatcher = SESSION_PATTERN.matcher(baseName);
            if (sessionMatcher.find()) {
                sessionName = sessionMatcher.group(1);
                sessionId = sessionMatcher.group(2);
            }

            File sessionDir = new File(targetDirectory, sessionName + "." + sessionId);
            FileIO.makeDirectories(sessionDir);

            File destFile = new File(sessionDir, fileName);
            Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            File jsonFile = new File(sessionDir, sessionName + "." + sessionId + ".json");
            if (!jsonFile.exists()) {
                ObjectNode jsonNode = mapper.createObjectNode();
                jsonNode.put("id", sessionId);
                jsonNode.put("name", sessionName);
                FileIO.writeFileContent(jsonFile, jsonNode.toPrettyString().getBytes());
            }
            return true;
        }
        return false;
    }

    private String calculateMd5(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(Files.readAllBytes(file.toPath()));
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Failed to calculate MD5", e);
            return "";
        }
    }
}
