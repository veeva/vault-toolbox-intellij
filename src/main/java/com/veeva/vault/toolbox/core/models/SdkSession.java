package com.veeva.vault.toolbox.core.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base class representing shared metadata fields for Vault SDK log session records.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SdkSession {

	@JsonProperty("log_date")
	private String logDate;
	private String md5checksum;
	private String fileName;

	/**
	 * Returns the log date of this session.
	 *
	 * @return the log date string
	 */
	public String getLogDate() {
		return logDate;
	}

	/**
	 * Sets the log date of this session.
	 *
	 * @param logDate the log date string
	 */
	public void setLogDate(String logDate) {
		this.logDate = logDate;
	}

	/**
	 * Returns the MD5 checksum of the associated log file.
	 *
	 * @return the MD5 checksum string
	 */
	public String getMd5checksum() {
		return md5checksum;
	}

	/**
	 * Sets the MD5 checksum of the associated log file.
	 *
	 * @param md5checksum the MD5 checksum string
	 */
	public void setMd5checksum(String md5checksum) {
		this.md5checksum = md5checksum;
	}

	/**
	 * Returns the log file name associated with this session.
	 *
	 * @return the file name
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Sets the log file name associated with this session.
	 *
	 * @param fileName the file name
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
