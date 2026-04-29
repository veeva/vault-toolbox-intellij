package com.veeva.vault.toolbox.core.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SdkRuntimeSession {
    @JsonProperty("log_date")
    private String logDate;
    private String md5checksum;
    private String fileName;

    public String getLogDate() { return logDate; }
    public void setLogDate(String logDate) { this.logDate = logDate; }
    public String getMd5checksum() { return md5checksum; }
    public void setMd5checksum(String md5checksum) { this.md5checksum = md5checksum; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
