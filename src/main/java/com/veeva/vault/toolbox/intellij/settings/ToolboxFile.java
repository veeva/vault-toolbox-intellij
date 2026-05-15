package com.veeva.vault.toolbox.intellij.settings;

import com.veeva.vault.vapil.api.model.VaultModel;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tracks the synchronization state of a single file linked between the local
 * workspace and a remote Vault instance, including its local path, the last
 * known remote MD5 checksum, and the timestamp of the most recent sync.
 */
public class ToolboxFile extends VaultModel {

	private static final String DATE_UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	private String lastSyncDate;
	private String localPath;
	private String remoteMd5;

	public ToolboxFile() {
	}

	/**
	 * Creates a {@code ToolboxFile} initialized with the given local path and
	 * remote MD5. The last sync date is set to the current time as a side effect
	 * of setting the remote MD5.
	 *
	 * @param localPath the local path of the linked file
	 * @param remoteMd5 the MD5 checksum of the remote file
	 */
	public ToolboxFile(String localPath, String remoteMd5) {
		setLocalPath(localPath);
		setRemoteMd5(remoteMd5);
	}

	/**
	 * @return the timestamp of the last successful sync, formatted as ISO-8601 UTC
	 */
	public String getLastSyncDate() {
		return lastSyncDate;
	}

	public void setLastSyncDate(String lastSyncDate) {
		this.lastSyncDate = lastSyncDate;
	}

	/**
	 * @return the local path of the linked file
	 */
	public String getLocalPath() {
		return localPath;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	/**
	 * @return the MD5 checksum recorded for the remote file at the last sync
	 */
	public String getRemoteMd5() {
		return remoteMd5;
	}

	/**
	 * Updates the remote MD5 checksum and stamps the last sync date with the
	 * current UTC time.
	 *
	 * @param remoteMd5 the new MD5 checksum of the remote file
	 */
	public void setRemoteMd5(String remoteMd5) {
		this.remoteMd5 = remoteMd5;
		this.lastSyncDate = ZonedDateTime.now().format(DateTimeFormatter.ofPattern(DATE_UTC_FORMAT));
	}
}
