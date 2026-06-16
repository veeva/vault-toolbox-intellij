package com.veeva.vault.toolbox.intellij;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Application-level service that handles plugin cleanup on unload.
 *
 * Deregisters any JDBC drivers loaded by the plugin classloader — the SQLite
 * driver auto-registers into the JVM's bootstrap {@link DriverManager} on first
 * use, which would otherwise keep a reference from the bootstrap classloader
 * into the plugin classloader and prevent GC. Also disposes all active
 * {@link ToolboxProject} instances so the static map is drained before the
 * plugin classloader is collected.
 */
public final class ToolboxPluginService implements Disposable {

    private static final Logger logger = LoggerFactory.getLogger(ToolboxPluginService.class);

    /**
     * Retrieves the singleton instance of the {@link ToolboxPluginService}.
     *
     * @return the active plugin service instance
     */
    public static ToolboxPluginService getInstance() {
        return ApplicationManager.getApplication().getService(ToolboxPluginService.class);
    }

    /**
     * Performs cleanup operations when the plugin is disposed or unloaded.
     * Disposes active projects and deregisters JDBC drivers loaded by the plugin.
     */
    @Override
    public void dispose() {
        ToolboxProject.disposeAllInstances();
        deregisterJdbcDrivers();
    }

    /**
     * Deregisters any JDBC drivers that were loaded by this plugin's class loader
     * to prevent class loader leaks and memory leaks.
     */
    private void deregisterJdbcDrivers() {
        ClassLoader pluginClassLoader = getClass().getClassLoader();
        List<Driver> toRemove = new ArrayList<>();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == pluginClassLoader) {
                toRemove.add(driver);
            }
        }
        for (Driver driver : toRemove) {
            try {
                DriverManager.deregisterDriver(driver);
                logger.debug("Deregistered JDBC driver: {}", driver.getClass().getName());
            } catch (Exception e) {
                logger.warn("Failed to deregister JDBC driver: {}", driver.getClass().getName(), e);
            }
        }
    }
}
