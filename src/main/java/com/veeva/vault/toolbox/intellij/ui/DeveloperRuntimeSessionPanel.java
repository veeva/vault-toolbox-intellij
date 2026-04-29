package com.veeva.vault.toolbox.intellij.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.Messages;
import com.veeva.vault.toolbox.core.models.SdkApiSession;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.AnalyzeLocalApiLogTask;
import com.veeva.vault.toolbox.intellij.tasks.DownloadRuntimeLogTask;
import com.veeva.vault.toolbox.core.models.SdkRuntimeSession;
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

public class DeveloperRuntimeSessionPanel extends AbstractDeveloperSessionPanel<SdkRuntimeSession> {
    private static final Logger logger = LoggerFactory.getLogger(DeveloperRuntimeSessionPanel.class);

    public DeveloperRuntimeSessionPanel(ToolboxProject toolboxProject) {
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
                    DeveloperLogItem<SdkRuntimeSession> item = allItems.get(sessionTable.convertRowIndexToModel(row));

                    if (item.isLocal()) {
                        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/runtime/" + toolboxProject.getVaultId());
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
                        ((JComponent) c).setToolTipText("Double-click to open in File Viewer");
                    } else if ("Locate".equals(colName)) {
                        ((JComponent) c).setToolTipText("Double-click to locate in Project Tree");
                    }
                } else {
                    ((JComponent) c).setToolTipText(null);
                }
            }
            return c;
        };

        sessionTable.getColumnModel().getColumn(1).setCellRenderer(clickableIconRenderer);
        sessionTable.getColumnModel().getColumn(2).setCellRenderer(clickableIconRenderer);
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{"Select", "View", "Locate", "SDK Date"};
    }

    @Override
    protected DefaultActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new AnAction("Download Log", "Download Runtime usage logs for selected dates", AllIcons.Actions.Download) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                downloadSelectedLogs();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeveloperLogItem<SdkRuntimeSession>> selected = getSelectedItems();
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
                List<DeveloperLogItem<SdkRuntimeSession>> selected = getSelectedItems();
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
                List<DeveloperLogItem<SdkRuntimeSession>> selected = getSelectedItems();
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

                Map<String, DeveloperLogItem<SdkRuntimeSession>> itemMap = new HashMap<>();

                File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/runtime/" + toolboxProject.getVaultId());

                if (vaultLogsDir.exists()) {
                    try {
                        List<File> jsonFiles = com.veeva.vault.toolbox.core.utils.FileIO.getFiles(vaultLogsDir, ".json");

                        if (jsonFiles != null) {
                            ObjectMapper mapper = new ObjectMapper();
                            for (File jsonFile : jsonFiles) {
                                try {
                                    SdkRuntimeSession session = mapper.readValue(jsonFile, SdkRuntimeSession.class);
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
                        SdkRuntimeSession vaultSession = new SdkRuntimeSession();
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
                logger.error("Error loading SDK Runtime sessions", e);
                SwingUtilities.invokeLater(() -> {
                    filterAndUpdateTable();
                    if (sessionTable != null) {
                        sessionTable.packAll();
                    }
                });
            }
        }).start();
    }

    @Override
    protected void populateRow(DeveloperLogItem<SdkRuntimeSession> item) {
        tableModel.addRow(new Object[]{
                false,
                item.isLocal() ? AllIcons.Actions.Show : null,     // View Icon (Eye)
                item.isLocal() ? AllIcons.General.Locate : null,   // Locate Icon (Target)
                item.getItem().getLogDate()
        });
    }

    @Override
    protected void downloadSelectedLogs() {
        List<DeveloperLogItem<SdkRuntimeSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        LocalDate minDate = null;
        LocalDate maxDate = null;
        boolean requiresOverwriteConfirmation = false;

        for (DeveloperLogItem<SdkRuntimeSession> item : selectedSessions) {
            if (item.isInVault()) {
                if (item.isLocal()) {
                    requiresOverwriteConfirmation = true;
                }

                LocalDate date = LocalDate.parse(item.getItem().getLogDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                if (minDate == null || date.isBefore(minDate)) minDate = date;
                if (maxDate == null || date.isAfter(maxDate)) maxDate = date;
            }
        }

        if (requiresOverwriteConfirmation) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "One or more of the selected logs have already been downloaded.\nDo you want to re-download and overwrite the local files with the latest Vault data?",
                    "Confirm Re-download",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        if (minDate != null && maxDate != null) {
            DownloadRuntimeLogTask task = new DownloadRuntimeLogTask(toolboxProject.getProject(), minDate, maxDate, () -> {
                SwingUtilities.invokeLater(this::loadData);
            });
            task.queue();
        }
    }

    private void analyzeSelectedLogs() {
        List<DeveloperLogItem<SdkRuntimeSession>> selectedSessions = getSelectedItems();

        if (selectedSessions.isEmpty()) return;

        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/runtime/" + toolboxProject.getVaultId());
        List<File> allCsvFiles = new ArrayList<>();

        for (DeveloperLogItem<SdkRuntimeSession> item : selectedSessions) {
            if (item.isLocal()) {
                File dateDir = new File(vaultLogsDir, item.getItem().getLogDate());

                if (dateDir.exists()) {
                    try {
                        List<File> extractedCsvFiles = FileIO.getFiles(dateDir, ".csv");
                        if (extractedCsvFiles != null && !extractedCsvFiles.isEmpty()) {
                            allCsvFiles.addAll(extractedCsvFiles);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to find csv files in date directory: " + item.getItem().getLogDate(), e);
                    }
                }
            }
        }

        if (allCsvFiles.isEmpty()) {
            Messages.showWarningDialog(toolboxProject.getProject(), "No CSV files found for the selected logs.", "Analysis Error");
            return;
        }

        com.veeva.vault.toolbox.intellij.tasks.AnalyzeLocalRuntimeLogTask task =
                new com.veeva.vault.toolbox.intellij.tasks.AnalyzeLocalRuntimeLogTask(toolboxProject.getProject(), allCsvFiles);
        task.queue();
    }

    private void deleteSelectedLocalSessions() {
        List<DeveloperLogItem<SdkRuntimeSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete local files for " + selectedSessions.size() + " session(s)?", "Confirm Local Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/runtime/" + toolboxProject.getVaultId());

        for (DeveloperLogItem<SdkRuntimeSession> item : selectedSessions) {
            if (item.isLocal()) {
                File dateDir = new File(vaultLogsDir, item.getItem().getLogDate());

                if (dateDir.exists()) {
                    try {
                        org.apache.commons.io.FileUtils.deleteDirectory(dateDir);
                    } catch (java.io.IOException e) {
                        logger.error("Failed to delete log directory: " + dateDir.getAbsolutePath(), e);
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