package com.veeva.vault.toolbox.intellij.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ToolboxSettings {
    private static final String DEFAULT_TOOLBOX_PATH = "/toolbox";
    private static final String DEFAULT_CONFIG_PATH = "/toolbox/config";
    private static final String DEFAULT_LOG_PATH = "/toolbox/logs";
    private static final String DEFAULT_MDL_PATH = "/toolbox/mdl";
    private static final String DEFAULT_VPK_PATH = "/toolbox/vpk";

    private String toolboxPath = DEFAULT_TOOLBOX_PATH;
    private String configPath = DEFAULT_CONFIG_PATH;
    private String logsPath = DEFAULT_LOG_PATH;
    private String mdlPath = DEFAULT_MDL_PATH;
    private String vpkPath = DEFAULT_VPK_PATH;

    public String getToolboxPath() {
        return (toolboxPath != null) ? toolboxPath : DEFAULT_TOOLBOX_PATH;
    }
    public void setToolboxPath(String toolboxPath) {
        this.toolboxPath = (toolboxPath != null) ? toolboxPath : DEFAULT_TOOLBOX_PATH;
    }

    public String getConfigPath() {
        return (configPath != null) ? configPath : DEFAULT_CONFIG_PATH;
    }
    public String getLogsPath() {
        return (logsPath != null) ? logsPath : DEFAULT_LOG_PATH;
    }
    public String getMdlPath() {
        return (mdlPath != null) ? mdlPath : DEFAULT_MDL_PATH;
    }
    public String getVpkPath() {
        return (vpkPath != null) ? vpkPath : DEFAULT_VPK_PATH;
    }

    public void setConfigPath(String configPath) {
        this.configPath = (configPath != null) ? configPath : DEFAULT_CONFIG_PATH;
    }
    public void setLogsPath(String logsPath) {
        this.logsPath = (logsPath != null) ? logsPath : DEFAULT_LOG_PATH;
    }
    public void setMdlPath(String mdlPath) {
        this.mdlPath = (mdlPath != null) ? mdlPath : DEFAULT_MDL_PATH;
    }
    public void setVpkPath(String vpkPath) {
        this.vpkPath = (vpkPath != null) ? vpkPath : DEFAULT_VPK_PATH;
    }

    @JsonIgnore
    public void save(File settingsFile) {
        try	 {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            String json = mapper.writeValueAsString(this);
            FileUtils.writeStringToFile(new File(settingsFile.getAbsolutePath()), json,"UTF-8");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @JsonIgnore
    public static ToolboxSettings load(File settingsFile) {
        try	 {
            if (settingsFile.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                ToolboxSettings toolboxSettings = mapper.readValue(new String(Files.readAllBytes(Paths.get(settingsFile.getPath()))), ToolboxSettings.class);
                return toolboxSettings;
            }
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }
}