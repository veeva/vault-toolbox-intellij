package com.veeva.vault.toolbox.core.utils;

import com.opencsv.CSVReader;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility methods for file system operations including reading, writing,
 * directory traversal, and ZIP archive handling.
 */
public final class FileIO {

	private static final Logger logger = LoggerFactory.getLogger(FileIO.class);

	private static final int BUFFER_SIZE = 1024;

	private FileIO() {
	}

	/**
	 * Returns {@code true} if the given file's name ends with any of the supplied
	 * extensions. The wildcard tokens {@code "*"} and {@code "*.*"} match any file.
	 *
	 * @param file the file to test
	 * @param fileExtensions the set of extensions to match (for example {@code ".mdl"} or {@code "*.vpk"})
	 * @return {@code true} if the file matches any extension; {@code false} otherwise
	 */
	public static boolean endsWith(File file, Set<String> fileExtensions) {
		if (file == null || fileExtensions == null || fileExtensions.isEmpty()) {
			return false;
		}
		for (String fileExtension : fileExtensions) {
			if (endsWith(file, fileExtension)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns {@code true} if the given file's name ends with the supplied
	 * extension. The wildcard tokens {@code "*"} and {@code "*.*"} match any file.
	 *
	 * @param file the file to test
	 * @param fileExtension the extension to match (for example {@code ".mdl"} or {@code "*.vpk"})
	 * @return {@code true} if the file matches the extension; {@code false} otherwise
	 */
	public static boolean endsWith(File file, String fileExtension) {
		if (file == null || fileExtension == null) {
			return false;
		}
		if (fileExtension.equals("*") || fileExtension.equals("*.*")) {
			return true;
		}
		String normalized = fileExtension.replace("*.", ".");
		return file.getName().toLowerCase().endsWith(normalized);
	}

	/**
	 * Recursively walks the given path and returns the absolute paths of files
	 * matching any of the provided extensions.
	 *
	 * @param sourceFile the directory or file to walk
	 * @param fileExtensions the extensions to filter by, or {@code null} to include all paths
	 * @return matching file paths, or {@code null} if an I/O error occurred
	 */
	public static List<String> getFileNames(File sourceFile, Set<String> fileExtensions) {
		try (Stream<Path> walk = Files.walk(sourceFile.toPath())) {
			return walk.map(Path::toString)
					.filter(file -> fileExtensions == null || endsWith(new File(file), fileExtensions))
					.collect(Collectors.toList());
		} catch (IOException e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * Recursively walks the given path and returns the absolute paths of files
	 * matching the provided extension.
	 *
	 * @param sourceFile the directory or file to walk
	 * @param extension the extension to filter by
	 * @return matching file paths, or {@code null} if an I/O error occurred
	 */
	public static List<String> getFileNames(File sourceFile, String extension) {
		try (Stream<Path> walk = Files.walk(sourceFile.toPath())) {
			return walk.map(Path::toString)
					.filter(file -> endsWith(new File(file), extension))
					.collect(Collectors.toList());
		} catch (IOException e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * Returns a sorted list of files matching the provided extensions. If
	 * {@code source} is a single file, it is returned as the only element.
	 *
	 * @param source the directory or file to inspect
	 * @param fileExtensions the extensions to filter by, or {@code null} to include all
	 * @return matching files, or {@code null} if an error occurred
	 */
	public static List<File> getFiles(File source, Set<String> fileExtensions) {
		try {
			List<File> files = new ArrayList<>();
			if (source.isDirectory()) {
				List<String> fileNames = getFileNames(source, fileExtensions);
				Collections.sort(fileNames);
				for (String filePath : fileNames) {
					files.add(new File(filePath));
				}
			}
			else {
				files.add(source);
			}
			return files;
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * Returns a sorted list of files matching the provided extension. If
	 * {@code source} is a single file, it is returned as the only element.
	 *
	 * @param source the directory or file to inspect
	 * @param extension the extension to filter by
	 * @return matching files, or {@code null} if an error occurred
	 */
	public static List<File> getFiles(File source, String extension) {
		try {
			List<File> files = new ArrayList<>();
			if (source.isDirectory()) {
				List<String> fileNames = getFileNames(source, extension);
				Collections.sort(fileNames);
				for (String filePath : fileNames) {
					files.add(new File(filePath));
				}
			}
			else {
				files.add(source);
			}
			return files;
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * Reads the given resource using the provided class loader.
	 *
	 * @param resourcePath the resource path
	 * @param classLoader the class loader used to locate the resource
	 * @return the resource bytes, or {@code null} if the resource cannot be located or read
	 * @throws IOException if an I/O error occurs reading the stream
	 */
	public static byte[] getResourceContent(String resourcePath, ClassLoader classLoader) throws IOException {
		if (classLoader == null) {
			logger.debug("NOCLASSLOADER: " + resourcePath);
			return null;
		}
		try (InputStream stream = classLoader.getResourceAsStream(resourcePath)) {
			if (stream == null) {
				logger.debug("NOURL: " + resourcePath);
				return null;
			}
			logger.debug("URL: " + resourcePath);
			return IOUtils.toByteArray(stream);
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * Reads the given resource using the system class loader.
	 *
	 * @param resourcePath the resource path
	 * @return the resource bytes, or {@code null} if the resource cannot be located or read
	 * @throws IOException if an I/O error occurs reading the stream
	 */
	public static byte[] getResourceContent(String resourcePath) throws IOException {
		return getResourceContent(resourcePath, ClassLoader.getSystemClassLoader());
	}

	/**
	 * Creates the given directory and any missing parent directories. Has no
	 * effect if the path already exists or refers to an existing file.
	 *
	 * @param directory the directory to create
	 */
	public static void makeDirectories(File directory) {
		try {
			if (directory != null && !directory.isFile() && !directory.exists()) {
				logger.debug("MKDIR: " + directory);
				directory.mkdirs();
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Extracts the contents of the given ZIP file into the supplied output
	 * directory. Entries that resolve outside the output directory are skipped
	 * to guard against path traversal.
	 *
	 * @param zipFile the ZIP file to extract
	 * @param outputDirectory the directory to extract into
	 */
	public static void unzipFiles(File zipFile, File outputDirectory) {
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.getAbsolutePath()))) {
			byte[] buffer = new byte[BUFFER_SIZE];
			Path outputRoot = outputDirectory.toPath().normalize();
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				File newFile = new File(outputDirectory, zipEntry.getName());
				if (!newFile.toPath().normalize().startsWith(outputRoot)) {
					logger.warn("SKIP: entry outside target directory: " + zipEntry.getName());
					zipEntry = zis.getNextEntry();
					continue;
				}
				makeDirectories(newFile.getParentFile());

				if (!zipEntry.isDirectory()) {
					try (FileOutputStream fos = new FileOutputStream(newFile)) {
						int len;
						while ((len = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
					}
				}
				logger.info("UNZIP: " + newFile.getAbsolutePath());
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Adds the given files to a ZIP archive at {@code zipFile}.
	 *
	 * @param zipFile the destination archive
	 * @param files the files to add
	 * @param relativeFile if non-null, files are stored relative to this directory inside the archive
	 * @param pathInZip optional prefix path inside the archive
	 * @param appendToFile when {@code true}, append to an existing archive; when {@code false}, the existing archive is replaced
	 * @throws Exception if the archive cannot be created or written to
	 */
	public static void zipFiles(File zipFile,
								List<File> files,
								File relativeFile,
								String pathInZip,
								boolean appendToFile) throws Exception {
		if (files == null || files.isEmpty()) {
			return;
		}
		makeDirectories(zipFile.getParentFile());
		if (!appendToFile && zipFile.exists()) {
			logger.warn("RECREATE: " + zipFile.getAbsolutePath());
			zipFile.delete();
		}
		else if (!zipFile.exists()) {
			logger.warn("CREATE: " + zipFile.getAbsolutePath());
		}
		else {
			logger.warn("APPEND: " + zipFile.getAbsolutePath());
		}

		Map<String, Object> env = new HashMap<>();
		env.put("create", "true");
		env.put("useTempFile", Boolean.TRUE.toString());
		URI uri = URI.create("jar:" + zipFile.toPath().toUri());
		try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
			for (File inputFile : files) {
				logger.info("ZIP: " + inputFile.getAbsolutePath());

				StringBuilder zipFilePath = new StringBuilder();
				if (pathInZip != null) {
					Path zipDirectory = zipfs.getPath(pathInZip);
					if (Files.notExists(zipDirectory)) {
						Files.createDirectories(zipDirectory);
					}
					zipFilePath.append(pathInZip).append("/");
				}

				if (relativeFile != null && inputFile.getParentFile().compareTo(relativeFile) != 0) {
					String zipEntryPath = zipFile.getParentFile().getAbsolutePath();
					String sourceParentPath = inputFile.getParentFile().getAbsolutePath();
					if (!relativeFile.getAbsolutePath().equals(sourceParentPath)) {
						zipEntryPath = inputFile.getAbsolutePath()
								.substring(relativeFile.getAbsolutePath().length())
								.replaceAll("\\\\", "/");
					}
					Path zipDirectory = zipfs.getPath(zipEntryPath).getParent();
					if (zipDirectory != null) {
						if (Files.notExists(zipDirectory)) {
							Files.createDirectories(zipDirectory);
						}
						zipFilePath.append(zipDirectory).append("/");
					}
				}
				zipFilePath.append(inputFile.getName());
				Path pathInZipfile = zipfs.getPath(zipFilePath.toString());
				Files.copy(inputFile.toPath(), pathInZipfile, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	/**
	 * Writes the given text to a file using UTF-8 encoding. Parent directories
	 * are created as needed.
	 *
	 * @param outputFile the destination file
	 * @param fileContent the text to write
	 */
	public static void writeFileContent(File outputFile, String fileContent) {
		if (fileContent == null) {
			return;
		}
		writeFileContent(outputFile, fileContent.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Writes the given bytes to a file. Parent directories are created as needed.
	 *
	 * @param outputFile the destination file
	 * @param fileContent the bytes to write
	 */
	public static void writeFileContent(File outputFile, byte[] fileContent) {
		if (outputFile == null || fileContent == null) {
			return;
		}
		try {
			makeDirectories(outputFile.getParentFile());
			Files.write(outputFile.toPath(), fileContent);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * Resolves the given path against the current working directory and returns
	 * the normalized absolute path.
	 *
	 * @param maybeRelative a path that may be relative
	 * @return the normalized absolute path
	 */
	public static String toAbsolutePath(String maybeRelative) {
		Path path = Paths.get(maybeRelative);
		Path base = Paths.get("");
		return base.resolve(path).toAbsolutePath().normalize().toString();
	}

	/**
	 * Counts the number of rows in the given CSV file.
	 *
	 * @param csvFile the CSV file to count
	 * @return the number of rows, or {@code 0} if the file cannot be read
	 */
	public static int getCsvRowCount(File csvFile) {
		try (CSVReader csvReader = new CSVReader(new FileReader(csvFile))) {
			int rowCount = 0;
			while (csvReader.readNext() != null) {
				rowCount++;
			}
			return rowCount;
		} catch (Exception e) {
			logger.error(e.getMessage());
			return 0;
		}
	}

	/**
	 * Reads the entire contents of the given file into a byte array.
	 *
	 * @param inputFile the file to read
	 * @return the file contents, or {@code null} if the file is missing or cannot be read
	 */
	public static byte[] getFileBytes(File inputFile) {
		if (inputFile == null || !inputFile.exists()) {
			return null;
		}
		try {
			return Files.readAllBytes(inputFile.toPath());
		} catch (IOException e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * Reads the entire contents of the given file as a UTF-8 string.
	 *
	 * @param inputFile the file to read
	 * @return the file contents, or {@code null} if the file is missing or cannot be read
	 */
	public static String getFileContent(File inputFile) {
		byte[] bytes = getFileBytes(inputFile);
		return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
	}

	/**
	 * Reads the entire contents of the file at the given path as a UTF-8 string.
	 *
	 * @param inputFilePath the path of the file to read
	 * @return the file contents, or {@code null} if the path is null or the file cannot be read
	 */
	public static String getFileContent(String inputFilePath) {
		return inputFilePath == null ? null : getFileContent(new File(inputFilePath));
	}
}
