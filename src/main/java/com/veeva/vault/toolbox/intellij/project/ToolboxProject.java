package com.veeva.vault.toolbox.intellij.project;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.veeva.vault.toolbox.intellij.credentials.BasicAuth;
import com.veeva.vault.toolbox.intellij.credentials.VaultCredentialManager;
import com.veeva.vault.toolbox.intellij.listeners.ConnectionListener;
import com.veeva.vault.toolbox.intellij.settings.*;
import com.veeva.vault.toolbox.intellij.tasks.SaveCredentialsTask;
import com.veeva.vault.toolbox.intellij.ui.LoginDialog;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.common.User;
import com.veeva.vault.vapil.api.model.response.AuthenticationResponse;
import com.veeva.vault.vapil.api.model.response.DomainResponse;
import com.veeva.vault.vapil.api.model.response.UserRetrieveResponse;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.AuthenticationRequest;
import com.veeva.vault.vapil.api.request.DomainRequest;
import com.veeva.vault.vapil.api.request.UserRequest;
import com.veeva.vault.vapil.connector.HttpRequestConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Manages the lifecycle and state of a Vault Toolbox project within IntelliJ.
 * This includes connection state, settings management, and integration with the IDE's UI components.
 */
public class ToolboxProject {

    private static final Logger logger = LoggerFactory.getLogger(ToolboxProject.class);

    public static final String CLIENT_ID = "veeva-vault-toolbox-intellij";
    public static final String SETTING_FILE_NAME = "vault-toolbox.json";
    public static final int MAX_FAILED_ATTEMPTS = 3;
    public static final int SLEEP_WAIT = 500;

    private final Project project;
    private final List<ConnectionListener> connectionListeners = new ArrayList<>();
    private final VaultSettings vaultSettings;
    private final FileSettings fileSettings;
    private final File settingsFile;

    private VaultClient vaultClient;
    private User vaultUser;
    private Integer vaultId;
    private int failedLoginCount = 0;
    private ToolboxSettings toolboxSettings;
    private ToolWindow toolWindow;
    private final AtomicBoolean handlingSessionExpiration = new AtomicBoolean(false);

    /**
     * Constructs a new ToolboxProject instance for the given IntelliJ project.
     *
     * @param project the IntelliJ project
     */
    private ToolboxProject(Project project) {
        this.project = project;
        this.vaultSettings = VaultSettings.getInstance(project);
        this.fileSettings = FileSettings.getInstance(project);
        this.settingsFile = new File(project.getBasePath(), SETTING_FILE_NAME);
        this.toolboxSettings = ToolboxSettings.load(settingsFile);
        init();
    }

    /**
     * Checks if the toolbox is currently enabled for this project.
     *
     * @return true if toolbox settings are loaded, false otherwise.
     */
    public boolean isToolboxEnabled() {
        if (toolboxSettings == null) {
            logger.warn("Toolbox project settings are null");
        }
        return toolboxSettings != null;
    }

    /**
     * Initializes or updates the toolbox project link, ensuring settings are loaded and UI components are shown.
     */
    public void linkProject() {
        logger.debug("Linking toolbox project");
        if (settingsFile.exists()) {
            toolboxSettings = ToolboxSettings.load(settingsFile);
        } else {
            toolboxSettings = new ToolboxSettings();
            toolboxSettings.save(settingsFile);
        }
        this.showToolWindow();
        this.saveAsync();
    }

    /**
     * Unlinks the toolbox from the project, disconnecting active sessions and hiding UI components.
     */
    public void unlinkProject() {
        logger.debug("Unlinking toolbox project");
        this.disconnect();
        this.toolboxSettings = null;
        this.hideToolWindow();
        this.saveAsync();
    }

    /**
     * Checks if there is an active and authenticated session with Vault.
     *
     * @return true if connected with a valid session ID.
     */
    public boolean isConnected() {
        return vaultClient != null && vaultClient.hasSessionId();
    }

    /**
     * Ensures a connection is active before proceeding with a request.
     * If not connected, prompts the user with a login dialog.
     *
     * @return true if connection is established or already active.
     */
    public boolean prepareRequest() {
        if (vaultClient != null && vaultClient.hasSessionId()) {
            return true;
        }
        return connectWithDialog();
    }

