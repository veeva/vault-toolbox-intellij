package com.veeva.vault.toolbox.intellij.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.Messages;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.DownloadProfilerLogTask;
import com.veeva.vault.vapil.api.model.common.SdkProfilingSession;
import com.veeva.vault.vapil.api.model.response.SdkProfilingSessionBulkResponse;
import com.veeva.vault.vapil.api.model.response.SdkProfilingSessionCreateResponse;
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

public class DeveloperProfilerSessionPanel extends AbstractDeveloperSessionPanel<SdkProfilingSession> {
    private static final Logger logger = LoggerFactory.getLogger(DeveloperProfilerSessionPanel.class);
    private Map<String, String> userNames = new HashMap<>();

    public DeveloperProfilerSessionPanel(ToolboxProject toolboxProject) {
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
                    DeveloperLogItem<SdkProfilingSession> item = allItems.get(sessionTable.convertRowIndexToModel(row));

                    if (item.isLocal()) {
                        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/profiler/" + toolboxProject.getVaultId());
                        String sessionFolderName = item.getItem().getName() + "." + item.getItem().getId();
                        File sessionDir = new File(vaultLogsDir, sessionFolderName);

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
        return new String[]{"Select", "Vault", "View", "Locate", "Name", "Label", "Status", "Description", "User ID", "User Name", "Created Date", "Expiration Date"};
    }

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
                getStatusLabel(session.getStatus()),
                session.getDescription(),
                userId,
                userName,
                session.getCreatedDate(),
                session.getExpirationDate()
        });
    }

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

    @Override
    public void loadData() {
        if (!toolboxProject.prepareRequest()) return;

        new Thread(() -> {
            try {
                allItems.clear();
                userNames.clear();
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
                        if (itemMap.containsKey(vSession.getId())) {
                            DeveloperLogItem<SdkProfilingSession> item = itemMap.get(vSession.getId());
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

    private void resolveUserNames(List<String> userIds) {
        if (!userIds.isEmpty()) {
            String ids = userIds.stream().distinct().map(id -> "'" + id + "'").collect(Collectors.joining(","));
            String query = "SELECT id, username__sys FROM user__sys WHERE id CONTAINS (" + ids + ")";

            com.veeva.vault.vapil.api.model.response.QueryResponse queryResponse = toolboxProject.getVaultClient()
                    .newRequest(com.veeva.vault.vapil.api.request.QueryRequest.class)
                    .query(query);

            if (queryResponse != null && !queryResponse.isFailure()) {
                for (com.veeva.vault.vapil.api.model.response.QueryResponse.QueryResult result : queryResponse.getData()) {
                    userNames.put(result.getString("id"), result.getString("username__sys"));
                }
            }
        }
    }

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

    private void deleteSelectedVaultSessions() {
        List<DeveloperLogItem<SdkProfilingSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the selected session(s) from Vault?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
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
                        org.apache.commons.io.FileUtils.deleteDirectory(sessionDir);
                    } catch (java.io.IOException e) {
                        logger.error("Failed to delete profiler log directory: " + sessionDir.getName(), e);
                    }
                }
            }
        }

        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            com.intellij.openapi.vfs.VirtualFile vVaultLogsDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                    .refreshAndFindFileByIoFile(vaultLogsDir);
            if (vVaultLogsDir != null) {
                vVaultLogsDir.refresh(true, true);
            }
        });

        JOptionPane.showMessageDialog(this, "Selected local files deleted.", "Info", JOptionPane.INFORMATION_MESSAGE);
        loadData();
    }
}