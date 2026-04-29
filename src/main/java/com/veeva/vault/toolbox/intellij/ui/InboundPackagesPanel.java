package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.request.QueryRequest;
import org.jdesktop.swingx.JXTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class InboundPackagesPanel extends AbstractDeploymentPanel<QueryResponse.QueryResult> {

    public InboundPackagesPanel(ToolboxProject toolboxProject) {
        super(toolboxProject);
        initUI();

        toolboxProject.addConnectionListener(new com.veeva.vault.toolbox.intellij.listeners.ConnectionListener() {
            @Override
            public void connected() {
                ApplicationManager.getApplication().invokeLater(() -> loadData());
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

        deploymentTable.setSortOrder(6, SortOrder.DESCENDING);

        deploymentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                // trigger action update
            }
        });

        java.awt.event.MouseAdapter localMouseAdapter = new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = deploymentTable.rowAtPoint(e.getPoint());
                int col = deploymentTable.columnAtPoint(e.getPoint());

                if (row >= 0 && col >= 0) {
                    String colName = deploymentTable.getColumnName(col);
                    DeploymentItem<QueryResponse.QueryResult> item = allItems.get(deploymentTable.convertRowIndexToModel(row));

                    if (item.isLocal()) {
                        String packageId = item.getItem().getString("id");
                        String packageName = item.getItem().getString("name__v");

                        if (packageId != null) {
                            java.io.File logDir = new java.io.File(toolboxProject.getLogsDirectory(), "deployment/" + packageName + "." + packageId);

                            if (logDir.exists()) {
                                boolean isActionIcon = "View".equals(colName) || "Locate".equals(colName);

                                if ((isActionIcon && e.getClickCount() == 1) || (!isActionIcon && e.getClickCount() == 2)) {

                                    if ("Locate".equals(colName)) {
                                        com.intellij.openapi.vfs.VirtualFile vFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(logDir);
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
                                        new com.veeva.vault.toolbox.intellij.ui.fileviewer.FileViewerDialog(toolboxProject.getProject(), logDir).show();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = deploymentTable.rowAtPoint(e.getPoint());
                int col = deploymentTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    String colName = deploymentTable.getColumnName(col);
                    if ("View".equals(colName) || "Locate".equals(colName)) {
                        Object value = deploymentTable.getValueAt(row, col);
                        if (value != null) {
                            deploymentTable.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                            return;
                        }
                    }
                }
                deploymentTable.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        };

        deploymentTable.addMouseListener(localMouseAdapter);
        deploymentTable.addMouseMotionListener(localMouseAdapter);
    }

    @Override
    protected String[] getColumnNames() {
        return new String[]{"Select", "View", "Locate", "Name", "Summary", "Description", "Deployment Status", "Modified Date"};
    }

    @Override
    protected void setupColumnWidths(JXTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        javax.swing.table.TableCellRenderer defaultIconRenderer = table.getDefaultRenderer(Icon.class);

        javax.swing.table.TableCellRenderer clickableIconRenderer = (tbl, value, isSelected, hasFocus, row, column) -> {
            java.awt.Component c = defaultIconRenderer.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
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

    @Override
    protected DefaultActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new AnAction("Download", "Download selected package", AllIcons.Actions.Download) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                List<DeploymentItem<QueryResponse.QueryResult>> selected = getSelectedItems();
                if (selected.isEmpty()) return;

                final java.util.concurrent.atomic.AtomicInteger downloadsRemaining = new java.util.concurrent.atomic.AtomicInteger(selected.size());


                Runnable onAllDownloadsComplete = () -> {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (toolboxProject != null) {
                            toolboxProject.refresh();
                        }
                        loadData();

                        com.intellij.openapi.ui.Messages.showInfoMessage(
                                toolboxProject.getProject(),
                                "Successfully downloaded deployment logs for " + selected.size() + " package(s).",
                                "Download Complete"
                        );
                    });
                };

                for (DeploymentItem<QueryResponse.QueryResult> item : selected) {
                    QueryResponse.QueryResult qr = item.getItem();
                    String packageId = qr.getString("id");
                    String packageName = qr.getString("name__v");
                    if (packageId != null) {
                        java.io.File logDir = new java.io.File(toolboxProject.getLogsDirectory(), "deployment/" + packageName + "." + packageId);
                        if (logDir.exists() && logDir.isDirectory() && logDir.list() != null && logDir.list().length > 0) {
                            if (downloadsRemaining.decrementAndGet() == 0) {
                                onAllDownloadsComplete.run();
                            }
                            continue;
                        }

                        ApplicationManager.getApplication().invokeLater(() -> {
                            com.veeva.vault.toolbox.intellij.tasks.DownloadDeploymentLogsTask task =
                                    new com.veeva.vault.toolbox.intellij.tasks.DownloadDeploymentLogsTask(toolboxProject.getProject(), packageId, packageName) {
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
                List<DeploymentItem<QueryResponse.QueryResult>> selected = getSelectedItems();
                e.getPresentation().setEnabled(!selected.isEmpty());
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
                                java.io.File logDir = new java.io.File(toolboxProject.getLogsDirectory(), "deployment/" + packageName + "." + packageId);
                                if (logDir.exists()) {
                                    try {
                                        org.apache.commons.io.FileUtils.deleteDirectory(logDir);
                                    } catch (java.io.IOException ex) {
                                        // Ignore
                                    }
                                }
                            }
                        }

                        com.intellij.openapi.vfs.VirtualFile vLogsDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                                .refreshAndFindFileByIoFile(toolboxProject.getLogsDirectory());

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

    @Override
    public void loadData() {
        allItems.clear();
        tableModel.setRowCount(0);

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
                    SwingUtilities.invokeLater(() -> filterAndUpdateTable());
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    if (response != null && !response.isFailure() && response.getData() != null) {
                        for (QueryResponse.QueryResult row : response.getData()) {
                            String packageId = row.getString("id");
                            String packageName = row.getString("name__v");
                            java.io.File logDir = new java.io.File(toolboxProject.getLogsDirectory(), "deployment/" + packageName + "." + packageId);
                            boolean isLocal = logDir.exists() && logDir.isDirectory() && logDir.list() != null && logDir.list().length > 0;
                            allItems.add(new DeploymentItem<>(row, true, isLocal));
                        }
                    } else if (response != null) {
                        String errorMsg = response.getResponseMessage();
                        if ((errorMsg == null || errorMsg.isEmpty()) && response.getErrors() != null && !response.getErrors().isEmpty()) {
                            errorMsg = response.getErrors().get(0).getMessage();
                        }

                        JOptionPane.showMessageDialog(this, "Error fetching inbound packages: " + errorMsg, "Query Error", JOptionPane.ERROR_MESSAGE);
                    }
                    filterAndUpdateTable();

                    if (deploymentTable != null) {
                        deploymentTable.packAll();
                    }
                });
            } catch (Exception e) {
                if (toolboxProject.handleSessionExpiration(e)) {
                    SwingUtilities.invokeLater(() -> filterAndUpdateTable());
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    filterAndUpdateTable();
                });
            }
        });
    }

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
                qr.getString("modified_date__v")
        });
    }

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