    /**
     * Creates a new message instance associated with this project.
     *
     * @return a new Message object.
     */
    public Message newMessage() {
        return new Message(this);
    }

    /**
     * Gets the IntelliJ project associated with this instance.
     *
     * @return the IntelliJ project.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Gets the directory where toolbox-specific files are stored.
     *
     * @return the toolbox directory File, or null if toolbox is disabled.
     */
    public File getToolboxDirectory() {
        if (toolboxSettings != null) {
            return new File(project.getBasePath(), toolboxSettings.getToolboxPath());
        }
        return null;
    }

    /**
     * Gets the directory for storing configuration files.
     *
     * @return the configuration directory File, or null if toolbox is disabled.
     */
    public File getConfigDirectory() {
        if (toolboxSettings != null) {
            return new File(project.getBasePath(), toolboxSettings.getConfigPath());
        }
        return null;
    }

    /**
     * Sets the configuration directory based on a virtual file.
     *
     * @param virtualFile the virtual file representing the configuration directory.
     */
    public void setConfigDirectory(VirtualFile virtualFile) {
        if (toolboxSettings != null) {
            if (virtualFile != null && virtualFile.exists()) {
                String path = getRelativePath(virtualFile);
                toolboxSettings.setConfigPath(path);
            }
        } else {
            toolboxSettings.setConfigPath(null);
        }
    }

    /**
     * Gets the directory for storing log files.
     *
     * @return the logs directory File, or null if toolbox is disabled.
     */
    public File getLogsDirectory() {
        if (toolboxSettings != null) {
            return new File(project.getBasePath(), toolboxSettings.getLogsPath());
        }
        return null;
    }

    /**
     * Sets the logs directory based on a virtual file.
     *
     * @param virtualFile the virtual file representing the logs directory.
     */
    public void setLogsDirectory(VirtualFile virtualFile) {
        if (toolboxSettings != null) {
            if (virtualFile != null && virtualFile.exists()) {
                String path = getRelativePath(virtualFile);
                toolboxSettings.setLogsPath(path);
            }
        } else {
            toolboxSettings.setLogsPath(null);
        }
    }

    /**
     * Gets the directory for storing MDL files.
     *
     * @return the MDL directory File, or null if toolbox is disabled.
     */
    public File getMdlDirectory() {
        if (toolboxSettings != null) {
            return new File(project.getBasePath(), toolboxSettings.getMdlPath());
        }
        return null;
    }

    /**
     * Sets the MDL directory based on a virtual file.
     *
     * @param virtualFile the virtual file representing the MDL directory.
     */
    public void setMdlDirectory(VirtualFile virtualFile) {
        if (toolboxSettings != null) {
            if (virtualFile != null && virtualFile.exists()) {
                String path = getRelativePath(virtualFile);
                toolboxSettings.setMdlPath(path);
            }
        } else {
            toolboxSettings.setMdlPath(null);
        }
    }

    /**
     * Gets the directory for storing VPK files.
     *
     * @return the VPK directory File, or null if toolbox is disabled.
     */
    public File getVpkDirectory() {
        if (toolboxSettings != null) {
            return new File(project.getBasePath(), toolboxSettings.getVpkPath());
        }
        return null;
    }

    /**
     * Sets the VPK directory based on a virtual file.
     *
     * @param virtualFile the virtual file representing the VPK directory.
     */
    public void setVpkDirectory(VirtualFile virtualFile) {
        if (toolboxSettings != null) {
            if (virtualFile != null && virtualFile.exists()) {
                String path = getRelativePath(virtualFile);
                toolboxSettings.setVpkPath(path);
            }
        } else {
            toolboxSettings.setVpkPath(null);
        }
    }

    /**
     * Gets the toolbox settings file.
     *
     * @return the settings file.
     */
    public File getSettingsFile() {
        return settingsFile;
    }

    /**
     * Gets the tool window associated with this project.
     *
     * @return the tool window.
     */
    public ToolWindow getToolWindow() {
        return toolWindow;
    }

    /**
     * Gets the currently active Vault configuration from settings.
     *
     * @return the active Vault, or null if none is set.
     */
    public Vault getActiveVault() {
        if (vaultSettings != null) {
            return vaultSettings.getActiveVault();
        }
        return null;
    }

