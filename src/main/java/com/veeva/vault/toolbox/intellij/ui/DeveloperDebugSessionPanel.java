package com.veeva.vault.toolbox.intellij.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.Messages;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.AnalyzeLocalDebugLogTask;
import com.veeva.vault.toolbox.intellij.tasks.DownloadDebugLogTask;
import com.veeva.vault.vapil.api.model.common.SdkDebugSession;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.model.response.SdkDebugSessionBulkResponse;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.LogRequest;
import icons.ToolboxIcons;
import org.jdesktop.swingx.JXTable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeveloperDebugSessionPanel extends AbstractDeveloperSessionPanel<SdkDebugSession> {
    private static final Logger logger = LoggerFactory.getLogger(DeveloperDebugSessionPanel.class);
    private Map<String, String> userNames = new HashMap<>();

    public DeveloperDebugSessionPanel(ToolboxProject toolboxProject) {
        super(toolboxProject);
        initUI();

        setupIconRenderer();

        java.awt.event.MouseAdapter localMouseAdapter = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = sessionTable.rowAtPoint(e.getPoint());
                int col = sessionTable.columnAtPoint(e.getPoint());

                if (row >= 0 && col >= 0) {
                    String colName = sessionTable.getColumnName(col);
                    DeveloperLogItem<SdkDebugSession> item = allItems.get(sessionTable.convertRowIndexToModel(row));

                    if (item.isLocal()) {
                        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/debug/" + toolboxProject.getVaultId());
                        String baseFilename = item.getItem().getName() + "." + item.getItem().getId();
                        File sessionDir = new File(vaultLogsDir, baseFilename);

                        if (sessionDir.exists()) {
                            boolean isActionIcon = "View".equals(colName) || "Locate".equals(colName);

                            if ((isActionIcon && e.getClickCount() == 1) || (!isActionIcon && e.getClickCount() == 2)) {
                                if ("Locate".equals(colName)) {
                                    com.intellij.openapi.vfs.VirtualFile vFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(sessionDir);
                                    if (vFile != null) {
                                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                                            com.intellij.openapi.wm.ToolWindow projectViewToolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(toolboxProject.getProject())
                                                    .getToolWindow(com.intellij.openapi.wm.ToolWindowId.PROJECT_VIEW);

                                            if (projectViewToolWindow != null) {
                                                Runnable selectFile = () -> {
                                                    com.intellij.ide.projectView.ProjectView.getInstance(toolboxProject.getProject()).select(null, vFile, false);
                                                };

                                                if (!projectViewToolWindow.isVisible()) {
                                                    projectViewToolWindow.show(selectFile);
                                                } else {
                                                    selectFile.run();
                                                }
                                            }
                                        }, com.intellij.openapi.application.ModalityState.any());
                                    }
                                } else {
                                    new com.veeva.vault.toolbox.intellij.ui.fileviewer.FileViewerDialog(toolboxProject.getProject(), sessionDir).show();
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = sessionTable.rowAtPoint(e.getPoint());
                int col = sessionTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    String colName = sessionTable.getColumnName(col);
                    if ("View".equals(colName) || "Locate".equals(colName)) {
                        Object value = sessionTable.getValueAt(row, col);
                        if (value != null) {
                            sessionTable.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                            return;
                        }
                    }
                }
                sessionTable.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        };

        sessionTable.addMouseListener(localMouseAdapter);
        sessionTable.addMouseMotionListener(localMouseAdapter);

        loadData();
    }

    private void setupIconRenderer() {
        javax.swing.table.TableCellRenderer defaultIconRenderer = sessionTable.getDefaultRenderer(Icon.class);

        javax.swing.table.TableCellRenderer clickableIconRenderer = (tbl, value, isSelected, hasFocus, row, column) -> {
            java.awt.Component c = defaultIconRenderer.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
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

    @Override
    protected String[] getColumnNames() {
        return new String[]{"Select", "Vault", "View", "Locate", "ID", "Name", "User ID", "User Name", "Log Level", "Status", "Created Date", "Expiration Date"};
    }

    @Override
    protected void populateRow(DeveloperLogItem<SdkDebugSession> item) {
        SdkDebugSession session = item.getItem();
        String userId = session.getUserId() != null ? session.getUserId().toString() : null;
        String userName = userId != null ? userNames.getOrDefault(userId, "") : "All Users";

        tableModel.addRow(new Object[]{
                false,              // 0: Checkbox
                getVaultIcon(item), // 1: Vault Icon
                item.isLocal() ? AllIcons.Actions.Show : null,   // 2: View Icon
                item.isLocal() ? AllIcons.General.Locate : null, // 3: Locate Icon
                session.getId(),    // 4: ID
                session.getName(),  // 5: Name
                userId,             // 6: User ID
                userName,           // 7: User Name
                formatLogLevel(session.getLogLevel()), // 8: Log Level
                formatStatus(session.getStatus()),     // 9: Status
                session.getCreatedDate(),              // 10: Created Date
                session.getExpirationDate()            // 11: Expiration Date
        });
    }


    @Override
    protected DefaultActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new AnAction("Create Session", "Create a new debug log session", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                createSession();
            }
        });

        actionGroup.add(new AnAction("Reset Session", "Reset selected session(s)", AllIcons.Actions.Restart) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                resetSelectedSessions();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeveloperLogItem<SdkDebugSession>> selected = getSelectedItems();
                boolean anyInVault = selected.stream().anyMatch(DeveloperLogItem::isInVault);
                e.getPresentation().setEnabled(!selected.isEmpty() && anyInVault);
            }
        });

        actionGroup.add(new AnAction("Delete from Vault", "Delete selected session(s) from Vault", AllIcons.Actions.GC) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                deleteSelectedVaultSessions();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeveloperLogItem<SdkDebugSession>> selected = getSelectedItems();
                boolean anyInVault = selected.stream().anyMatch(DeveloperLogItem::isInVault);
                e.getPresentation().setEnabled(!selected.isEmpty() && anyInVault);
            }
        });

        actionGroup.add(new AnAction("Download / Update Log", "Download or update logs for selected session(s)", AllIcons.Actions.Download) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                downloadSelectedLogs();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeveloperLogItem<SdkDebugSession>> selected = getSelectedItems();
                boolean canDownload = selected.stream().anyMatch(DeveloperLogItem::isInVault);
                e.getPresentation().setEnabled(!selected.isEmpty() && canDownload);
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
                List<DeveloperLogItem<SdkDebugSession>> selected = getSelectedItems();
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
                List<DeveloperLogItem<SdkDebugSession>> selected = getSelectedItems();
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

    @Override
    public void loadData() {
        if (!toolboxProject.prepareRequest()) return;

        new Thread(() -> {
            try {
                allItems.clear();
                userNames.clear();
                Map<String, DeveloperLogItem<SdkDebugSession>> itemMap = new HashMap<>();
                List<String> userIds = new ArrayList<>();

                File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/debug/" + toolboxProject.getVaultId());
                if (vaultLogsDir.exists()) {
                    try {
                        List<File> jsonFiles = FileIO.getFiles(vaultLogsDir, ".json");
                        if (jsonFiles != null) {
                            ObjectMapper mapper = new ObjectMapper();
                            for (File jsonFile : jsonFiles) {
                                try {
                                    SdkDebugSession session = mapper.readValue(jsonFile, SdkDebugSession.class);
                                    itemMap.put(session.getId(), new DeveloperLogItem<>(session, false, true));
                                    if (session.getUserId() != null) userIds.add(session.getUserId().toString());
                                } catch (Exception ex) {
                                    logger.error("Error reading JSON file: " + jsonFile.getName(), ex);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        logger.error("Error listing JSON files", ex);
                    }
                }

                SdkDebugSessionBulkResponse response = toolboxProject.getVaultClient()
                        .newRequest(LogRequest.class)
                        .retrieveAllDebugLogs();

                if (toolboxProject.handleSessionExpiration(response)) {
                    return;
                }

                if (response != null && !response.isFailure()) {
                    for (SdkDebugSession vSession : response.getData()) {
                        if (itemMap.containsKey(vSession.getId())) {
                            DeveloperLogItem<SdkDebugSession> item = itemMap.get(vSession.getId());
                            item.setInVault(true);
                            item.getItem().setStatus(vSession.getStatus());
                            item.getItem().setExpirationDate(vSession.getExpirationDate());
                        } else {
                            itemMap.put(vSession.getId(), new DeveloperLogItem<>(vSession, true, false));
                        }
                        if (vSession.getUserId() != null) userIds.add(vSession.getUserId().toString());
                    }
                }

                resolveUserNames(userIds);

                allItems.addAll(itemMap.values());
                allItems.sort((a, b) -> {
                    String dateA = a.getItem().getCreatedDate();
                    String dateB = b.getItem().getCreatedDate();
                    if (dateA == null && dateB == null) return 0;
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA);
                });

                SwingUtilities.invokeLater(() -> {
                    filterAndUpdateTable();
                    if (sessionTable != null) {
                        sessionTable.packAll();
                    }
                });

            } catch (Exception e) {
                logger.error("Error loading SDK Debug sessions", e);
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

    private void resolveUserNames(List<String> userIds) {
        if (!userIds.isEmpty()) {
            String ids = userIds.stream().distinct().map(id -> "'" + id + "'").collect(Collectors.joining(","));
            String query = "SELECT id, username__sys FROM user__sys WHERE id CONTAINS (" + ids + ")";

            QueryResponse queryResponse = toolboxProject.getVaultClient()
                    .newRequest(com.veeva.vault.vapil.api.request.QueryRequest.class)
                    .query(query);

            if (toolboxProject.handleSessionExpiration(queryResponse)) {
                return;
            }

            if (queryResponse != null && !queryResponse.isFailure()) {
                for (QueryResponse.QueryResult result : queryResponse.getData()) {
                    userNames.put(result.getString("id"), result.getString("username__sys"));
                }
            }
        }
    }


    private String formatLogLevel(String level) {
        if (level == null) return "";
        switch (level) {
            case "all__sys": return "ALL";
            case "exceptions__sys": return "EXCEPTIONS";
            case "error__sys": return "ERROR";
            case "warning__sys": return "WARNING";
            case "info__sys": return "INFO";
            case "debug__sys": return "DEBUG";
            default: return level;
        }
    }

    private String formatStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case "active__sys": return "Active";
            case "inactive__sys": return "Inactive";
            default: return status;
        }
    }

    private void createSession() {
        CreateDebugSessionDialog dialog = new CreateDebugSessionDialog(toolboxProject);
        if (dialog.showAndGet()) {
            loadData();
        }
    }

    private void deleteSelectedVaultSessions() {
        List<DeveloperLogItem<SdkDebugSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the selected session(s) from Vault?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            for (DeveloperLogItem<SdkDebugSession> item : selectedSessions) {
                if (item.isInVault()) {
                    try {
                        VaultResponse response = toolboxProject.getVaultClient()
                                .newRequest(LogRequest.class)
                                .deleteDebugLog(item.getItem().getId());

                        if (toolboxProject.handleSessionExpiration(response)) {
                            break;
                        }

                        if (response == null || response.isFailure()) {
                            String errorMessage = response != null ? response.getResponseMessage() : "Unknown error";
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to delete session " + item.getItem().getId() + ": " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE));
                        }
                    } catch (Exception e) {
                        logger.error("Error deleting SDK Debug session " + item.getItem().getId(), e);
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "An error occurred while deleting session " + item.getItem().getId(), "Error", JOptionPane.ERROR_MESSAGE));
                    }
                }
            }
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Selected sessions deleted from Vault.", "Info", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            });
        }).start();
    }

    private void resetSelectedSessions() {
        List<DeveloperLogItem<SdkDebugSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to reset the selected session(s)? This will clear current log files in Vault.", "Confirm Reset", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            for (DeveloperLogItem<SdkDebugSession> item : selectedSessions) {
                if (item.isInVault()) {
                    try {
                        VaultResponse response = toolboxProject.getVaultClient()
                                .newRequest(LogRequest.class)
                                .resetDebugLog(item.getItem().getId());

                        if (toolboxProject.handleSessionExpiration(response)) {
                            break;
                        }

                        if (response == null || response.isFailure()) {
                            String errorMessage = response != null ? response.getResponseMessage() : "Unknown error";
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to reset session " + item.getItem().getId() + ": " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE));
                        }
                    } catch (Exception e) {
                        logger.error("Error resetting SDK Debug session " + item.getItem().getId(), e);
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "An error occurred while resetting session " + item.getItem().getId(), "Error", JOptionPane.ERROR_MESSAGE));
                    }
                }
            }
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Selected sessions reset.", "Info", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            });
        }).start();
    }

    @Override
    protected void downloadSelectedLogs() {
        List<DeveloperLogItem<SdkDebugSession>> selectedSessions = getSelectedItems();
        List<SdkDebugSession> toDownload = new ArrayList<>();
        boolean willOverwrite = false;

        for (DeveloperLogItem<SdkDebugSession> item : selectedSessions) {
            if (item.isInVault()) {
                toDownload.add(item.getItem());
                if (item.isLocal()) {
                    willOverwrite = true;
                }
            }
        }

        if (toDownload.isEmpty()) return;

        if (willOverwrite) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "One or more selected logs have already been downloaded.\nDo you want to re-download and overwrite the local copies with the latest Vault data?",
                    "Confirm Update", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        DownloadDebugLogTask task = new DownloadDebugLogTask(toolboxProject.getProject(), toDownload, () -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Successfully downloaded/updated " + toDownload.size() + " log(s).", "Download Complete", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            });
        });
        task.queue();
    }
    private void analyzeSelectedLogs() {
        List<DeveloperLogItem<SdkDebugSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/debug/" + toolboxProject.getVaultId());

        java.util.List<File> allTxtFilesToAnalyze = new java.util.ArrayList<>();
        java.util.List<String> selectedIds = new java.util.ArrayList<>();

        for (DeveloperLogItem<SdkDebugSession> item : selectedSessions) {
            if (item.isLocal()) {
                String baseFilename = item.getItem().getName() + "." + item.getItem().getId();
                File sessionDir = new File(vaultLogsDir, baseFilename);
                File zipFile = new File(sessionDir, baseFilename + ".zip");

                if (zipFile.exists()) {
                    try {
                        FileIO.unzipFiles(zipFile, sessionDir);
                    } catch (Exception e) {
                        logger.error("Failed to unzip debug log", e);
                    }
                }

                try {
                    List<File> extractedTxtFiles = FileIO.getFiles(sessionDir, ".txt");
                    if (extractedTxtFiles != null && !extractedTxtFiles.isEmpty()) {
                        allTxtFilesToAnalyze.addAll(extractedTxtFiles);
                        selectedIds.add(item.getItem().getId());
                    }
                } catch (Exception e) {
                    logger.error("Failed to find txt files in session directory", e);
                }
            }
        }

        if (!allTxtFilesToAnalyze.isEmpty()) {
            String logIdSuffix;
            if (selectedIds.size() == 1) {
                logIdSuffix = selectedIds.get(0);
            } else {
                logIdSuffix = "bulk_" + selectedIds.size() + "_sessions_" + com.veeva.vault.toolbox.core.utils.Date.getDateTimeAsFileName(java.time.ZonedDateTime.now());
            }

            AnalyzeLocalDebugLogTask task = new AnalyzeLocalDebugLogTask(
                    toolboxProject.getProject(),
                    allTxtFilesToAnalyze,
                    logIdSuffix
            );
            task.queue();

        } else {
            com.intellij.openapi.ui.Messages.showWarningDialog(toolboxProject.getProject(), "No log files found to analyze for selected sessions.", "Analysis Error");
        }
    }

    private void deleteSelectedLocalSessions() {
        List<DeveloperLogItem<SdkDebugSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete local files for the selected session(s)?", "Confirm Local Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/debug/" + toolboxProject.getVaultId());
        for (DeveloperLogItem<SdkDebugSession> item : selectedSessions) {
            if (item.isLocal()) {
                String baseFilename = item.getItem().getName() + "." + item.getItem().getId();
                File sessionDir = new File(vaultLogsDir, baseFilename);
                if (sessionDir.exists()) {
                    try {
                        org.apache.commons.io.FileUtils.deleteDirectory(sessionDir);
                    } catch (java.io.IOException e) {
                        logger.error("Failed to delete debug log directory: " + sessionDir.getName(), e);
                    }
                }
            }
        }

        com.intellij.openapi.vfs.VirtualFile vLogsDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                .refreshAndFindFileByIoFile(toolboxProject.getLogsDirectory());

        if (vLogsDir != null) {
            vLogsDir.refresh(false, true);
        }

        JOptionPane.showMessageDialog(this, "Selected local files deleted.", "Info", JOptionPane.INFORMATION_MESSAGE);
        loadData();
    }
}