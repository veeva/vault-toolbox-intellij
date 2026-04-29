package com.veeva.vault.toolbox.core.utils;

import com.opencsv.CSVReader;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileIO {
	private static final Logger logger = LoggerFactory.getLogger(FileIO.class);

	public static boolean endsWith(File file, Set<String> fileExtensions) {
		if (file != null && fileExtensions != null && !fileExtensions.isEmpty()) {
			for (String fileExtension : fileExtensions) {
				fileExtension = fileExtension.replace("*.", ".");

				if (file.getName().toLowerCase().endsWith(fileExtension)) {
					return true;
				}
				else if (fileExtension.equals("*") || fileExtension.equals("*.*")) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean endsWith(File file, String fileExtension) {
		if (file != null && fileExtension != null) {
			fileExtension = fileExtension.replace("*.", ".");

			if (file.getName().toLowerCase().endsWith(fileExtension)) {
				return true;
			}
			else if (fileExtension.equals("*") || fileExtension.equals("*.*")) {
				return true;
			}
		}
		return false;
	}

	public static List<String> getFileNames(File sourceFile, Set<String> fileExtensions) {
		try (Stream<Path> walk = Files.walk(sourceFile.toPath())) {

			return walk.map(x -> x.toString())
					.filter(file -> (fileExtensions == null
							|| endsWith(new File(file), fileExtensions)))
					.collect(Collectors.toList());

		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}


	public static List<String> getFileNames(File sourceFile, String extension) {
		try (Stream<Path> walk = Files.walk(sourceFile.toPath())) {

			return walk.map(x -> x.toString())
					.filter(file -> (endsWith(new File(file), extension)))
					.collect(Collectors.toList());

		} catch (IOException e) {
			logger.error(e.getMessage());
			return null;
		}
	}

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
			e.printStackTrace();
			return null;
		}
	}

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

	public static byte[] getResourceContent(String resourcePath, ClassLoader classLoader) throws IOException {
		byte[] resourceContent = null;
		if (classLoader != null) {
			URL resourceUrl = classLoader.getResource(resourcePath);
			if (resourceUrl != null) {
				logger.debug("URL: " + resourcePath);
				try {
					InputStream sourceStream = classLoader.getResourceAsStream(resourcePath);
					resourceContent = IOUtils.toByteArray(sourceStream);
				}
				catch (Exception e) {
					logger.error(e.getMessage());
					e.printStackTrace();
				}
			}
			else {
				logger.debug("NOURL: " + resourcePath);
			}
		}
		else {
			logger.debug("NOCLASSLOADER: " + resourcePath);
		}

		return resourceContent;
	}

	public static byte[] getResourceContent(String resourcePath) throws IOException {
		return getResourceContent(resourcePath, ClassLoader.getSystemClassLoader());
	}

	public static void makeDirectories(File directory) {
		try {
			if (directory != null && !directory.isFile() && !directory.exists()) {
				logger.debug("MKDIR: " + directory);
				directory.mkdirs();
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

	}

	public static void unzipFiles(File zipFile, File outputDirectory) {
		try {
			byte[] buffer = new byte[1024];
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.getAbsolutePath()));
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				File newFile = new File(outputDirectory, zipEntry.getName());
				String parentDirPath = newFile.getParent();
				File parentDir = new File(parentDirPath);
				makeDirectories(parentDir);

				if (!zipEntry.isDirectory()) {
					FileOutputStream fos = new FileOutputStream(newFile);
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
					fos.close();
				}
				logger.info("UNZIP: " + newFile.getAbsolutePath());
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
			zis.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void zipFiles(File zipFile,
								List<File> files,
								File relativeFile,
								String pathInZip,
								boolean appendToFile) throws Exception {
		if (files != null && !files.isEmpty()) {
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
						zipFilePath.append(pathInZip + "/");
					}

					if (relativeFile != null) {
						if (inputFile.getParentFile().compareTo(relativeFile) != 0) {
							String zipEntryPath = zipFile.getParentFile().getAbsolutePath();
							String sourceParentPath = inputFile.getParentFile().getAbsolutePath();
							if (!relativeFile.getAbsolutePath().equals(sourceParentPath)) {
								zipEntryPath = inputFile.getAbsolutePath().substring(relativeFile.getAbsolutePath().length()).replaceAll("\\\\", "/");
							}
							Path zipDirectory = zipfs.getPath(zipEntryPath).getParent();
							if (zipDirectory != null) {
								if (Files.notExists(zipDirectory)) {
									Files.createDirectories(zipDirectory);
								}
								zipFilePath.append(zipDirectory + "/");
							}
						}
					}
					zipFilePath.append(inputFile.getName());
					Path pathInZipfile = zipfs.getPath(zipFilePath.toString());
					// copy a file into the zip file
					Files.copy(inputFile.toPath(), pathInZipfile,
							StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	public static void writeFileContent(File outputFile, String fileContent) {
		try {
			writeFileContent(outputFile, fileContent.getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

	}

	public static void writeFileContent(File outputFile, byte[] fileContent) {
		try {
			if (outputFile != null && fileContent != null) {
				makeDirectories(outputFile.getParentFile());
				Files.write(outputFile.toPath(), fileContent);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

	}

	public static String toAbsolutePath(String maybeRelative) {
		Path path = Paths.get(maybeRelative);
		Path effectivePath = path;
		Path base = Paths.get("");
		effectivePath = base.resolve(path).toAbsolutePath();
		return effectivePath.normalize().toString();
	}

	public static boolean checkApiLogFileNameFormat(String fileName) {
		String format = "^[0-9]+-[a-zA-Z]+-\\d{4}-\\d{2}-\\d{2}$";

		return false;
	}

	public static  int getCsvRowCount(File csvFile) {
		try {
			int rowCount = 0;
			CSVReader csvReader = new CSVReader(new FileReader(csvFile));
			String[] values = null;
			while ((values = csvReader.readNext()) != null) {
				rowCount = rowCount + 1;
			}
			return rowCount;
		}
		catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return  0;
		}
	}

	public static byte[] getFileBytes(File inputFile) {
		if (inputFile != null) {
			if (inputFile.exists()) {
				try {
					return Files.readAllBytes(inputFile.toPath());
				}
				catch (Exception e) {
					return null;
				}
			}
		}
		return null;
	}

	public static String getFileContent(File inputFile) {
		if (inputFile != null) {
			if (inputFile.exists()) {
				try {
					return new String(getFileBytes(inputFile), StandardCharsets.UTF_8);
				}
				catch (Exception e) {
					logger.error(e.getMessage());
					return null;
				}
			}
		}
		return null;
	}

	public static String getFileContent(String inputFilePath) {
		if (inputFilePath != null) {
			return getFileContent(new File(inputFilePath));
		}
		else
			return null;
	}
}
