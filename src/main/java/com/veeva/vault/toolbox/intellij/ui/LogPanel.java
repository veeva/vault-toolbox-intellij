package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTabbedPane;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.AnalyzApiLogTask;
import com.veeva.vault.toolbox.intellij.tasks.AnalyzDebugLogTask;
import com.veeva.vault.toolbox.intellij.tasks.DownloadAnalyzeApiLogTask;
import com.veeva.vault.toolbox.intellij.tasks.DownloadAnalyzeDebugLogTask;
import com.veeva.vault.vapil.api.model.common.SdkDebugSession;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.model.response.SdkDebugSessionBulkResponse;
import com.veeva.vault.vapil.api.request.LogRequest;
import com.veeva.vault.vapil.api.request.QueryRequest;
import icons.ToolboxIcons;
import org.jdesktop.swingx.JXComboBox;
import org.jdesktop.swingx.JXDatePicker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel for configuring and initiating log analysis tasks.
 * Supports API Usage, SDK Debug, and SDK Runtime logs.
 */
public class LogPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(LogPanel.class);

    private enum LogType {
        API_USAGE,
        SDK_DEBUG,
        SDK_RUNTIME
    }

    private final ToolboxProject toolboxProject;
    private final boolean includeDownload;
    private final boolean includeStartButton = false;
    private final LogType logType;

    private final JPanel mainPanel = new JPanel();
    private final JPanel apiUsagePanel = new JPanel(new GridLayout(5, 1));
    private final JPanel sdkDebugPanel = new JPanel(new GridLayout(4, 1));
    private final JPanel sdkRuntimePanel = new JPanel(new GridLayout(4, 1));
    private final JBTabbedPane logsTab = new JBTabbedPane();
    private final ToolboxButton startButton;

    private final JXDatePicker apiStartDatePicker = new JXDatePicker();
    private final JXDatePicker apiEndDatePicker = new JXDatePicker();
    private final JXDatePicker sdkStartDatePicker = new JXDatePicker();
    private final JXDatePicker sdkEndDatePicker = new JXDatePicker();
    private final JXComboBox sdkDebugUser = new JXComboBox();

    /**
     * Initializes the log configuration panel.
     *
     * @param toolboxProject  The toolbox project context.
     * @param includeDownload true to include download configuration options.
     * @param logType         The type of log to configure.
     */
    public LogPanel(ToolboxProject toolboxProject, boolean includeDownload, LogType logType) {
        super();
        this.toolboxProject = toolboxProject;
        this.includeDownload = includeDownload;
        this.logType = logType;
        this.startButton = new ToolboxButton(toolboxProject, "Start");
        this.setLayout(new BorderLayout());
        init();
    }

    /**
     * Checks if the API Usage log tab is currently selected.
     *
     * @return true if the API Usage tab is selected, false otherwise.
     */
    public boolean isApiSelected() {
        return logsTab.getSelectedIndex() == 0;
    }

    /**
     * Checks if the SDK Debug log tab is currently selected.
     *
     * @return true if the SDK Debug tab is selected, false otherwise.
     */
    public boolean isSdkDebugSelected() {
        return logsTab.getSelectedIndex() == 1;
    }

    /**
     * Checks if the SDK Runtime log tab is currently selected.
     *
     * @return true if the SDK Runtime tab is selected, false otherwise.
     */
    public boolean isSdkRuntimeSelected() {
        return logsTab.getSelectedIndex() == 3;
    }

    /**
     * Retrieves the start date for API log analysis from the date picker.
     *
     * @return The selected start date as a LocalDate.
     */
    public LocalDate getApiStartDate() {
        return apiStartDatePicker.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    /**
     * Retrieves the end date for API log analysis from the date picker.
     *
     * @return The selected end date as a LocalDate.
     */
    public LocalDate getApiEndDate() {
        return apiEndDatePicker.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    /**
     * Retrieves the start date for SDK log analysis from the date picker.
     *
     * @return The selected start date as a LocalDate.
     */
    public LocalDate getSdkStartDate() {
        return sdkStartDatePicker.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    /**
     * Retrieves the end date for SDK log analysis from the date picker.
     *
     * @return The selected end date as a LocalDate.
     */
    public LocalDate getSdkEndDate() {
        return sdkEndDatePicker.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    /**
     * Retrieves the unique ID of the selected debug log session from the user dropdown.
     *
     * @return The debug log session ID, or null if no session is selected.
     */
    public String getDebugLogId() {
        DebugLogItem debugLogItem = (DebugLogItem) sdkDebugUser.getSelectedItem();
        return debugLogItem != null ? debugLogItem.getId() : null;
    }

    /**
     * Configures the UI layout and components based on the panel initialization flags.
     */
    protected void init() {
        Date today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        apiStartDatePicker.setDate(today);
        sdkStartDatePicker.setDate(today);
        apiEndDatePicker.setDate(today);
        sdkEndDatePicker.setDate(today);
        this.mainPanel.add(logsTab);
        logsTab.addTab("", ToolboxIcons.Api, apiUsagePanel);
        logsTab.addTab("", ToolboxIcons.Debug, sdkDebugPanel);

        if (includeDownload) {
            logsTab.addTab("", ToolboxIcons.Runtime, sdkRuntimePanel);
            apiUsagePanel.add(new JLabel("Download and analyze API Usage Logs"));
            sdkDebugPanel.add(new JLabel("Download and analyze SDK Debug Logs"));
            sdkDebugPanel.add(new JLabel("Download SDK Runtime Logs"));

            apiUsagePanel.add(new JLabel("Start Date:"));
            apiUsagePanel.add(apiStartDatePicker);
            apiUsagePanel.add(new JLabel("End Date:"));
            apiUsagePanel.add(apiEndDatePicker);

            sdkDebugPanel.add(new JLabel("User:"));
            sdkDebugPanel.add(sdkDebugUser);

            sdkRuntimePanel.add(new JLabel("Start Date:"));
            sdkRuntimePanel.add(sdkStartDatePicker);
            sdkRuntimePanel.add(new JLabel("End Date:"));
            sdkRuntimePanel.add(sdkEndDatePicker);
        } else {
            apiUsagePanel.add(new JLabel("Analyze existing API Usage Logs"));
            sdkDebugPanel.add(new JLabel("Analyze existing SDK Debug"));
        }

        if (includeStartButton) {
            this.mainPanel.add(startButton);
            startButton.addActionListener(e -> ApplicationManager.getApplication().invokeLater(this::start));
        }
        this.add(mainPanel);
    }

    /**
     * Fetches debug session users from Vault and populates the user selection dropdown.
     */
    private void loadDebugUsers() {
        sdkDebugUser.removeAllItems();
        SdkDebugSessionBulkResponse bulkResponse = toolboxProject.getVaultClient()
                .newRequest(LogRequest.class)
                .setIncludeInactive(true)
                .retrieveAllDebugLogs();
        if (bulkResponse != null && !bulkResponse.isFailure()) {
            ArrayList<String> userIds = new ArrayList<>();
            for (SdkDebugSession debugSession : bulkResponse.getData()) {
                userIds.add(debugSession.getUserId().toString());
            }

            Map<String, String> users = new HashMap<>();
			String query = "SELECT id, username__sys FROM user__sys WHERE id CONTAINS (" +
					String.join(",", userIds) +
					")";
            QueryResponse queryResponse = toolboxProject.getVaultClient().newRequest(QueryRequest.class)
                    .query(query);
            if (queryResponse != null && !queryResponse.isFailure()) {
                for (QueryResponse.QueryResult queryResult : queryResponse.getData()) {
                    users.put(queryResult.getString("id"), queryResult.getString("username__sys"));
                }
            }

            for (SdkDebugSession debugSession : bulkResponse.getData()) {
                String username = users.get(debugSession.getUserId().toString());
                DebugLogItem debugLogItem = new DebugLogItem(debugSession.getId(), username);
                sdkDebugUser.addItem(debugLogItem);
            }
        }
    }

    /**
     * Starts the configured log analysis or download task.
     */
    public void start() {
        VirtualFile virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
        if (includeDownload) {
            if (logsTab.getSelectedComponent().equals(apiUsagePanel)) {
				assert virtualFile != null;
				DownloadAnalyzeApiLogTask task = new DownloadAnalyzeApiLogTask(toolboxProject.getProject(),
                        virtualFile, this.getApiStartDate(), this.getApiEndDate());
                task.queue();
            } else if (logsTab.getSelectedComponent().equals(sdkDebugPanel)) {
				assert virtualFile != null;
				DownloadAnalyzeDebugLogTask task = new DownloadAnalyzeDebugLogTask(toolboxProject.getProject(),
                        virtualFile, this.getDebugLogId());
                task.queue();
            }
        } else {
            if (logsTab.getSelectedComponent().equals(apiUsagePanel)) {
				assert virtualFile != null;
				AnalyzApiLogTask task = new AnalyzApiLogTask(toolboxProject.getProject(), virtualFile);
                task.queue();
            } else if (logsTab.getSelectedComponent().equals(sdkDebugPanel)) {
				assert virtualFile != null;
				AnalyzDebugLogTask task = new AnalyzDebugLogTask(toolboxProject.getProject(), virtualFile);
                task.queue();
            }
        }
    }

    /**
     * Data class for entries in the debug session user dropdown.
     */
    public static class DebugLogItem {
        private final String id;
        private final String label;

        /**
         * Constructs a new DebugLogItem with a specific ID and display label.
         *
         * @param id    The unique identifier for the debug log.
         * @param label The display label (typically the username) for the debug log.
         */
        public DebugLogItem(String id, String label) {
            this.id = id;
            this.label = label;
        }

        /**
         * Returns the unique identifier for the debug log.
         *
         * @return the debug log ID.
         */
        public String getId() {
            return id;
        }

        /**
         * Returns the display label for the debug log.
         *
         * @return the debug log label.
         */
        public String getLabel() {
            return label;
        }

        /**
         * Returns the label to be displayed in the UI component (e.g., JComboBox).
         *
         * @return the display label.
         */
        @Override
        public String toString() {
            return label;
        }
    }
}
