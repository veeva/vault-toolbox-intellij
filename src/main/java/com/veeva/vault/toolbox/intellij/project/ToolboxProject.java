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

public class ToolboxProject {

    private static final Logger logger = LoggerFactory.getLogger(ToolboxProject.class);


    public static final String CLIENT_ID = "veeva-vault-toolbox-intellij";
    public static final String SETTING_FILE_NAME = "vault-toolbox.json";
    public static final int MAX_FAILED_ATTEMPTS = 3;
    public static final int SLEEP_WAIT = 500;

    private final Project project;

    private VaultClient vaultClient;
    private User vaultUser;
    private Integer vaultId;

    private int failedLoginCount = 0;

    //listeners
    private final List<ConnectionListener> connectionListeners = new ArrayList<ConnectionListener>();

    //settings
    private final VaultSettings vaultSettings;
    //private final FileSettings fileSettings;
    private ToolboxSettings toolboxSettings;
    private final File settingsFile;
    ToolWindow toolWindow;

    private ToolboxProject(Project project) {
        this.project = project;
        this.vaultSettings = VaultSettings.getInstance(project);
        //this.fileSettings = FileSettings.getInstance(project);
        this.settingsFile = new File(project.getBasePath(), SETTING_FILE_NAME);
        this.toolboxSettings = ToolboxSettings.load(settingsFile);
        init();
    }

    public boolean isToolboxEnabled() {
        if (toolboxSettings == null) {
            logger.warn("Toolbox project settings is null");
        }
        return toolboxSettings != null;
    }

    public void linkProject() {
        logger.debug("Linking toolbox project");
        if (settingsFile.exists()) {
            toolboxSettings = ToolboxSettings.load(settingsFile);
        }
        else {
            toolboxSettings = new ToolboxSettings();
            toolboxSettings.save(settingsFile);
        }
        this.showToolWindow();
        this.saveAsync();
    }

    public void unlinkProject() {
        logger.debug("Unlinking toolbox project");
        this.disconnect();
        this.toolboxSettings = null;
        this.hideToolWindow();
        this.saveAsync();
    }

    public boolean isConnected() {
		return vaultClient != null && vaultClient.hasSessionId();
	}

    public boolean prepareRequest() {
        if (vaultClient != null && vaultClient.hasSessionId()) {
            return true;
        }
        return connectWithDialog();
    }

    public Message newMessage() {
        return new Message(this);
    }

    public Project getProject() {
        return project;
    }

    public File getToolboxDirectory() {
        if (toolboxSettings != null) {
            return new File(project.getBasePath(), toolboxSettings.getToolboxPath());
        }
        else {
            return null;
        }
    }

    public File getConfigDirectory() {
        if (toolboxSettings != null) {
			return new File (project.getBasePath(), toolboxSettings.getConfigPath());
        }
        else {
            return null;
        }
    }

    public void setConfigDirectory(VirtualFile virtualFile) {
        if (toolboxSettings != null) {
            if (virtualFile != null && virtualFile.exists()) {
                String path = getRelativePath(virtualFile);
                toolboxSettings.setConfigPath(path);
            }
        }
        else {
            toolboxSettings.setConfigPath(null);
        }
    }

    public File getLogsDirectory() {
        if (toolboxSettings != null) {
			return new File (project.getBasePath(), toolboxSettings.getLogsPath());
        }
        else {
            return null;
        }
    }

    public void setLogsDirectory(VirtualFile virtualFile) {
        if (toolboxSettings != null) {
            if (virtualFile != null && virtualFile.exists()) {
                String path = getRelativePath(virtualFile);
                toolboxSettings.setLogsPath(path);
            }
        }
        else {
            toolboxSettings.setLogsPath(null);
        }
    }

    public File getMdlDirectory() {
        if (toolboxSettings != null) {
            File file = new File (project.getBasePath(), toolboxSettings.getMdlPath());
            return file;
        }
        else {
            return null;
        }
    }

    public void setMdlDirectory(VirtualFile virtualFile) {
        if (toolboxSettings != null) {
            if (virtualFile != null && virtualFile.exists()) {
                String path = getRelativePath(virtualFile);
                toolboxSettings.setMdlPath(path);
            }
        }
        else {
            toolboxSettings.setMdlPath(null);
        }
    }

