package com.veeva.vault.toolbox.intellij.settings;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.veeva.vault.vapil.api.model.VaultModel;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ToolboxFile extends VaultModel {
	private static final String DATE_UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

	public ToolboxFile() {
	}

	public ToolboxFile(String localPath, String remoteMd5) {
		setLocalPath(localPath);
		setRemoteMd5(remoteMd5);
	}

	String lastSyncDate;
	String localPath;
	String remoteMd5;

	public String getLastSyncDate() {
		return lastSyncDate;
	}

	public void setLastSyncDate(String lastSyncDate) {
		this.lastSyncDate = lastSyncDate;
	}

	public String getLocalPath() {
		return localPath;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public String getRemoteMd5() {
		return remoteMd5;
	}
	public void setRemoteMd5(String remoteMd5) {
		this.remoteMd5 = remoteMd5;
		this.lastSyncDate = ZonedDateTime.now().format(DateTimeFormatter.ofPattern(DATE_UTC_FORMAT));
	}
}
