/*---------------------------------------------------------------------
 *	Copyright (c) 2020 Veeva Systems Inc.  All Rights Reserved.
 *	This code is based on pre-existing content developed and
 *	owned by Veeva Systems Inc. and may only be used in connection
 *	with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.toolbox.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class Checksum {

	public static String getHash(InputStream inputStream, String hashType) {
		try {
			MessageDigest md5er = MessageDigest.getInstance(hashType);
			byte[] buffer = new byte[1024];
			int read;
			do {
				read = inputStream.read(buffer);
				if (read > 0)
					md5er.update(buffer, 0, read);
			} while (read != -1);
			inputStream.close();
			byte[] digest = md5er.digest();
			if (digest == null)
				return null;
			StringBuilder checksum = new StringBuilder();
			for (int i = 0; i < digest.length; i++) {
				checksum.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1).toLowerCase());
			}
			return checksum.toString();
		} catch (Exception e) {
			return null;
		}
	}

	public static String getMd5(String txt) {
		try {
			return getHash(org.apache.commons.io.IOUtils.toInputStream(txt, "UTF-8"), "MD5");
		} catch (Exception e) {
			return null;
		}
	}

	public static String getMd5(File file) {
		try {
			return getHash(new FileInputStream(file), "MD5");
		} catch (Exception e) {
			return null;
		}
	}

	public static String getMd5(InputStream inputStream) {
		try {
			return getHash(inputStream, "MD5");
		} catch (Exception e) {
			return null;
		}
	}
}
