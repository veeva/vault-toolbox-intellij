package com.veeva.vault.toolbox.intellij.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.veeva.vault.toolbox.core.utils.Date;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.DownloadProfilerLogTask;
import com.veeva.vault.toolbox.intellij.ui.fileviewer.FileViewerDialog;
import com.veeva.vault.vapil.api.model.common.SdkProfilingSession;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.model.response.SdkProfilingSessionBulkResponse;
import com.veeva.vault.vapil.api.model.response.SdkProfilingSessionCreateResponse;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.LogRequest;
import com.veeva.vault.vapil.api.request.QueryRequest;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Panel for managing and analyzing Vault SDK Profiler sessions.
 * Provides capabilities to create, end, delete, and download profiling logs,
 * as well as tools for local log analysis and performance metrics visualization.
 */
public class DeveloperProfilerSessionPanel extends AbstractDeveloperSessionPanel<SdkProfilingSession> {
    private static final Logger logger = LoggerFactory.getLogger(DeveloperProfilerSessionPanel.class);
    private final Map<String, String> userNames = new HashMap<>();

    /**
     * Initializes the SDK Profiler session panel.
     *
     * @param toolboxProject The toolbox project context.
     */
    public DeveloperProfilerSessionPanel(ToolboxProject toolboxProject) {
        super(toolboxProject);
        initUI();

        setupIconRenderer();

        MouseAdapter localMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = sessionTable.rowAtPoint(e.getPoint());
                int col = sessionTable.columnAtPoint(e.getPoint());

                if (row >= 0 && col >= 0) {
                    String colName = sessionTable.getColumnName(col);
                    DeveloperLogItem<SdkProfilingSession> item = allItems.get(sessionTable.convertRowIndexToModel(row));

                    if (item.isLocal()) {
                        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/profiler/" + toolboxProject.getVaultId());
                        String sessionFolderName = item.getItem().getName() + "." + item.getItem().getId();
                        File sessionDir = new File(vaultLogsDir, sessionFolderName);

                        if (sessionDir.exists()) {
                            boolean isActionIcon = "View".equals(colName) || "Locate".equals(colName);

                            if ((isActionIcon && e.getClickCount() == 1) || (!isActionIcon && e.getClickCount() == 2)) {
                                if ("Locate".equals(colName)) {
                                    VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(sessionDir);
                                    if (vFile != null) {
                                        ApplicationManager.getApplication().invokeLater(() -> {
                                            ToolWindow projectViewToolWindow = ToolWindowManager.getInstance(toolboxProject.getProject())
                                                    .getToolWindow(ToolWindowId.PROJECT_VIEW);

                                            if (projectViewToolWindow != null) {
                                                Runnable selectFile = () -> ProjectView.getInstance(toolboxProject.getProject()).select(null, vFile, false);

                                                if (!projectViewToolWindow.isVisible()) {
                                                    projectViewToolWindow.show(selectFile);
                                                } else {
                                                    selectFile.run();
                                                }
                                            }
                                        }, ModalityState.any());
                                    }
                                } else {
                                    new FileViewerDialog(toolboxProject.getProject(), sessionDir).show();
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int row = sessionTable.rowAtPoint(e.getPoint());
                int col = sessionTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    String colName = sessionTable.getColumnName(col);
                    if ("View".equals(colName) || "Locate".equals(colName)) {
                        Object value = sessionTable.getValueAt(row, col);
                        if (value != null) {
                            sessionTable.setCursor(new Cursor(Cursor.HAND_CURSOR));
                            return;
                        }
                    }
                }
                sessionTable.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        };

        sessionTable.addMouseListener(localMouseAdapter);
        sessionTable.addMouseMotionListener(localMouseAdapter);
    }

    /**
     * Configures specialized renderers for action icons in the session table.
     */
    private void setupIconRenderer() {
        TableCellRenderer defaultIconRenderer = sessionTable.getDefaultRenderer(Icon.class);

        TableCellRenderer clickableIconRenderer = (tbl, value, isSelected, hasFocus, row, column) -> {
            Component c = defaultIconRenderer.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
            if (c instanceof JComponent) {
                String colName = sessionTable.getColumnName(column);
                if (value != null) {
                    if ("View".equals(colName)) {
                        ((JComponent) c).setToolTipText("Open in File Viewer");
                    } else if ("Locate".equals(colName)) {
                        ((JComponent) c).setToolTipText("Locate in Project Tree");
                    }
                } else {
                    ((JComponent) c).setToolTipText(null);
                }
            }
            return c;
        };

        sessionTable.getColumnModel().getColumn(2).setCellRenderer(clickableIconRenderer);
        sessionTable.getColumnModel().getColumn(3).setCellRenderer(clickableIconRenderer);
    }

    /**
     * Gets the column names for the SDK Profiler session table.
     *
     * @return An array of column names.
     */
    @Override
    protected String[] getColumnNames() {
        return new String[]{"Select", "Vault", "View", "Locate", "Name", "Label", "Status", "Description", "User ID", "User Name", "Created Date", "Expiration Date"};
    }

    /**
     * Populates a row in the table model for the given SDK Profiler session item.
     *
     * @param item The item to populate the row with.
     */
    @Override
    protected void populateRow(DeveloperLogItem<SdkProfilingSession> item) {
        SdkProfilingSession session = item.getItem();
        String userId = session.getUserId() != null ? session.getUserId().toString() : null;
        String userName = userId != null ? userNames.getOrDefault(userId, "") : "All Users";

        tableModel.addRow(new Object[]{
                false,
                getVaultIcon(item),
                item.isLocal() ? AllIcons.Actions.Show : null,
                item.isLocal() ? AllIcons.General.Locate : null,
                session.getName(),
                session.getLabel(),
                getStatusLabel(item.isInVault() ? session.getStatus() : null),
                session.getDescription(),
                userId,
                userName,
                Date.formatVaultDateTime(session.getCreatedDate()),
                Date.formatVaultDateTime(session.getExpirationDate())
        });
    }

    /**
     * Creates the action group for the toolbar.
     *
     * @return The created DefaultActionGroup.
     */
    @Override
    protected DefaultActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new AnAction("Create Session", "Create a new profiling session", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                createSession();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                boolean anyInProgress = allItems.stream()
                        .filter(DeveloperLogItem::isInVault)
                        .anyMatch(item -> "in_progress__sys".equalsIgnoreCase(item.getItem().getStatus()));
                e.getPresentation().setEnabled(!anyInProgress);
            }
        });

        actionGroup.add(new AnAction("End Session", "End selected active session(s)", AllIcons.Actions.Suspend) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                endSelectedSessions();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeveloperLogItem<SdkProfilingSession>> selected = getSelectedItems();
                e.getPresentation().setEnabled(selected.size() == 1 &&
                        selected.get(0).isInVault() &&
                        "in_progress__sys".equalsIgnoreCase(selected.get(0).getItem().getStatus()));
            }
        });

        actionGroup.add(new AnAction("Delete Session", "Delete selected inactive session(s) from Vault", AllIcons.Actions.GC) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                deleteSelectedVaultSessions();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeveloperLogItem<SdkProfilingSession>> selected = getSelectedItems();
                boolean allSelectedAreDeletable = !selected.isEmpty() && selected.stream()
                        .allMatch(item -> item.isInVault() &&
                                ("complete__sys".equalsIgnoreCase(item.getItem().getStatus()) ||
                                        "error__sys".equalsIgnoreCase(item.getItem().getStatus())));
                e.getPresentation().setEnabled(allSelectedAreDeletable);
            }
        });

        actionGroup.add(new AnAction("Download Log", "Download logs for selected completed session(s)", AllIcons.Actions.Download) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                downloadSelectedLogs();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeveloperLogItem<SdkProfilingSession>> selected = getSelectedItems();
                boolean allSelectedAreComplete = !selected.isEmpty() && selected.stream()
                        .allMatch(item -> item.isInVault() && "complete__sys".equalsIgnoreCase(item.getItem().getStatus()));
                boolean anyNotDownloaded = selected.stream().anyMatch(item -> !item.isLocal());
                e.getPresentation().setEnabled(allSelectedAreComplete && anyNotDownloaded);
            }
        });

        actionGroup.addSeparator();

        actionGroup.add(new AnAction("Analyze Logs", "Analyze selected downloaded logs", AllIcons.Actions.IntentionBulb) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                analyzeSelectedLogs();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeveloperLogItem<SdkProfilingSession>> selected = getSelectedItems();
                boolean allLocal = selected.stream().allMatch(DeveloperLogItem::isLocal);
                e.getPresentation().setEnabled(!selected.isEmpty() && allLocal);
            }
        });

        actionGroup.add(new AnAction("Clear Local Files", "Delete selected session files from local drive", AllIcons.Actions.Cancel) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                deleteSelectedLocalSessions();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeveloperLogItem<SdkProfilingSession>> selected = getSelectedItems();
                boolean anyLocal = selected.stream().anyMatch(DeveloperLogItem::isLocal);
                e.getPresentation().setEnabled(!selected.isEmpty() && anyLocal);
            }
        });

        actionGroup.addSeparator();

        actionGroup.add(new AnAction("Refresh", "Refresh list", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                loadData();
            }
        });

        return actionGroup;
    }

    /**
     * Loads the data to be displayed in the panel.
     */
    @Override
    public void loadData() {
        if (!toolboxProject.prepareRequest()) return;

        new Thread(() -> {
            try {
                Map<String, DeveloperLogItem<SdkProfilingSession>> itemMap = new HashMap<>();
                List<String> userIds = new ArrayList<>();

                File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/profiler/" + toolboxProject.getVaultId());
                if (vaultLogsDir.exists()) {
                    File[] sessionDirs = vaultLogsDir.listFiles(File::isDirectory);
                    if (sessionDirs != null) {
                        ObjectMapper mapper = new ObjectMapper();
                        for (File sessionDir : sessionDirs) {
                            File[] jsonFiles = sessionDir.listFiles((dir, name) -> name.endsWith(".json"));
                            if (jsonFiles != null && jsonFiles.length > 0) {
                                try {
                                    SdkProfilingSession session = mapper.readValue(jsonFiles[0], SdkProfilingSession.class);
                                    itemMap.put(session.getId(), new DeveloperLogItem<>(session, false, true));
                                    if (session.getUserId() != null) userIds.add(session.getUserId().toString());
                                } catch (Exception ex) {
                                    logger.error("Error reading JSON file in: " + sessionDir.getName(), ex);
                                }
                            }
                        }
                    }
                }

                SdkProfilingSessionBulkResponse response = toolboxProject.getVaultClient()
                        .newRequest(LogRequest.class)
                        .retrieveAllProfilingSessions();

                if (toolboxProject.handleSessionExpiration(response)) {
                    return;
                }

                if (response != null && !response.isFailure()) {
                    for (SdkProfilingSession vSession : response.getData()) {
                        boolean wasLocal = itemMap.containsKey(vSession.getId());
                        itemMap.put(vSession.getId(), new DeveloperLogItem<>(vSession, true, wasLocal));
                        if (vSession.getUserId() != null) userIds.add(vSession.getUserId().toString());
                    }
                }

                Map<String, String> newUserNames = resolveUserNames(userIds);

                List<DeveloperLogItem<SdkProfilingSession>> newItems = new ArrayList<>(itemMap.values());
                newItems.sort((a, b) -> {
                    String dateA = a.getItem().getCreatedDate();
                    String dateB = b.getItem().getCreatedDate();
                    if (dateA == null && dateB == null) return 0;
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA);
                });

                SwingUtilities.invokeLater(() -> {
                    allItems.clear();
                    allItems.addAll(newItems);
                    userNames.clear();
                    userNames.putAll(newUserNames);
                    filterAndUpdateTable();
                    if (sessionTable != null) {
                        sessionTable.packAll();
                    }
                });

            } catch (Exception e) {
                logger.error("Error loading SDK Profiler sessions", e);
                SwingUtilities.invokeLater(() -> {
                    filterAndUpdateTable();
                    if (sessionTable != null) {
                        sessionTable.packAll();
                    }
                    JOptionPane.showMessageDialog(this, "An error occurred while loading sessions.", "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    /**
     * Resolves Vault user IDs to usernames by querying the Vault user table.
     *
     * @param userIds List of user IDs to resolve.
     */
    private Map<String, String> resolveUserNames(List<String> userIds) {
        Map<String, String> resolved = new HashMap<>();
        if (!userIds.isEmpty()) {
            String ids = userIds.stream().distinct().map(id -> "'" + id + "'").collect(Collectors.joining(","));
            String query = "SELECT id, username__sys FROM user__sys WHERE id CONTAINS (" + ids + ")";

            QueryResponse queryResponse = toolboxProject.getVaultClient()
                    .newRequest(QueryRequest.class)
                    .query(query);

            if (queryResponse != null && !queryResponse.isFailure()) {
                for (QueryResponse.QueryResult result : queryResponse.getData()) {
                    resolved.put(result.getString("id"), result.getString("username__sys"));
                }
            }
        }
        return resolved;
    }

    /**
     * Maps Vault session status codes to user-friendly labels.
     *
     * @param status The status code from Vault.
     * @return A human-readable status string.
     */
    private String getStatusLabel(String status) {
        if (status == null) return "";
        switch (status) {
            case "in_progress__sys": return "In Progress";
            case "processing__sys": return "Processing";
            case "complete__sys": return "Complete";
            case "error__sys": return "Error";
            default: return status;
        }
    }

    /**
     * Opens the dialog to create a new SDK Profiler session.
     * Enforces limits on concurrent and total sessions.
     */
    private void createSession() {
        long vaultCount = allItems.stream().filter(DeveloperLogItem::isInVault).count();
        if (vaultCount >= 10) {
            JOptionPane.showMessageDialog(this, "Cannot create new session. Maximum limit of 10 sessions reached. Please delete old logs.", "Limit Reached", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean activeExists = allItems.stream()
                .filter(DeveloperLogItem::isInVault)
                .anyMatch(item -> "in_progress__sys".equalsIgnoreCase(item.getItem().getStatus()));

        if (activeExists) {
            JOptionPane.showMessageDialog(this, "Cannot create new session. An active session already exists.", "Active Session Exists", JOptionPane.WARNING_MESSAGE);
            return;
        }

        CreateProfilerSessionDialog dialog = new CreateProfilerSessionDialog(toolboxProject);
        if (dialog.showAndGet()) {
            new Thread(() -> {
                try {
                    LogRequest request = toolboxProject.getVaultClient().newRequest(LogRequest.class);
                    if (dialog.getUserId() != null) {
                        request.setUserId(dialog.getUserId());
                    }
                    if (dialog.getDescription() != null && !dialog.getDescription().isEmpty()) {
                        request.setDescription(dialog.getDescription());
                    }

                    SdkProfilingSessionCreateResponse response = request.createProfilingSession(dialog.getLabel());

                    if (response != null && !response.isFailure()) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "Session created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                            loadData();
                        });
                    } else {
                        String errorMessage = response != null ? response.getResponseMessage() : "Unknown error";
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to create session: " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE));
                    }
                } catch (Exception e) {
                    logger.error("Error creating SDK Profiler session", e);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "An error occurred while creating session.", "Error", JOptionPane.ERROR_MESSAGE));
                }
            }).start();
        }
    }

    /**
     * Explicitly ends the selected active profiling sessions in Vault.
     */
    private void endSelectedSessions() {
        List<DeveloperLogItem<SdkProfilingSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        new Thread(() -> {
            for (DeveloperLogItem<SdkProfilingSession> item : selectedSessions) {
                if (item.isInVault()) {
                    try {
                        VaultResponse response = toolboxProject.getVaultClient()
                                .newRequest(LogRequest.class)
                                .endProfilingSession(item.getItem().getName());

                        if (response == null || response.isFailure()) {
                            String errorMessage = response != null ? response.getResponseMessage() : "Unknown error";
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to end session " + item.getItem().getName() + ": " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE));
                        }
                    } catch (Exception e) {
                        logger.error("Error ending SDK Profiler session " + item.getItem().getName(), e);
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "An error occurred while ending session " + item.getItem().getName(), "Error", JOptionPane.ERROR_MESSAGE));
                    }
                }
            }
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Selected sessions processed.", "Info", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            });
        }).start();
    }

    /**
     * Deletes the selected inactive profiling sessions from the remote Vault instance.
     */
    private void deleteSelectedVaultSessions() {
        List<DeveloperLogItem<SdkProfilingSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the selected session(s) from Vault?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            if (toolboxProject.isProductionVault()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "This tool cannot be run in a Production domain.", "Error", JOptionPane.ERROR_MESSAGE));
                return;
            }
            for (DeveloperLogItem<SdkProfilingSession> item : selectedSessions) {
                if (item.isInVault()) {
                    try {
                        VaultResponse response = toolboxProject.getVaultClient()
                                .newRequest(LogRequest.class)
                                .deleteProfilingSession(item.getItem().getName());

                        if (toolboxProject.handleSessionExpiration(response)) {
                            break;
                        }

                        if (response == null || response.isFailure()) {
                            String errorMessage = response != null ? response.getResponseMessage() : "Unknown error";
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to delete session " + item.getItem().getName() + ": " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE));
                        }
                    } catch (Exception e) {
                        logger.error("Error deleting SDK Profiler session " + item.getItem().getName(), e);
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "An error occurred while deleting session " + item.getItem().getName(), "Error", JOptionPane.ERROR_MESSAGE));
                    }
                }
            }
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Selected sessions deleted from Vault.", "Info", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            });
        }).start();
    }

    /**
     * Downloads the logs for the selected items in the session table.
     */
    @Override
    protected void downloadSelectedLogs() {
        List<DeveloperLogItem<SdkProfilingSession>> selectedSessions = getSelectedItems();
        List<SdkProfilingSession> toDownload = new ArrayList<>();
        for (DeveloperLogItem<SdkProfilingSession> item : selectedSessions) {
            if (item.isInVault() && !item.isLocal()) {
                toDownload.add(item.getItem());
            }
        }

        if (toDownload.isEmpty()) return;

        DownloadProfilerLogTask task = new DownloadProfilerLogTask(toolboxProject.getProject(), toDownload, () -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Successfully downloaded " + toDownload.size() + " log(s).", "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            });
        });
        task.queue();
    }

    /**
     * Initiates the analysis of selected local profiling logs.
     * Opens the performance visualization dialog.
     */
    private void analyzeSelectedLogs() {
        List<DeveloperLogItem<SdkProfilingSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        Map<SdkProfilingSession, File> sessionFiles = new HashMap<>();
        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/profiler/" + toolboxProject.getVaultId());

        for (DeveloperLogItem<SdkProfilingSession> item : selectedSessions) {
            if (item.isLocal()) {
                String sessionFolderName = item.getItem().getName() + "." + item.getItem().getId();
                File sessionDir = new File(vaultLogsDir, sessionFolderName);

                if (sessionDir.exists()) {
                    File[] csvFiles = sessionDir.listFiles((dir, name) -> name.endsWith(".csv"));
                    if (csvFiles != null && csvFiles.length > 0) {
                        sessionFiles.put(item.getItem(), csvFiles[0]);
                    }
                }
            }
        }

        if (!sessionFiles.isEmpty()) {
            SdkProfilerAnalysisDialog dialog = new SdkProfilerAnalysisDialog(toolboxProject.getProject(), sessionFiles);
            dialog.show();
        } else {
            Messages.showWarningDialog(toolboxProject.getProject(), "No CSV files found to analyze.", "Analysis Error");
        }
    }

    /**
     * Deletes the local log directories for the selected profiling sessions.
     */
    private void deleteSelectedLocalSessions() {
        List<DeveloperLogItem<SdkProfilingSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete local files for the selected session(s)?", "Confirm Local Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/profiler/" + toolboxProject.getVaultId());

        for (DeveloperLogItem<SdkProfilingSession> item : selectedSessions) {
            if (item.isLocal()) {
                String sessionFolderName = item.getItem().getName() + "." + item.getItem().getId();
                File sessionDir = new File(vaultLogsDir, sessionFolderName);

                if (sessionDir.exists()) {
                    try {
                        FileUtils.deleteDirectory(sessionDir);
                    } catch (IOException e) {
                        logger.error("Failed to delete profiler log directory: " + sessionDir.getName(), e);
                    }
                }
            }
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile vVaultLogsDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(vaultLogsDir);
            if (vVaultLogsDir != null) {
                vVaultLogsDir.refresh(true, true);
            }
        });
        toolboxProject.refresh();

        JOptionPane.showMessageDialog(this, "Selected local files deleted.", "Info", JOptionPane.INFORMATION_MESSAGE);
        loadData();
    }
}
