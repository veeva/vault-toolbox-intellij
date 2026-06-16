package com.veeva.vault.toolbox.intellij.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.icons.AllIcons;
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
import com.intellij.ide.projectView.ProjectView;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.AnalyzeLocalApiLogTask;
import com.veeva.vault.toolbox.intellij.tasks.DownloadApiLogTask;
import com.veeva.vault.toolbox.core.models.SdkApiSession;
import com.veeva.vault.toolbox.intellij.ui.fileviewer.FileViewerDialog;
import org.apache.commons.io.FileUtils;
import org.jdesktop.swingx.JXTable;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel for managing and analyzing Vault API Usage logs.
 * Supports downloading remote logs, viewing local log files, and running analysis tasks.
 */
public class DeveloperApiSessionPanel extends AbstractDeveloperSessionPanel<SdkApiSession> {
    private static final Logger logger = LoggerFactory.getLogger(DeveloperApiSessionPanel.class);

    /**
     * Initializes the API session panel.
     *
     * @param toolboxProject The toolbox project context.
     */
    public DeveloperApiSessionPanel(ToolboxProject toolboxProject) {
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
                    DeveloperLogItem<SdkApiSession> item = allItems.get(sessionTable.convertRowIndexToModel(row));

                    if (item.isLocal()) {
                        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/api/" + getSelectedVaultId());
                        File dateDir = new File(vaultLogsDir, item.getItem().getLogDate());

                        if (dateDir.exists()) {
                            boolean isActionIcon = "View".equals(colName) || "Locate".equals(colName);

                            if ((isActionIcon && e.getClickCount() == 1) || (!isActionIcon && e.getClickCount() == 2)) {
                                if ("Locate".equals(colName)) {
                                    VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dateDir);
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
                                    new FileViewerDialog(toolboxProject.getProject(), dateDir).show();
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

    @Override
    protected String getLogTypeSubdir() {
        return "api";
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{"Select", "API Date", "View", "Locate"};
    }

    /**
     * Populates a row in the table model for the given API session item.
     *
     * @param item The item to populate the row with.
     */
    @Override
    protected void populateRow(DeveloperLogItem<SdkApiSession> item) {
        tableModel.addRow(new Object[]{
                false,
                item.getItem().getLogDate(),
                item.isLocal() ? AllIcons.Actions.Show : null,
                item.isLocal() ? AllIcons.General.Locate : null
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

        actionGroup.add(createImportAction(DeveloperLogsDialog.LogType.API_USAGE));

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
        new Thread(() -> {
            try {
                String selectedVaultId = getSelectedVaultId();
                Map<String, DeveloperLogItem<SdkApiSession>> itemMap = new HashMap<>();
                File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/api/" + selectedVaultId);

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

                if (isOnConnectedVault()) {
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
                }

                List<DeveloperLogItem<SdkApiSession>> newItems = new ArrayList<>(itemMap.values());
                newItems.sort((a, b) -> b.getItem().getLogDate().compareTo(a.getItem().getLogDate()));

                SwingUtilities.invokeLater(() -> {
                    allItems.clear();
                    allItems.addAll(newItems);
                    filterAndUpdateTable();

                });

            } catch (Exception e) {
                logger.error("Error loading API Usage sessions", e);
                SwingUtilities.invokeLater(() -> {
                    filterAndUpdateTable();

                    JOptionPane.showMessageDialog(this, "An error occurred while loading sessions.", "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    /**
     * Downloads the logs for the selected items in the session table.
     */
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

    /**
     * Initiates the analysis task for selected local API logs.
     */
    private void analyzeSelectedLogs() {
        List<DeveloperLogItem<SdkApiSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/api/" + getSelectedVaultId());
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

    /**
     * Deletes local log files for the selected sessions after user confirmation.
     */
    private void deleteSelectedLocalSessions() {
        List<DeveloperLogItem<SdkApiSession>> selectedSessions = getSelectedItems();
        if (selectedSessions.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete local files for " + selectedSessions.size() + " session(s)?", "Confirm Local Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        File vaultLogsDir = new File(toolboxProject.getLogsDirectory(), "/api/" + getSelectedVaultId());

        for (DeveloperLogItem<SdkApiSession> item : selectedSessions) {
            if (item.isLocal()) {
                File dateDir = new File(vaultLogsDir, item.getItem().getLogDate());
                if (dateDir.exists()) {
                    try {
                        FileUtils.deleteDirectory(dateDir);
                    } catch (IOException e) {
                        logger.error("Failed to delete log directory: {}", dateDir.getAbsolutePath(), e);
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
