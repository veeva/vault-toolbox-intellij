/*---------------------------------------------------------------------
 *	Copyright (c) 2020 Veeva Systems Inc.  All Rights Reserved.
 *	This code is based on pre-existing content developed and
 *	owned by Veeva Systems Inc. and may only be used in connection
 *	with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.toolbox.core.utils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Utility methods for computing cryptographic checksums of files, streams,
 * and strings.
 */
public final class Checksum {

	private static final Logger logger = LoggerFactory.getLogger(Checksum.class);

	private static final int BUFFER_SIZE = 1024;

	private Checksum() {
	}

	/**
	 * Computes a hex-encoded digest of the given input stream using the named
	 * algorithm. The supplied stream is fully consumed and closed.
	 *
	 * @param inputStream the stream to hash
	 * @param hashType the {@link MessageDigest} algorithm name (for example {@code "MD5"} or {@code "SHA-256"})
	 * @return the lowercase hex digest, or {@code null} if the algorithm is unsupported or the stream cannot be read
	 */
	public static String getHash(InputStream inputStream, String hashType) {
		try (InputStream in = inputStream) {
			MessageDigest digest = MessageDigest.getInstance(hashType);
			byte[] buffer = new byte[BUFFER_SIZE];
			int read;
			while ((read = in.read(buffer)) != -1) {
				digest.update(buffer, 0, read);
			}
			return toHex(digest.digest());
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * Computes the MD5 digest of the given UTF-8 string.
	 *
	 * @param txt the text to hash
	 * @return the lowercase hex digest, or {@code null} if the text cannot be hashed
	 */
	public static String getMd5(String txt) {
		return getHash(IOUtils.toInputStream(txt, StandardCharsets.UTF_8), "MD5");
	}

	/**
	 * Computes the MD5 digest of the given file.
	 *
	 * @param file the file to hash
	 * @return the lowercase hex digest, or {@code null} if the file cannot be read
	 */
	public static String getMd5(File file) {
		try {
			return getHash(new FileInputStream(file), "MD5");
		} catch (Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	/**
	 * Computes the MD5 digest of the given input stream.
	 *
	 * @param inputStream the stream to hash
	 * @return the lowercase hex digest, or {@code null} if the stream cannot be read
	 */
	public static String getMd5(InputStream inputStream) {
		return getHash(inputStream, "MD5");
	}

	/**
	 * Converts a byte array into a lowercase hex string.
	 *
	 * @param bytes the bytes to convert
	 * @return the hex string representation
	 */
	private static String toHex(byte[] bytes) {
		StringBuilder hex = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			hex.append(String.format("%02x", b & 0xff));
		}
		return hex.toString();
	}
}