    public File getVpkDirectory() {
        if (toolboxSettings != null) {
			return new File (project.getBasePath(), toolboxSettings.getVpkPath());
        }
        else {
            return null;
        }
    }

    public void setVpkDirectory(VirtualFile virtualFile) {
        if (toolboxSettings != null) {
            if (virtualFile != null && virtualFile.exists()) {
                String path = getRelativePath(virtualFile);
                toolboxSettings.setVpkPath(path);
            }
        }
        else {
            toolboxSettings.setVpkPath(null);
        }
    }

    public File getSettingsFile() {
        return settingsFile;
    }

    public ToolWindow getToolWindow() {
        return toolWindow;
    }

    public Vault getActiveVault() {
       if (vaultSettings != null) {
           return vaultSettings.getActiveVault();
       }
       return null;
    }

    public void setToolWindow(ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        if (toolWindow != null) {
            if (this.isToolboxEnabled()) {
                this.showToolWindow();
            }
            else {
                this.hideToolWindow();
            }
        }
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------
    //listeners
    //----------------------------------------------------------------------------------------------------
    public void addConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.add(connectionListener);
    }

    public void removeConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.remove(connectionListener);
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------
    //OEPRATIONS
    //----------------------------------------------------------------------------------------------------

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
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void init() {
        try {
            logger.debug("ToolboxProject.Init " + project.getName());
            AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());

            if (appState.autoConnect) {
                connectSilent();
            }
        }
        catch (Exception e) {
            logger.error("Toolbox project initialization failed");
            logger.error(e.getMessage(), e);
        }
    }

    public void saveAsync() {
        save();
        //SaveSettingsTask task = new SaveSettingsTask(this.getProject());
        //task.queue();
    }

    public void save() {
        toolboxSettings.save(settingsFile);
        refresh();
    }

    public void refresh() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                Thread.sleep(SLEEP_WAIT);
            }
            catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            //project.getProjectFile().getFileSystem().refresh(false);
            VirtualFileManager.getInstance().asyncRefresh();
        });
    }

    public void showToolWindow() {
        if (toolWindow != null) {
            logger.debug("ToolboxProject.showToolWindow " + project.getName());
            toolWindow.setAvailable(true);
        } else {
            logger.debug("No tool window to show " + project.getName());
        }
    }

    public void hideToolWindow() {
        if (toolWindow != null) {
            logger.debug("ToolboxProject.hideToolWindow " + project.getName());
            toolWindow.setAvailable(false);
        }
        else {
            logger.debug("No tool window to hide " + project.getName());
        }
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------
    //VAULT CLIENT
    //----------------------------------------------------------------------------------------------------

    public VaultClient getVaultClient() {
        return vaultClient;
    }

    public static class ConnectionResult {
        private boolean isConnected = false;
        private String errorMessage = null;

        public ConnectionResult(boolean isConnected) {
            this.isConnected = isConnected;
        }

        public ConnectionResult(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public boolean isConnected() {
            return isConnected;
        }

        public boolean isFailure() {
            return errorMessage != null;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public void connectSilent() {
        if (this.isToolboxEnabled()) {
            Vault currentVault = getActiveVault();
            switch (currentVault.getAuthenticationType()) {
                case BASIC -> {
                    BasicAuth basicAuth = VaultCredentialManager.getUsernamePassword(currentVault.getVaultDNS());

					new Thread(() -> {
						connectWithBasic(
								currentVault.getVaultDNS(),
								basicAuth.getUsername(),
								basicAuth.getPassword(),
								currentVault.getSaveSecret()

						);
					}).start();
                }
                case SESSION_ID -> {


                    connectWithSession(
                            currentVault.getVaultDNS(),
                            VaultCredentialManager.getSessionId(currentVault.getVaultDNS()),
                            currentVault.getSaveSecret()

                    );
                }
            }
            if (!isConnected()) {
                //connectWithDialog();
            }
        }
    }

    public boolean connectWithDialog() {
        if (!this.isToolboxEnabled()) {
            LoginDialog loginDialog = new LoginDialog(this);
            if (!loginDialog.showAndGet()) {
                vaultClient = null;
            }
        }
        else {
            LoginDialog loginDialog = new LoginDialog(this);
            loginDialog.show();
        }

        return vaultClient != null && vaultClient.hasSessionId();
    }

    public ConnectionResult connectWithBasic(String vaultDNS, String username, String password, boolean savePassword) {
        try {
            if (AppSettings.requireRestart) {
                return new ConnectionResult("IntelliJ requires restart.");
            } else if (failedLoginCount < MAX_FAILED_ATTEMPTS) {

                forceResetVapilClient();

                AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());

                // Pass exact casing to VAPIL to avoid artificial mismatches, just trim spaces
                String exactDns = vaultDNS.trim();
                boolean needsCertBypass = appState.allowAllCertificates || exactDns.toLowerCase().contains("vaultpvm.com");

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
                    return new ConnectionResult("Connection timed out after " + timeoutSeconds + " seconds. The Vault may be sleeping, inactive, or inaccessible.");
                } catch (java.util.concurrent.ExecutionException e) {
                    // Unwrap the exception so your specific validations below catch the true error
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception) {
                        throw (Exception) cause;
                    }
                    throw e;
                } finally {
                    executor.shutdownNow();
                }

                AuthenticationResponse authResponse = tempVaultClient.getAuthenticationResponse();

                Message loginMessage = newMessage();
                loginMessage.setTitle("Vault Login");

                if (tempVaultClient != null && tempVaultClient.hasSessionId()) {
                    vaultClient = tempVaultClient;
                    failedLoginCount = 0;

                    // Normalize purely for IntelliJ's internal credential storage to prevent duplicates
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

                    invokeConnectionListenrs();
                    logger.debug("Connected to Vault");
                    return new ConnectionResult(true);

                } else {
                    failedLoginCount++;
                    if (authResponse != null && authResponse.getErrors() != null && !authResponse.getErrors().isEmpty()) {
                        String vapilError = authResponse.getErrors().get(0).getMessage();
                        return new ConnectionResult(vapilError);
                    } else if (authResponse != null && authResponse.isFailure()) {
                        // This catches VAPIL's strict DNS verification failure (like a case mismatch)
                        return new ConnectionResult("Authentication failed. If your credentials are correct, please ensure your Vault DNS casing exactly matches the server.");
                    } else {
                        return new ConnectionResult("Authentication failed. Please check your credentials and DNS.");
                    }
                }

            } else {
                return new ConnectionResult("Exceeded Invalid Failed Attempts");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            // Dig into the error chain to see if a network issue caused the crash
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

            // Fallback: check the error message string itself
            String msg = e.getMessage();
            if (msg != null) {
                if (msg.contains("UnknownHostException") || msg.contains("Unable to resolve host")) {
                    return new ConnectionResult("Could not resolve host. Please check your Vault DNS for typos.");
                }
                if (msg.contains("SSLHandshakeException") || msg.contains("PKIX path building failed")) {
                    return new ConnectionResult("SSL Verification Failed. Try enabling 'Allow All Certificates' in Settings.");
                }
                // Catch VAPIL's internal crash when a bad DNS completely fails the network request
                if (e instanceof NullPointerException && msg.contains("HttpResponseConnector.getResponse()")) {
                    return new ConnectionResult("Could not connect to server. Please check your Vault DNS for typos.");
                }
                return new ConnectionResult(msg);
            }

            // If the NPE has no message (older Java versions), safely assume it's the same DNS network drop
            if (e instanceof NullPointerException) {
                return new ConnectionResult("Network connection failed. Please check your Vault DNS for typos.");
            }

            return new ConnectionResult("An unexpected error occurred.");
        }
    }

    public ConnectionResult connectWithSession(String vaultDNS, String sessionid, boolean saveSessionId) {
        try {
            if (AppSettings.requireRestart) {
                return new ConnectionResult("IntelliJ requires restart.");
            } else if (failedLoginCount < MAX_FAILED_ATTEMPTS) {

                forceResetVapilClient();

                AppSettings.AppState appState = Objects.requireNonNull(AppSettings.getInstance().getState());
                boolean needsCertBypass = appState.allowAllCertificates || vaultDNS.toLowerCase().contains("vaultpvm.com");

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
                    return new ConnectionResult("Connection timed out (15s). The Vault may be sleeping, inactive, or inaccessible.");
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

                    invokeConnectionListenrs();
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
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ConnectionResult("Unknown Error");
        }
    }

    private void invokeConnectionListenrs() {
        try {
            if (connectionListeners.size() > 0) {
                for (ConnectionListener connectionListener : connectionListeners) {
                    logger.debug("Invoking connect listener " + connectionListener);
                    connectionListener.connected();
                }
            }
            else {
                logger.debug("No connection listener found");
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------
    // INTERCEPTOR 1: For standard VAPIL API responses (JSON)
    // ---------------------------------------------------------
    public boolean handleSessionExpiration(VaultResponse response) {
        if (response != null && response.isFailure() && response.getErrors() != null) {
            boolean isExpired = response.getErrors().stream()
                    .anyMatch(error -> "INVALID_SESSION_ID".equalsIgnoreCase(error.getType()));

            if (isExpired) {
                disconnect();
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                    com.intellij.openapi.ui.Messages.showWarningDialog(project,
                            "Your Veeva Vault session has expired. Please log in again to continue.",
                            "Session Expired");

                    connectWithDialog();

                    if (isConnected()) {
                        invokeConnectionListenrs();
                    }

                }, com.intellij.openapi.application.ModalityState.any());

                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------
    // INTERCEPTOR 2: For VAPIL File Downloads (Exceptions)
    // ---------------------------------------------------------
    public boolean handleSessionExpiration(Exception e) {
        if (e != null && e.getMessage() != null && e.getMessage().contains("INVALID_SESSION_ID")) {
            disconnect();
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                com.intellij.openapi.ui.Messages.showWarningDialog(project,
                        "Your Veeva Vault session has expired. Please log in again to continue.",
                        "Session Expired");

                connectWithDialog();

                if (isConnected()) {
                    invokeConnectionListenrs();
                }

            }, com.intellij.openapi.application.ModalityState.any());

            return true;
        }
        return false;
    }

    public String getVaultDNS() {
        try {
            return vaultClient.getVaultDNS();
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public User getVaultUser() {
        if (vaultUser == null && isConnected()) {
            UserRetrieveResponse response = getVaultClient()
                    .newRequest(UserRequest.class).validateSessionUser();
            if (response != null  & !response.isFailure()) {
                vaultUser = response.getUsers().get(0).getUser();
                vaultId = response.getHeaderVaultId();
            }
        }
        return vaultUser;
    }

    public Integer getVaultId() {
        if (vaultId == null && isConnected()) {
            VaultResponse response = getVaultClient()
                    .newRequest(AuthenticationRequest.class)
                    .sessionKeepAlive();
            if (response != null  & !response.isFailure()) {
                vaultId = response.getHeaderVaultId();
            }
        }
        return vaultId;
    }

    public String getVaultName() {
        if (isConnected()) {
            try {
                DomainResponse response = getVaultClient()
                        .newRequest(com.veeva.vault.vapil.api.request.DomainRequest.class)
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

    public String getVaultFamily() {
        if (isConnected()) {
            try {
                DomainResponse response = getVaultClient()
                        .newRequest(com.veeva.vault.vapil.api.request.DomainRequest.class)
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

    public String getVaultApplication() {
        if (isConnected()) {
            try {
                com.veeva.vault.vapil.api.model.response.DomainResponse response = getVaultClient()
                        .newRequest(com.veeva.vault.vapil.api.request.DomainRequest.class)
                        .setIncludeApplications(true)
                        .retrieveDomainInformation();

                if (response != null && !response.isFailure() && response.getDomain() != null && response.getDomain().getVaults() != null) {

                    String currentVaultId = String.valueOf(getVaultId());

                    for (DomainResponse.Domain.DomainVault vault : response.getDomain().getVaults()) {

                        if (currentVaultId.equals(String.valueOf(vault.getId()))) {

                            java.util.List<DomainResponse.Domain.DomainVault.VaultApplication> applications = vault.getVaultApplication();

                            if (applications != null && !applications.isEmpty()) {
                                return applications.stream()
                                        .map(app -> {
                                            // BYPASS VAPIL DEFECT: Current it's name instead of name__v so cannot use getName()
                                            String label = app.getString("label");

                                            // Fallback to name just in case the label is missing
                                            if (label == null || label.isEmpty()) {
                                                label = app.getString("name");
                                            }
                                            return label;
                                        })
                                        .filter(java.util.Objects::nonNull)
                                        .collect(java.util.stream.Collectors.joining(", "));
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
     * Forcibly resets the VAPIL OkHttpClient singleton via reflection
     * VAPIL locks its HTTP client upon the first connection, preventing SSL context changes
     * mid-session. Clearing this forces a rebuild, allowing the plugin to seamlessly switch
     * between strict (Dev) and permissive (PVM) SSL environments without restarting the IDE.
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

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------
    //FILES
    //----------------------------------------------------------------------------------------------------

    public String getRelativePath(String fullPath) {
        if (fullPath != null && (fullPath.lastIndexOf(project.getBasePath()) + project.getBasePath().length()) < fullPath.length()) {
            return fullPath.substring(fullPath.lastIndexOf(project.getBasePath()) + project.getBasePath().length());
        }
        else {
            return fullPath;
        }
    }

    public String getRelativePath(VirtualFile virtualFile) {
        String fullPath = virtualFile.getPath();
        if (fullPath != null && (fullPath.lastIndexOf(project.getBasePath()) + project.getBasePath().length()) < fullPath.length()) {
            return fullPath.substring(fullPath.lastIndexOf(project.getBasePath()) + project.getBasePath().length());
        }
        else {
            return fullPath;
        }
    }

    public void includeFile(String localPath) {
        includeFile(localPath, null);
    }

    public void includeFile(String localPath, String remoteMd5) {
        /*
        if (fileSettings != null) {
            String relativePath = getRelativePath(localPath);
            fileSettings.addLinkedFile(relativePath, remoteMd5);
            //refresh();
        }
         */
    }

    public boolean isLinkedFile(String localPath) {
        /*
        if (fileSettings != null) {
            String relativePath = getRelativePath(localPath);
            return fileSettings.getLinkedFile(relativePath) != null;
        }
         */
        return false;
    }

    public ToolboxFile getFile(String localPath) {
        /*
        if (fileSettings != null) {
            String relativePath = getRelativePath(localPath);
            return fileSettings.getLinkedFile(relativePath);
        }
         */
        return null;
    }

    public void removeFile(String localPath) {
        /*
        if (fileSettings != null) {
            String relativePath = getRelativePath(localPath);
            fileSettings.removeLinkedFile(relativePath);
            //refresh();
        }
         */
    }

    //-------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------
    // STATIC INSTANCE
    //-------------------------------------------------------------------------------------

    private static final Map<Project, ToolboxProject> vaultProjects = new HashMap<>();

    public static ToolboxProject initInstance(Project project) {
        try {
            if (!vaultProjects.containsKey(project)) {
                if (LocalDate.now().isBefore(LocalDate.parse("2027-01-01"))) {
                    logger.debug("Initializing Toolbox Project " + project.getName());
                    vaultProjects.put(project, new ToolboxProject(project));
                }
            }
            return vaultProjects.get(project);
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public static ToolboxProject getInstance(Project project) {
        try {
            ToolboxProject vaultProject = vaultProjects.get(project);
            if (vaultProject == null) {
                return initInstance(project);
            }
            return vaultProject;
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public static void closeInstance(Project project) {
        try {
            ToolboxProject toolboxProject = vaultProjects.get(project);
            if (toolboxProject != null) {
                logger.debug("Closing Toolbox Project " + project.getName());
                vaultProjects.remove(project);
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

	public static boolean isProjectFile(VirtualFile file, Project project) {
		logger.debug("Checking if project file " + file.getPath() + " exists in project " + project.getName());
		return Objects.equals(project.getBasePath(), file.getPath());
	}
}
