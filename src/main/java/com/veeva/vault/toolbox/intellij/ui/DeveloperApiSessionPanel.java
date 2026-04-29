package com.veeva.vault.toolbox.intellij.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.AnalyzeLocalApiLogTask;
import com.veeva.vault.toolbox.intellij.tasks.DownloadApiLogTask;
import com.veeva.vault.toolbox.core.models.SdkApiSession;
import icons.ToolboxIcons;
import org.jdesktop.swingx.JXTable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeveloperApiSessionPanel extends AbstractDeveloperSessionPanel<SdkApiSession> {
    private static final Logger logger = LoggerFactory.getLogger(DeveloperApiSessionPanel.class);

    public DeveloperApiSessionPanel(ToolboxProject toolboxProject) {
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
                    DeveloperLogItem<SdkApiSession> item = allItems.get(sessionTable.convertRowIndexToModel(row));

                    if (item.isLocal()) {
                        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/api/" + toolboxProject.getVaultId());
                        File dateDir = new File(vaultLogsDir, item.getItem().getLogDate());

                        if (dateDir.exists()) {
                            boolean isActionIcon = "View".equals(colName) || "Locate".equals(colName);

                            if ((isActionIcon && e.getClickCount() == 1) || (!isActionIcon && e.getClickCount() == 2)) {

                                if ("Locate".equals(colName)) {
                                    com.intellij.openapi.vfs.VirtualFile vFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dateDir);
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
                                    new com.veeva.vault.toolbox.intellij.ui.fileviewer.FileViewerDialog(toolboxProject.getProject(), dateDir).show();
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
        return new String[]{"Select", "API Date", "View", "Locate"};
    }

    @Override
    protected void populateRow(DeveloperLogItem<SdkApiSession> item) {
        tableModel.addRow(new Object[]{
                false, // 0: Select Checkbox
                item.getItem().getLogDate(), // 1: API Date
                item.isLocal() ? AllIcons.Actions.Show : null,   // 2: View Icon
                item.isLocal() ? AllIcons.General.Locate : null  // 3: Locate Icon
        });
    }

    @Override
    protected DefaultActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new AnAction("Download Log", "Download API usage logs for selected dates", AllIcons.Actions.Download) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                downloadSelectedLogs();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeveloperLogItem<SdkApiSession>> selected = getSelectedItems();
                boolean canDownload = selected.stream().anyMatch(item -> item.isInVault() && !item.isLocal());
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
                List<DeveloperLogItem<SdkApiSession>> selected = getSelectedItems();
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
                List<DeveloperLogItem<SdkApiSession>> selected = getSelectedItems();
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
        new Thread(() -> {
            try {
                allItems.clear();

                Map<String, DeveloperLogItem<SdkApiSession>> itemMap = new HashMap<>();

                File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/api/" + toolboxProject.getVaultId());

                if (vaultLogsDir.exists()) {
                    try {
                        List<File> jsonFiles = FileIO.getFiles(vaultLogsDir, ".json");

                        if (jsonFiles != null) {
                            ObjectMapper mapper = new ObjectMapper();
                            for (File jsonFile : jsonFiles) {
                                try {
                                    SdkApiSession session = mapper.readValue(jsonFile, SdkApiSession.class);

                                    session.setFileName(jsonFile.getParentFile().getName());

                                    itemMap.put(session.getLogDate(), new DeveloperLogItem<>(session, false, true));
                                } catch (Exception ex) {
                                    logger.error("Error reading JSON file: " + jsonFile.getName(), ex);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        logger.error("Error listing JSON files", ex);
                    }
                }

                LocalDate today = LocalDate.now(ZoneId.of("UTC"));
                for (int i = 0; i < 30; i++) {
                    String dateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(today.minusDays(i));

                    if (itemMap.containsKey(dateStr)) {
                        itemMap.get(dateStr).setInVault(true);
                    } else {
                        SdkApiSession vaultSession = new SdkApiSession();
                        vaultSession.setLogDate(dateStr);
                        itemMap.put(dateStr, new DeveloperLogItem<>(vaultSession, true, false));
                    }
                }

                allItems.addAll(itemMap.values());
                allItems.sort((a, b) -> b.getItem().getLogDate().compareTo(a.getItem().getLogDate()));

                SwingUtilities.invokeLater(() -> {
                    filterAndUpdateTable();
                    if (sessionTable != null) {
                        sessionTable.packAll();
                    }
                });

            } catch (Exception e) {
                logger.error("Error loading API Usage sessions", e);
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

    @Override
    protected void downloadSelectedLogs() {
        List<DeveloperLogItem<SdkApiSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        LocalDate minDate = null;
        LocalDate maxDate = null;

        for (DeveloperLogItem<SdkApiSession> item : selectedSessions) {
            if (item.isInVault() && !item.isLocal()) {
                LocalDate date = LocalDate.parse(item.getItem().getLogDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                if (minDate == null || date.isBefore(minDate)) minDate = date;
                if (maxDate == null || date.isAfter(maxDate)) maxDate = date;
            }
        }

        if (minDate != null && maxDate != null) {
            DownloadApiLogTask task = new DownloadApiLogTask(toolboxProject.getProject(), minDate, maxDate, () -> {
                SwingUtilities.invokeLater(this::loadData);
            });
            task.queue();
        }
    }

    private void analyzeSelectedLogs() {
        List<DeveloperLogItem<SdkApiSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/api/" + toolboxProject.getVaultId());
        List<File> csvFiles = new ArrayList<>();

        for (DeveloperLogItem<SdkApiSession> item : selectedSessions) {
            if (item.isLocal()) {
                File dateDir = new File(vaultLogsDir, item.getItem().getLogDate());

                if (dateDir.exists()) {
                    File[] foundCsvs = dateDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

                    if (foundCsvs != null && foundCsvs.length > 0) {
                        csvFiles.add(foundCsvs[0]);
                    }
                }
            }
        }

        if (csvFiles.isEmpty()) {
            Messages.showWarningDialog(toolboxProject.getProject(), "No CSV files found for the selected logs.", "Analysis Error");
            return;
        }

        AnalyzeLocalApiLogTask task = new AnalyzeLocalApiLogTask(toolboxProject.getProject(), csvFiles);
        task.queue();
    }

    private void deleteSelectedLocalSessions() {
        List<DeveloperLogItem<SdkApiSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete local files for " + selectedSessions.size() + " session(s)?", "Confirm Local Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/api/" + toolboxProject.getVaultId());

        for (DeveloperLogItem<SdkApiSession> item : selectedSessions) {
            if (item.isLocal()) {

                File dateDir = new File(vaultLogsDir, item.getItem().getLogDate());

                if (dateDir.exists()) {
                    try {
                        org.apache.commons.io.FileUtils.deleteDirectory(dateDir);
                    } catch (java.io.IOException e) {
                        logger.error("Failed to delete log directory: {}", dateDir.getAbsolutePath(), e);
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