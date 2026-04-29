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

public class LogPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(LogPanel.class);

    private enum LogType {
        API_USAGE,
        SDK_DEBUG,
        SDK_RUNTIME
    }

    ToolboxProject toolboxProject;
    boolean includeDownload;
    boolean includeStartButton = false;
    LogType logType;

    JPanel mainPanel = new JPanel();
    JPanel apiUsagePanel = new JPanel(new GridLayout(5,1));
    JPanel sdkDebugPanel = new JPanel(new GridLayout(4,1));
    JPanel sdkRuntimePanel = new JPanel(new GridLayout(4,1));
    JBTabbedPane logsTab = new JBTabbedPane();
    ToolboxButton startButton = new ToolboxButton(toolboxProject, "Start");

    JXDatePicker apiStartDatePicker = new JXDatePicker();
    JXDatePicker apiEndDatePicker = new JXDatePicker();
    JXDatePicker sdkStartDatePicker = new JXDatePicker();
    JXDatePicker sdkEndDatePicker = new JXDatePicker();
    JXComboBox sdkDebugUser = new JXComboBox();

    public LogPanel(ToolboxProject toolboxProject, boolean includeDownload, LogType logType) {
        super();
        this.toolboxProject = toolboxProject;
        this.includeDownload = includeDownload;
        this.logType = logType;
        this.setLayout(new BorderLayout());
        //this.setSize(400, 400);
        //this.setMaximumSize(new Dimension(400, 400));
        //this.setBorder(new EmptyBorder(20, 20, 20, 20));
        init();
    }

    public boolean isApiSelected() {
        return logsTab.getSelectedIndex() == 0;
    }

    public boolean isSdkDebugSelected() {
        return logsTab.getSelectedIndex() == 1;
    }

    public boolean isSdkRuntimeSelected() {
        return logsTab.getSelectedIndex() == 3;
    }

    public LocalDate getApiStartDate() {
        return apiStartDatePicker.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public LocalDate getApiEndDate() {
        return apiEndDatePicker.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public LocalDate getSdkStartDate() {
        return sdkStartDatePicker.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public LocalDate getSdkEndDate() {
        return sdkEndDatePicker.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public String getDebugLogId() {
        DebugLogItem debugLogItem = (DebugLogItem)sdkDebugUser.getSelectedItem();
        if (debugLogItem != null) {
            return debugLogItem.getId();
        }
        return null;
    }

    protected void init() {
        //Date startDate = Date.from(LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant());
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
        }
        else {
            apiUsagePanel.add(new JLabel("Analyze existing API Usage Logs"));
            sdkDebugPanel.add(new JLabel("Analyze existing SDK Debug"));
        }

        if (includeStartButton) {
            logger.debug("include start");
            this.mainPanel.add(startButton);
            startButton.addActionListener(e -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    this.start();
                });
            });
        }
        this.add(mainPanel);
    }

    private void loadDebugUsers() {
        sdkDebugUser.removeAllItems();
        SdkDebugSessionBulkResponse bulkResponse = toolboxProject.getVaultClient()
                .newRequest(LogRequest.class)
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

    public void start() {
        logger.debug("start");
        VirtualFile virtualFile = VfsUtil.findFileByIoFile(toolboxProject.getLogsDirectory(), true);
        if (includeDownload) {
            if (logsTab.getSelectedComponent().equals(apiUsagePanel)) {
				assert virtualFile != null;
				DownloadAnalyzeApiLogTask task = new DownloadAnalyzeApiLogTask(toolboxProject.getProject(),
                        virtualFile, this.getApiStartDate(), this.getApiEndDate());
                task.queue();
            }
            else if (logsTab.getSelectedComponent().equals(sdkDebugPanel)) {
				assert virtualFile != null;
				DownloadAnalyzeDebugLogTask task = new DownloadAnalyzeDebugLogTask(toolboxProject.getProject(),
                        virtualFile, this.getDebugLogId());
                task.queue();
            }
        }
        else {
            if (logsTab.getSelectedComponent().equals(apiUsagePanel)) {
				assert virtualFile != null;
				AnalyzApiLogTask task = new AnalyzApiLogTask(toolboxProject.getProject(), virtualFile);
                task.queue();
            }
            else if (logsTab.getSelectedComponent().equals(sdkDebugPanel)) {
				assert virtualFile != null;
				AnalyzDebugLogTask task = new AnalyzDebugLogTask(toolboxProject.getProject(), virtualFile);
                task.queue();
            }
        }
    }

    public static class DebugLogItem {
        private final String id;
        private final String label;

        public DebugLogItem(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public String toString() {
            return label;
        }
    }
}