    /**
     * Determines if the currently connected Vault is a Production Vault (excluding DEV/PVM domains).
     * Used to protect against irreversible actions in Production Vaults (e.g., Delete Jobs, MDL changes).
     *
     * @return true if logged into a Production Vault (excluding DEV/PVM domains), otherwise false.
     */
    public boolean isProductionVault() {
        if (isConnected()) {
            String domainType = getDomainType();
            if (domainType != null && domainType.equalsIgnoreCase("PRODUCTION")) {
                if (!isVaultDevOrPVMDomain(getVaultDNS())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if the given Vault DNS belongs to a development or PVM domain.
     *
     * @param vaultDNS the DNS to check
     * @return true if the Vault DNS contains 'vaultdev.com' or 'vaultpvm.com'.
     */
    private boolean isVaultDevOrPVMDomain(String vaultDNS) {
        if (vaultDNS != null) {
            String lowerCaseDns = vaultDNS.toLowerCase();
            return lowerCaseDns.contains("vaultdev.com") || lowerCaseDns.contains("vaultpvm.com");
        }
        return false;
    }

    /**
     * Determines if the given Vault DNS belongs to a PVM domain.
     *
     * @param vaultDNS the DNS to check
     * @return true if the Vault DNS contains 'vaultpvm.com'.
     */
    private boolean isVaultPVMDomain(String vaultDNS) {
        if (vaultDNS != null) {
            return vaultDNS.toLowerCase().contains("vaultpvm.com");
        }
        return false;
    }

    /**
     * Configures the tool window for this project and sets its initial visibility.
     *
     * @param toolWindow the IntelliJ ToolWindow.
     */
    public void setToolWindow(ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        if (toolWindow != null) {
            if (this.isToolboxEnabled()) {
                this.showToolWindow();
            } else {
                this.hideToolWindow();
            }
        }
    }

    /**
     * Registers a listener to be notified of connection state changes.
     *
     * @param connectionListener the listener to add.
     */
    public void addConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.add(connectionListener);
    }

    /**
     * Removes a previously registered connection listener.
     *
     * @param connectionListener the listener to remove.
     */
    public void removeConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.remove(connectionListener);
    }

    /**
     * Terminates the active session and notifies listeners.
     */
    public void disconnect() {
        try {
            failedLoginCount = 0;
            vaultClient = null;
            vaultUser = null;
            vaultId = null;
            for (ConnectionListener connectionListener : connectionListeners) {
                logger.debug("Invoking disconnect listener " + connectionListener);
                connectionListener.disconnected();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Initializes the project instance, potentially performing an automatic connection if configured.
     */
    public void init() {
        try {
            logger.debug("ToolboxProject.Init " + project.getName());
            AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());

            if (appState.autoConnect) {
                connectSilent();
            }
        } catch (Exception e) {
            logger.error("Toolbox project initialization failed");
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Saves settings asynchronously and refreshes the file system.
     */
    public void saveAsync() {
        save();
    }

    /**
     * Saves the current toolbox settings to disk and triggers a file system refresh.
     */
    public void save() {
        toolboxSettings.save(settingsFile);
        refresh();
    }

    /**
     * Triggers an asynchronous refresh of the virtual file system.
     */
    public void refresh() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Thread.sleep(SLEEP_WAIT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            VirtualFileManager.getInstance().asyncRefresh();
        });
    }

    /**
     * Makes the toolbox tool window available in the IDE.
     */
    public void showToolWindow() {
        if (toolWindow != null) {
            logger.debug("ToolboxProject.showToolWindow " + project.getName());
            toolWindow.setAvailable(true);
        } else {
            logger.debug("No tool window to show " + project.getName());
        }
    }

    /**
     * Hides the toolbox tool window from the IDE.
     */
    public void hideToolWindow() {
        if (toolWindow != null) {
            logger.debug("ToolboxProject.hideToolWindow " + project.getName());
            toolWindow.setAvailable(false);
        } else {
            logger.debug("No tool window to hide " + project.getName());
        }
    }

    /**
     * Gets the active Vault client.
     *
     * @return the vault client.
     */
    public VaultClient getVaultClient() {
        return vaultClient;
    }

    /**
     * Represents the result of a connection attempt.
     */
    public static class ConnectionResult {
        private boolean isConnected = false;
        private String errorMessage = null;

        public ConnectionResult(boolean isConnected) {
            this.isConnected = isConnected;
        }

        public ConnectionResult(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        /**
         * Checks if the connection was successful.
         *
         * @return true if connected.
         */
        public boolean isConnected() {
            return isConnected;
        }

        /**
         * Checks if the connection attempt failed.
         *
         * @return true if there is an error message.
         */
        public boolean isFailure() {
            return errorMessage != null;
        }

        /**
         * Gets the error message if the connection failed.
         *
         * @return the error message, or null if successful.
         */
        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Attempts to connect to the active Vault using stored credentials without showing a dialog.
     */
    public void connectSilent() {
        if (this.isToolboxEnabled()) {
            Vault currentVault = getActiveVault();
            if (currentVault == null) return;

            switch (currentVault.getAuthenticationType()) {
                case BASIC -> {
                    BasicAuth basicAuth = VaultCredentialManager.getUsernamePassword(currentVault.getVaultDNS());
                    if (basicAuth != null) {
                        new Thread(() -> connectWithBasic(
                                currentVault.getVaultDNS(),
                                basicAuth.getUsername(),
                                basicAuth.getPassword(),
                                currentVault.getSaveSecret()
                        )).start();
                    }
                }
                case SESSION_ID -> connectWithSession(
                        currentVault.getVaultDNS(),
                        VaultCredentialManager.getSessionId(currentVault.getVaultDNS()),
                        currentVault.getSaveSecret()
                );
            }
        }
    }

    /**
     * Opens the login dialog to establish a connection.
     *
     * @return true if connection was successful.
     */
    public boolean connectWithDialog() {
        LoginDialog loginDialog = new LoginDialog(this);
        if (!this.isToolboxEnabled()) {
            if (!loginDialog.showAndGet()) {
                vaultClient = null;
            }
        } else {
            loginDialog.show();
        }

        return isConnected();
    }

    /**
     * Connects to Vault using Basic Authentication (username/password).
     *
     * @param vaultDNS     the DNS of the Vault.
     * @param username     the username.
     * @param password     the password.
     * @param savePassword whether to persist credentials.
     * @return a ConnectionResult indicating success or failure.
     */
    public ConnectionResult connectWithBasic(String vaultDNS, String username, String password, boolean savePassword) {
        try {
            if (AppSettings.requireRestart) {
                return new ConnectionResult("IntelliJ requires restart.");
            } else if (failedLoginCount < MAX_FAILED_ATTEMPTS) {

                forceResetVapilClient();

                AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());
                String exactDns = vaultDNS.trim();
                if (isVaultPVMDomain(exactDns) && !appState.allowAllCertificates) {
                    return new ConnectionResult("SSL certificate verification failed.");
                }
                boolean needsCertBypass = appState.allowAllCertificates;

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<VaultClient> future = executor.submit(() -> {
                    var builder = VaultClient.newClientBuilder(VaultClient.AuthenticationType.BASIC)
                            .withVaultClientId(ToolboxProject.CLIENT_ID)
                            .withVaultDNS(exactDns)
                            .withVaultUsername(username)
                            .withVaultPassword(password);

                    if (needsCertBypass) {
                        builder.withAllowAllCertificates(true);
                    }
                    return builder.build();
                });

                VaultClient tempVaultClient;
                int timeoutSeconds = (appState.connectionTimeout > 0) ? appState.connectionTimeout : 15;
                try {
                    tempVaultClient = future.get(timeoutSeconds, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    return new ConnectionResult("Connection timed out after " + timeoutSeconds + " seconds.");
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception) {
                        throw (Exception) cause;
                    }
                    throw e;
                } finally {
                    executor.shutdownNow();
                }

                AuthenticationResponse authResponse = tempVaultClient.getAuthenticationResponse();

                if (tempVaultClient.hasSessionId()) {
                    vaultClient = tempVaultClient;
                    failedLoginCount = 0;

                    String storageDns = exactDns.toLowerCase();
                    vaultSettings.addVault(new Vault(
                            Vault.AuthenticationType.BASIC,
                            storageDns,
                            savePassword, true));

                    ApplicationManager.getApplication().invokeLaterOnWriteThread(() -> {
                        String passToSave = savePassword ? password : null;
                        SaveCredentialsTask task = new SaveCredentialsTask(this.getProject(), storageDns, username, passToSave);
                        task.queue();
                    });

                    invokeConnectionListeners();
                    logger.debug("Connected to Vault");
                    return new ConnectionResult(true);

                } else {
                    failedLoginCount++;
                    if (authResponse != null && authResponse.getErrors() != null && !authResponse.getErrors().isEmpty()) {
                        return new ConnectionResult(authResponse.getErrors().get(0).getMessage());
                    } else {
                        return new ConnectionResult("Authentication failed. Please check your credentials and DNS.");
                    }
                }

            } else {
                return new ConnectionResult("Exceeded Invalid Failed Attempts");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return handleConnectionException(e);
        }
    }

    /**
     * Maps connection-related exceptions to user-friendly error messages.
     * Handles specific cases like unknown hosts, SSL issues, and VAPIL-specific failures.
     *
     * @param e the exception that occurred during connection
     * @return a ConnectionResult containing the formatted error message
     */
    private ConnectionResult handleConnectionException(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof java.net.UnknownHostException) {
                return new ConnectionResult("Could not resolve host. Please check your Vault DNS for typos.");
            }
            if (cause instanceof javax.net.ssl.SSLHandshakeException) {
                return new ConnectionResult("SSL Verification Failed. Try enabling 'Allow All Certificates' in Settings.");
            }
            cause = cause.getCause();
        }

        String msg = e.getMessage();
        if (msg != null) {
            if (msg.contains("UnknownHostException") || msg.contains("Unable to resolve host")) {
                return new ConnectionResult("Could not resolve host. Please check your Vault DNS for typos.");
            }
            if (msg.contains("SSLHandshakeException") || msg.contains("PKIX path building failed")) {
                return new ConnectionResult("SSL Verification Failed. Try enabling 'Allow All Certificates' in Settings.");
            }
            if (e instanceof NullPointerException && msg.contains("HttpResponseConnector.getResponse()")) {
                return new ConnectionResult("Could not connect to server. Please check your Vault DNS for typos.");
            }
            return new ConnectionResult(msg);
        }

        if (e instanceof NullPointerException) {
            return new ConnectionResult("Network connection failed. Please check your Vault DNS for typos.");
        }

        return new ConnectionResult("An unexpected error occurred.");
    }

    /**
     * Connects to Vault using an existing Session ID.
     *
     * @param vaultDNS      the DNS of the Vault.
     * @param sessionid     the session ID.
     * @param saveSessionId whether to persist the session ID.
     * @return a ConnectionResult indicating success or failure.
     */
    public ConnectionResult connectWithSession(String vaultDNS, String sessionid, boolean saveSessionId) {
        try {
            if (AppSettings.requireRestart) {
                return new ConnectionResult("IntelliJ requires restart.");
            } else if (failedLoginCount < MAX_FAILED_ATTEMPTS) {

                forceResetVapilClient();

                AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());
                if (isVaultPVMDomain(vaultDNS) && !appState.allowAllCertificates) {
                    return new ConnectionResult("SSL certificate verification failed.");
                }
                boolean needsCertBypass = appState.allowAllCertificates;

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<VaultClient> future = executor.submit(() -> {
                    var builder = VaultClient.newClientBuilder(VaultClient.AuthenticationType.SESSION_ID)
                            .withVaultClientId(ToolboxProject.CLIENT_ID)
                            .withVaultDNS(vaultDNS)
                            .withVaultSessionId(sessionid);

                    if (needsCertBypass) {
                        builder.withAllowAllCertificates(true);
                    }
                    return builder.build();
                });

                VaultClient tempVaultClient;
                try {
                    tempVaultClient = future.get(15, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    return new ConnectionResult("Connection timed out (15s).");
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception) {
                        throw (Exception) cause;
                    }
                    throw e;
                } finally {
                    executor.shutdownNow();
                }

                if (tempVaultClient != null && tempVaultClient.hasSessionId()) {
                    vaultClient = tempVaultClient;
                    failedLoginCount = 0;

                    vaultSettings.addVault(new Vault(
                            Vault.AuthenticationType.SESSION_ID,
                            vaultDNS,
                            saveSessionId, true));

                    String sessionToSave = saveSessionId ? sessionid : null;
                    SaveCredentialsTask task = new SaveCredentialsTask(this.getProject(), vaultDNS, sessionToSave);
                    task.queue();

                    invokeConnectionListeners();
                    logger.debug("Connected to Vault");
                    return new ConnectionResult(true);
                } else {
                    failedLoginCount++;
                    if (tempVaultClient != null) {
                        AuthenticationResponse authenticationResponse = tempVaultClient.getAuthenticationResponse();
                        if (authenticationResponse != null && authenticationResponse.getErrors() != null) {
                            return new ConnectionResult(authenticationResponse.getErrors().get(0).getMessage());
                        }
                    }
                }

            } else {
                return new ConnectionResult("Exceeded Invalid Failed Attempts");
            }
            return new ConnectionResult("Unknown Error");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ConnectionResult("Unknown Error");
        }
    }

    /**
     * Invokes all registered connection listeners to notify them of a successful connection.
     */
    private void invokeConnectionListeners() {
        try {
            if (!connectionListeners.isEmpty()) {
                for (ConnectionListener connectionListener : connectionListeners) {
                    logger.debug("Invoking connect listener " + connectionListener);
                    connectionListener.connected();
                }
            } else {
                logger.debug("No connection listener found");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Intercepts a Vault API response to check for session expiration.
     * If expired, prompts the user to log in again.
     *
     * @param response the VaultResponse to check.
     * @return true if the session was expired and handled.
     */
    public boolean handleSessionExpiration(VaultResponse response) {
        if (response != null && response.isFailure() && response.getErrors() != null) {
            boolean isExpired = response.getErrors().stream()
                    .anyMatch(error -> "INVALID_SESSION_ID".equalsIgnoreCase(error.getType()));

            if (isExpired) {
                handleExpirationUI();
                return true;
            }
        }
        return false;
    }

    /**
     * Intercepts an exception to check for session expiration.
     *
     * @param e the Exception to check.
     * @return true if the session was expired and handled.
     */
    public boolean handleSessionExpiration(Exception e) {
        if (e != null && e.getMessage() != null && e.getMessage().contains("INVALID_SESSION_ID")) {
            handleExpirationUI();
            return true;
        }
        return false;
    }

    /**
     * Displays a session expiration warning and prompts the user to re-authenticate.
     * Uses an atomic boolean to ensure only one expiration dialog is shown at a time.
     */
    private void handleExpirationUI() {
        if (!handlingSessionExpiration.compareAndSet(false, true)) {
            return;
        }
        disconnect();
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                com.intellij.openapi.ui.Messages.showWarningDialog(project,
                        "Your Veeva Vault session has expired. Please log in again to continue.",
                        "Session Expired");

                connectWithDialog();

                if (isConnected()) {
                    invokeConnectionListeners();
                }
            } finally {
                handlingSessionExpiration.set(false);
            }
        }, com.intellij.openapi.application.ModalityState.any());
    }

    /**
     * Gets the DNS of the currently connected Vault.
     *
     * @return the Vault DNS, or null if not connected.
     */
    public String getVaultDNS() {
        try {
            return vaultClient != null ? vaultClient.getVaultDNS() : null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieves the current Vault user, validating the session if necessary.
     *
     * @return the current User, or null if not connected.
     */
    public User getVaultUser() {
        if (vaultUser == null && isConnected()) {
            UserRetrieveResponse response = getVaultClient()
                    .newRequest(UserRequest.class).validateSessionUser();
            if (response != null && !response.isFailure()) {
                vaultUser = response.getUsers().get(0).getUser();
                vaultId = response.getHeaderVaultId();
            }
        }
        return vaultUser;
    }

    /**
     * Retrieves the ID of the currently connected Vault.
     *
     * @return the Vault ID, or null if not connected.
     */
    public Integer getVaultId() {
        if (vaultId == null && isConnected()) {
            VaultResponse response = getVaultClient()
                    .newRequest(AuthenticationRequest.class)
                    .sessionKeepAlive();
            if (response != null && !response.isFailure()) {
                vaultId = response.getHeaderVaultId();
            }
        }
        return vaultId;
    }

    /**
     * Retrieves the name of the currently connected Vault from domain information.
     *
     * @return the Vault name, or "Unknown" if it cannot be retrieved.
     */
    public String getVaultName() {
        if (isConnected()) {
            try {
                DomainResponse response = getVaultClient()
                        .newRequest(DomainRequest.class)
                        .retrieveDomainInformation();

                if (response != null && !response.isFailure() && response.getDomain() != null && response.getDomain().getVaults() != null) {
                    String currentVaultId = String.valueOf(getVaultId());
                    for (DomainResponse.Domain.DomainVault vault : response.getDomain().getVaults()) {
                        if (currentVaultId.equals(String.valueOf(vault.getId()))) {
                            String name = vault.getVaultName();
                            if (name != null && !name.isEmpty()) {
                                return name;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error retrieving Vault Name: {}", e.getMessage());
            }
        }
        return "Unknown";
    }

    /**
     * Retrieves the domain type of the currently connected Vault.
     *
     * @return the domain type (e.g., "PRODUCTION", "SANDBOX"), or "Unknown" if it cannot be retrieved.
     */
    public String getDomainType() {
        if (isConnected()) {
            try {
                DomainResponse response = getVaultClient().newRequest(DomainRequest.class)
                        .retrieveDomainInformation();

                if (response != null && !response.isFailure() && response.getDomain() != null) {
                    return response.getDomain().getDomainType();
                }
            } catch (Exception e) {
                logger.error("Error retrieving Domain Type: " + e.getMessage());
            }
        }
        return "Unknown";
    }

    /**
     * Retrieves the family of the currently connected Vault.
     *
     * @return the Vault family label, or "Unknown" if it cannot be retrieved.
     */
    public String getVaultFamily() {
        if (isConnected()) {
            try {
                DomainResponse response = getVaultClient()
                        .newRequest(DomainRequest.class)
                        .retrieveDomainInformation();

                if (response != null && !response.isFailure() && response.getDomain() != null && response.getDomain().getVaults() != null) {
                    String currentVaultId = String.valueOf(getVaultId());
                    for (DomainResponse.Domain.DomainVault vault : response.getDomain().getVaults()) {
                        if (currentVaultId.equals(String.valueOf(vault.getId()))) {
                            return vault.getVaultFamily().getLabel();
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error retrieving Vault Family: " + e.getMessage());
            }
        }
        return "Unknown";
    }

    /**
     * Retrieves the applications associated with the currently connected Vault.
     *
     * @return a comma-separated list of application labels, or "Unknown" if they cannot be retrieved.
     */
    public String getVaultApplication() {
        if (isConnected()) {
            try {
                DomainResponse response = getVaultClient()
                        .newRequest(DomainRequest.class)
                        .setIncludeApplications(true)
                        .retrieveDomainInformation();

                if (response != null && !response.isFailure() && response.getDomain() != null && response.getDomain().getVaults() != null) {
                    String currentVaultId = String.valueOf(getVaultId());
                    for (DomainResponse.Domain.DomainVault vault : response.getDomain().getVaults()) {
                        if (currentVaultId.equals(String.valueOf(vault.getId()))) {
                            List<DomainResponse.Domain.DomainVault.VaultApplication> applications = vault.getVaultApplication();
                            if (applications != null && !applications.isEmpty()) {
                                return applications.stream()
                                        .map(app -> {
                                            String label = app.getString("label");
                                            if (label == null || label.isEmpty()) {
                                                label = app.getString("name");
                                            }
                                            return label;
                                        })
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.joining(", "));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error retrieving Vault Application: " + e.getMessage());
            }
        }
        return "Unknown";
    }

    /**
     * Forcibly resets the VAPIL OkHttpClient singleton via reflection.
     * This is used to allow SSL context changes mid-session without restarting the IDE.
     */
    private void forceResetVapilClient() {
        try {
            Field clientField = HttpRequestConnector.class.getDeclaredField("clientInstance");
            clientField.setAccessible(true);
            clientField.set(null, null);
            logger.debug("Successfully reset VAPIL OkHttpClient (clientInstance).");
        } catch (Exception e) {
            logger.warn("Reflection reset failed (VAPIL structure may have changed): " + e.getMessage());
        }
    }

    /**
     * Converts an absolute path to a project-relative path.
     *
     * @param fullPath the absolute file path.
     * @return the relative path.
     */
    public String getRelativePath(String fullPath) {
        String basePath = project.getBasePath();
        if (fullPath != null && basePath != null && fullPath.startsWith(basePath)) {
            return fullPath.substring(basePath.length());
        }
        return fullPath;
    }

    /**
     * Converts a VirtualFile path to a project-relative path.
     *
     * @param virtualFile the virtual file.
     * @return the relative path.
     */
    public String getRelativePath(VirtualFile virtualFile) {
        return getRelativePath(virtualFile.getPath());
    }

    /**
     * Includes a file in the project's linked files.
     *
     * @param localPath the local path of the file.
     */
    public void includeFile(String localPath) {
        includeFile(localPath, null);
    }

    /**
     * Includes a file in the project's linked files with a remote MD5 checksum.
     *
     * @param localPath  the local path of the file.
     * @param remoteMd5  the remote MD5 checksum.
     */
    public void includeFile(String localPath, String remoteMd5) {
        if (fileSettings != null) {
            String relativePath = getRelativePath(localPath);
            fileSettings.addLinkedFile(relativePath, remoteMd5);
        }
    }

    /**
     * Checks if a file is linked to the project.
     *
     * @param localPath the local path of the file.
     * @return true if the file is linked.
     */
    public boolean isLinkedFile(String localPath) {
        if (fileSettings != null) {
            String relativePath = getRelativePath(localPath);
            return fileSettings.getLinkedFile(relativePath) != null;
        }
        return false;
    }

    /**
     * Gets the linked file information for a local path.
     *
     * @param localPath the local path of the file.
     * @return the linked file information, or null if not linked.
     */
    public ToolboxFile getFile(String localPath) {
        if (fileSettings != null) {
            String relativePath = getRelativePath(localPath);
            return fileSettings.getLinkedFile(relativePath);
        }
        return null;
    }

    /**
     * Removes a file from the project's linked files.
     *
     * @param localPath the local path of the file.
     */
    public void removeFile(String localPath) {
        if (fileSettings != null) {
            String relativePath = getRelativePath(localPath);
            fileSettings.removeLinkedFile(relativePath);
        }
    }

    private static final Map<Project, ToolboxProject> vaultProjects = new HashMap<>();

    /**
     * Initializes and returns a ToolboxProject instance for the given IntelliJ project.
     *
     * @param project the IntelliJ project.
     * @return the project instance.
     */
    public static ToolboxProject initInstance(Project project) {
        try {
            if (!vaultProjects.containsKey(project)) {
                if (LocalDate.now().isBefore(LocalDate.parse("2027-01-01"))) {
                    logger.debug("Initializing Toolbox Project " + project.getName());
                    vaultProjects.put(project, new ToolboxProject(project));
                }
            }
            return vaultProjects.get(project);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Gets the ToolboxProject instance for the given project, initializing it if necessary.
     *
     * @param project the IntelliJ project.
     * @return the project instance.
     */
    public static ToolboxProject getInstance(Project project) {
        try {
            ToolboxProject vaultProject = vaultProjects.get(project);
            if (vaultProject == null) {
                return initInstance(project);
            }
            return vaultProject;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Closes the project instance and removes it from the cache.
     *
     * @param project the IntelliJ project.
     */
    public static void closeInstance(Project project) {
        try {
            ToolboxProject toolboxProject = vaultProjects.remove(project);
            if (toolboxProject != null) {
                logger.debug("Closing Toolbox Project " + project.getName());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Checks if a virtual file corresponds to the root of the project.
     *
     * @param file    the file to check.
     * @param project the project.
     * @return true if the file is the project root.
     */
    public static boolean isProjectFile(VirtualFile file, Project project) {
        logger.debug("Checking if project file " + file.getPath() + " exists in project " + project.getName());
        return Objects.equals(project.getBasePath(), file.getPath());
    }
}
