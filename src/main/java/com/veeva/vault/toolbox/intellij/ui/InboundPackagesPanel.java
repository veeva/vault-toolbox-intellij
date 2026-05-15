package com.veeva.vault.toolbox.intellij.ui;

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
import com.veeva.vault.toolbox.core.utils.Date;
import com.veeva.vault.toolbox.intellij.listeners.ConnectionListener;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.DownloadDeploymentLogsTask;
import com.veeva.vault.toolbox.intellij.ui.fileviewer.FileViewerDialog;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.request.QueryRequest;
import org.apache.commons.io.FileUtils;
import org.jdesktop.swingx.JXTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Panel for managing inbound Vault packages retrieved from a Vault instance.
 * Supports viewing deployment logs, downloading remote logs, and status tracking.
 */
public class InboundPackagesPanel extends AbstractDeploymentPanel<QueryResponse.QueryResult> {

    /**
     * Initializes the inbound packages panel with the specified project context.
     *
     * @param toolboxProject The toolbox project context.
     */
    public InboundPackagesPanel(ToolboxProject toolboxProject) {
        super(toolboxProject);
        initUI();

        toolboxProject.addConnectionListener(new ConnectionListener() {
            @Override
            public void connected() {
                ApplicationManager.getApplication().invokeLater(InboundPackagesPanel.this::loadData);
            }

            @Override
            public void disconnected() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    allItems.clear();
                    tableModel.setRowCount(0);
                    filterAndUpdateTable();
                });
            }
        });

        deploymentTable.setSortOrder(7, SortOrder.DESCENDING);

        MouseAdapter localMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = deploymentTable.rowAtPoint(e.getPoint());
                int col = deploymentTable.columnAtPoint(e.getPoint());

                if (row >= 0 && col >= 0) {
                    String colName = deploymentTable.getColumnName(col);
                    DeploymentItem<QueryResponse.QueryResult> item = allItems.get(deploymentTable.convertRowIndexToModel(row));

                    if (item.isLocal()) {
                        String packageId = item.getItem().getString("id");
                        String packageName = item.getItem().getString("name__v");

                        if (packageId != null) {
                            File logDir = new File(toolboxProject.getLogsDirectory(), "deployment/" + packageName + "." + packageId);

                            if (logDir.exists()) {
                                boolean isActionIcon = "View".equals(colName) || "Locate".equals(colName);

                                if ((isActionIcon && e.getClickCount() == 1) || (!isActionIcon && e.getClickCount() == 2)) {
                                    if ("Locate".equals(colName)) {
                                        VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(logDir);
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
                                        new FileViewerDialog(toolboxProject.getProject(), logDir).show();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int row = deploymentTable.rowAtPoint(e.getPoint());
                int col = deploymentTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    String colName = deploymentTable.getColumnName(col);
                    if ("View".equals(colName) || "Locate".equals(colName)) {
                        Object value = deploymentTable.getValueAt(row, col);
                        if (value != null) {
                            deploymentTable.setCursor(new Cursor(Cursor.HAND_CURSOR));
                            return;
                        }
                    }
                }
                deploymentTable.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        };

        deploymentTable.addMouseListener(localMouseAdapter);
        deploymentTable.addMouseMotionListener(localMouseAdapter);
    }

    /**
     * Gets the column names for the inbound packages table.
     *
     * @return An array of column names.
     */
    @Override
    protected String[] getColumnNames() {
        return new String[]{"Select", "View", "Locate", "Name", "Summary", "Description", "Deployment Status", "Modified Date"};
    }

    /**
     * Configures column widths and cell renderers for the inbound packages table.
     *
     * @param table The table to configure.
     */
    @Override
    protected void setupColumnWidths(JXTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        TableCellRenderer defaultIconRenderer = table.getDefaultRenderer(Icon.class);

        TableCellRenderer clickableIconRenderer = (tbl, value, isSelected, hasFocus, row, column) -> {
            Component c = defaultIconRenderer.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
            if (c instanceof JComponent) {
                String colName = table.getColumnName(column);
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

        table.getColumnModel().getColumn(1).setCellRenderer(clickableIconRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(clickableIconRenderer);

        table.getColumnModel().getColumn(0).setMinWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setMinWidth(50);
        table.getColumnModel().getColumn(1).setMaxWidth(50);
        table.getColumnModel().getColumn(2).setMinWidth(50);
        table.getColumnModel().getColumn(2).setMaxWidth(50);
    }

    /**
     * Creates the action group for the toolbar.
     *
     * @return The created DefaultActionGroup.
     */
    @Override
    protected DefaultActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new AnAction("Download", "Download selected package logs", AllIcons.Actions.Download) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                List<DeploymentItem<QueryResponse.QueryResult>> selected = getSelectedItems();
                if (selected.isEmpty()) return;

                final AtomicInteger downloadsRemaining = new AtomicInteger(selected.size());

                Runnable onAllDownloadsComplete = () -> ApplicationManager.getApplication().invokeLater(() -> {
                    if (toolboxProject != null) {
                        toolboxProject.refresh();
                    }
                    loadData();
                    Messages.showInfoMessage(
                            toolboxProject.getProject(),
                            "Successfully downloaded deployment logs for " + selected.size() + " package(s).",
                            "Download Complete"
                    );
                });

                for (DeploymentItem<QueryResponse.QueryResult> item : selected) {
                    QueryResponse.QueryResult qr = item.getItem();
                    String packageId = qr.getString("id");
                    String packageName = qr.getString("name__v");
                    if (packageId != null) {
                        File logDir = new File(toolboxProject.getLogsDirectory(), "deployment/" + packageName + "." + packageId);
                        if (logDir.exists() && logDir.isDirectory() && logDir.list() != null && logDir.list().length > 0) {
                            if (downloadsRemaining.decrementAndGet() == 0) {
                                onAllDownloadsComplete.run();
                            }
                            continue;
                        }

                        ApplicationManager.getApplication().invokeLater(() -> {
                            DownloadDeploymentLogsTask task =
                                    new DownloadDeploymentLogsTask(toolboxProject.getProject(), packageId, packageName) {
                                        @Override
                                        public void onFinished() {
                                            super.onFinished();
                                            if (downloadsRemaining.decrementAndGet() == 0) {
                                                onAllDownloadsComplete.run();
                                            }
                                        }
                                    };
                            task.queue();
                        });
                    } else {
                        if (downloadsRemaining.decrementAndGet() == 0) {
                            onAllDownloadsComplete.run();
                        }
                    }
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(!getSelectedItems().isEmpty());
            }
        });

        actionGroup.add(new AnAction("Clear Local Logs", "Delete local logs for selected packages", AllIcons.Actions.Cancel) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                List<DeploymentItem<QueryResponse.QueryResult>> selected = getSelectedItems();
                if (!selected.isEmpty()) {
                    int confirm = JOptionPane.showConfirmDialog(InboundPackagesPanel.this,
                            "Are you sure you want to delete local logs for " + selected.size() + " package(s)?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        for (DeploymentItem<QueryResponse.QueryResult> item : selected) {
                            String packageId = item.getItem().getString("id");
                            String packageName = item.getItem().getString("name__v");
                            if (packageId != null) {
                                File logDir = new File(toolboxProject.getLogsDirectory(), "deployment/" + packageName + "." + packageId);
                                if (logDir.exists()) {
                                    try {
                                        FileUtils.deleteDirectory(logDir);
                                    } catch (IOException ignored) {}
                                }
                            }
                        }

                        VirtualFile vLogsDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(toolboxProject.getLogsDirectory());
                        if (vLogsDir != null) {
                            vLogsDir.refresh(false, true);
                        }

                        if (toolboxProject != null) {
                            toolboxProject.refresh();
                        }
                        loadData();
                    }
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeploymentItem<QueryResponse.QueryResult>> selected = getSelectedItems();
                boolean anyLocal = selected.stream().anyMatch(DeploymentItem::isLocal);
                e.getPresentation().setEnabled(!selected.isEmpty() && anyLocal);
            }
        });

        actionGroup.addSeparator();

        actionGroup.add(new AnAction("Refresh", "Refresh inbound packages", AllIcons.Actions.Refresh) {
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
        if (!toolboxProject.isConnected()) {
            if (!toolboxProject.connectWithDialog()) {
                filterAndUpdateTable();
                return;
            }
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String query = "SELECT id, name__v, summary__v, description__v, deployment_status__v, modified_date__v FROM vault_package__v ORDER BY modified_date__v DESC";
                QueryResponse response = toolboxProject.getVaultClient().newRequest(QueryRequest.class).query(query);

                if (toolboxProject.handleSessionExpiration(response)) {
                    SwingUtilities.invokeLater(() -> {
                        allItems.clear();
                        tableModel.setRowCount(0);
                        filterAndUpdateTable();
                    });
                    return;
                }

                List<DeploymentItem<QueryResponse.QueryResult>> newItems = new ArrayList<>();
                boolean isError = false;
                String errorMsg = "";

                if (response != null && !response.isFailure() && response.getData() != null) {
                    for (QueryResponse.QueryResult row : response.getData()) {
                        String packageId = row.getString("id");
                        String packageName = row.getString("name__v");
                        File logDir = new File(toolboxProject.getLogsDirectory(), "deployment/" + packageName + "." + packageId);
                        boolean isLocal = logDir.exists() && logDir.isDirectory() && logDir.list() != null && logDir.list().length > 0;
                        newItems.add(new DeploymentItem<>(row, true, isLocal));
                    }
                } else if (response != null) {
                    isError = true;
                    errorMsg = response.getResponseMessage();
                    if ((errorMsg == null || errorMsg.isEmpty()) && response.getErrors() != null && !response.getErrors().isEmpty()) {
                        errorMsg = response.getErrors().get(0).getMessage();
                    }
                }

                final boolean finalIsError = isError;
                final String finalErrorMsg = errorMsg;

                SwingUtilities.invokeLater(() -> {
                    allItems.clear();
                    tableModel.setRowCount(0);
                    allItems.addAll(newItems);
                    
                    if (finalIsError) {
                        JOptionPane.showMessageDialog(this, "Error fetching inbound packages: " + finalErrorMsg, "Query Error", JOptionPane.ERROR_MESSAGE);
                    }
                    filterAndUpdateTable();

                    if (deploymentTable != null) {
                        deploymentTable.packAll();
                    }
                });
            } catch (Exception e) {
                if (toolboxProject.handleSessionExpiration(e)) {
                    SwingUtilities.invokeLater(() -> {
                        allItems.clear();
                        tableModel.setRowCount(0);
                        filterAndUpdateTable();
                    });
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    allItems.clear();
                    tableModel.setRowCount(0);
                    filterAndUpdateTable();
                });
            }
        });
    }

    /**
     * Populates a row in the table model for the given inbound package item.
     *
     * @param item The item to populate the row with.
     */
    @Override
    protected void populateRow(DeploymentItem<QueryResponse.QueryResult> item) {
        QueryResponse.QueryResult qr = item.getItem();

        String deploymentStatus = "";
        if (qr.getListString("deployment_status__v") != null && !qr.getListString("deployment_status__v").isEmpty()) {
            deploymentStatus = qr.getListString("deployment_status__v").get(0);
        }

        tableModel.addRow(new Object[]{
                false,
                item.isLocal() ? AllIcons.Actions.Show : null,
                item.isLocal() ? AllIcons.General.Locate : null,
                qr.getString("name__v"),
                qr.getString("summary__v"),
                qr.getString("description__v"),
                formatStatus(deploymentStatus),
                Date.formatVaultDateTime(qr.getString("modified_date__v"))
        });
    }

    /**
     * Converts raw Vault status codes into user-friendly display labels.
     *
     * @param status The raw status code from the Vault API.
     * @return A human-readable status string.
     */
    private String formatStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case "imported__v": return "Imported";
            case "verified__v": return "Verified";
            case "not_verified__v": return "Not Verified";
            case "in_deploy__v": return "In Deployment";
            case "deployed__v": return "Deployed";
            case "deployed_with_warning__v": return "Deployed with warnings";
            case "not_supported__v": return "Not Supported";
            case "skipped__v": return "Skipped";
            case "error__v": return "Error";
            case "failed_with_partial_commit__v": return "Failed with Partial Commit";
            case "deployed_with_failures__v": return "Deployed with failures";
            case "deployed_with_error__v": return "Deployed with error";
            case "blocked__v": return "Blocked";
            case "queued__v": return "Queued";
            default: return status;
        }
    }
}
