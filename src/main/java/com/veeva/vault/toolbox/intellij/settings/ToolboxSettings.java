package com.veeva.vault.toolbox.intellij.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Per-project toolbox configuration persisted as JSON alongside the project.
 * Stores the relative paths used by the plugin for downloads, logs, MDL, and VPK files,
 * each defaulting to a sensible location under the project root if unset.
 */
public class ToolboxSettings {

    private static final Logger logger = LoggerFactory.getLogger(ToolboxSettings.class);

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

    /**
     * @return the configured toolbox root path, or the default if unset
     */
    public String getToolboxPath() {
        return valueOrDefault(toolboxPath, DEFAULT_TOOLBOX_PATH);
    }

    /**
     * Sets the toolbox root path. A {@code null} value resets to the default.
     *
     * @param toolboxPath the new toolbox path, or {@code null} to reset
     */
    public void setToolboxPath(String toolboxPath) {
        this.toolboxPath = valueOrDefault(toolboxPath, DEFAULT_TOOLBOX_PATH);
    }

    /**
     * @return the configured config path, or the default if unset
     */
    public String getConfigPath() {
        return valueOrDefault(configPath, DEFAULT_CONFIG_PATH);
    }

    /**
     * Sets the config path. A {@code null} value resets to the default.
     *
     * @param configPath the new config path, or {@code null} to reset
     */
    public void setConfigPath(String configPath) {
        this.configPath = valueOrDefault(configPath, DEFAULT_CONFIG_PATH);
    }

    /**
     * @return the configured logs path, or the default if unset
     */
    public String getLogsPath() {
        return valueOrDefault(logsPath, DEFAULT_LOG_PATH);
    }

    /**
     * Sets the logs path. A {@code null} value resets to the default.
     *
     * @param logsPath the new logs path, or {@code null} to reset
     */
    public void setLogsPath(String logsPath) {
        this.logsPath = valueOrDefault(logsPath, DEFAULT_LOG_PATH);
    }

    /**
     * @return the configured MDL path, or the default if unset
     */
    public String getMdlPath() {
        return valueOrDefault(mdlPath, DEFAULT_MDL_PATH);
    }

    /**
     * Sets the MDL path. A {@code null} value resets to the default.
     *
     * @param mdlPath the new MDL path, or {@code null} to reset
     */
    public void setMdlPath(String mdlPath) {
        this.mdlPath = valueOrDefault(mdlPath, DEFAULT_MDL_PATH);
    }

    /**
     * @return the configured VPK path, or the default if unset
     */
    public String getVpkPath() {
        return valueOrDefault(vpkPath, DEFAULT_VPK_PATH);
    }

    /**
     * Sets the VPK path. A {@code null} value resets to the default.
     *
     * @param vpkPath the new VPK path, or {@code null} to reset
     */
    public void setVpkPath(String vpkPath) {
        this.vpkPath = valueOrDefault(vpkPath, DEFAULT_VPK_PATH);
    }

    /**
     * Serializes the current settings to the given file as pretty-printed JSON
     * with map entries ordered by key.
     *
     * @param settingsFile the file to write the settings to
     */
    @JsonIgnore
    public void save(File settingsFile) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            String json = mapper.writeValueAsString(this);
            FileUtils.writeStringToFile(settingsFile, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Failed to save toolbox settings to {}", settingsFile, e);
        }
    }

    /**
     * Loads settings from the given JSON file.
     *
     * @param settingsFile the file to read the settings from
     * @return the deserialized settings, or {@code null} if the file does not exist
     *         or could not be parsed
     */
    @JsonIgnore
    public static ToolboxSettings load(File settingsFile) {
        if (!settingsFile.exists()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(settingsFile, ToolboxSettings.class);
        } catch (Exception e) {
            logger.error("Failed to load toolbox settings from {}", settingsFile, e);
            return null;
        }
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
