package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ide.projectView.ProjectView;
import com.veeva.vault.toolbox.core.models.VpkBuildManifest;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.BuildVpkTask;
import com.veeva.vault.toolbox.intellij.tasks.DeployVpkTask;
import com.veeva.vault.toolbox.intellij.tasks.ValidateVpkTask;
import com.veeva.vault.toolbox.intellij.ui.fileviewer.FileViewerDialog;
import icons.ToolboxIcons;
import org.apache.commons.io.FileUtils;
import org.jdesktop.swingx.JXTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for managing Vault packages located in the local project directory.
 * Provides functionality for creating, editing, building, and deploying VPK packages.
 */
public class LocalPackagesPanel extends AbstractDeploymentPanel<File> {

    /**
     * Initializes the local packages panel with the specified project context.
     *
     * @param toolboxProject The toolbox project context.
     */
    public LocalPackagesPanel(ToolboxProject toolboxProject) {
        super(toolboxProject);
        initUI();

        MouseAdapter localMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = deploymentTable.rowAtPoint(e.getPoint());
                int col = deploymentTable.columnAtPoint(e.getPoint());

                if (row >= 0 && col >= 0) {
                    String colName = deploymentTable.getColumnName(col);
                    boolean isActionIcon = "VPK".equals(colName) || "Locate".equals(colName);

                    if ((isActionIcon && e.getClickCount() == 1) || (!isActionIcon && e.getClickCount() == 2)) {
                        DeploymentItem<File> item = allItems.get(deploymentTable.convertRowIndexToModel(row));
                        File manifestFile = item.getItem();

                        File packageDir = manifestFile.getParentFile();
                        String vpkFileName = manifestFile.getName().replace(".json", ".vpk");
                        File vpkFile = new File(packageDir, vpkFileName);

                        if (vpkFile.exists()) {
                            if ("Locate".equals(colName)) {
                                VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(packageDir);
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
                                new FileViewerDialog(toolboxProject.getProject(), vpkFile).show();
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
                    if ("VPK".equals(colName) || "Locate".equals(colName)) {
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
     * Gets the column names for the local packages table.
     *
     * @return An array of column names.
     */
    @Override
    protected String[] getColumnNames() {
        return new String[]{"Select", "VPK", "Locate", "Name", "Summary", "Description", "Components", "Java SDK", "Web SDK"};
    }

    /**
     * Configures column widths and cell renderers for the local packages table.
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
                    if ("VPK".equals(colName)) {
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
        table.getColumnModel().getColumn(6).setCellRenderer(defaultIconRenderer);
        table.getColumnModel().getColumn(7).setCellRenderer(defaultIconRenderer);
        table.getColumnModel().getColumn(8).setCellRenderer(defaultIconRenderer);

        table.getColumnModel().getColumn(0).setMinWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setMinWidth(50);
        table.getColumnModel().getColumn(1).setMaxWidth(50);
        table.getColumnModel().getColumn(2).setMinWidth(50);
        table.getColumnModel().getColumn(2).setMaxWidth(50);
    }

    /**
     * Packs the table columns while maintaining reasonable maximum width limits.
     */
    private void packTableWithLimits() {
        if (deploymentTable != null) {
            deploymentTable.getColumnModel().getColumn(3).setMaxWidth(300);
            deploymentTable.getColumnModel().getColumn(4).setMaxWidth(400);
            deploymentTable.getColumnModel().getColumn(5).setMaxWidth(500);
            deploymentTable.getColumnModel().getColumn(7).setMaxWidth(300);

            deploymentTable.packAll();

            deploymentTable.getColumnModel().getColumn(3).setMaxWidth(Integer.MAX_VALUE);
            deploymentTable.getColumnModel().getColumn(4).setMaxWidth(Integer.MAX_VALUE);
            deploymentTable.getColumnModel().getColumn(5).setMaxWidth(Integer.MAX_VALUE);
            deploymentTable.getColumnModel().getColumn(7).setMaxWidth(Integer.MAX_VALUE);
        }
    }

    /**
     * Creates the action group for the toolbar.
     *
     * @return The created DefaultActionGroup.
     */
    @Override
    protected DefaultActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new AnAction("Create", "Create new package", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                DesignPackageDialog dialog = new DesignPackageDialog(toolboxProject, null);
                dialog.show();
                loadData();
            }
        });

        actionGroup.add(new AnAction("Edit", "Edit selected package", AllIcons.Actions.Edit) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                List<DeploymentItem<File>> selected = getSelectedItems();
                if (selected.size() == 1) {
                    DesignPackageDialog dialog = new DesignPackageDialog(toolboxProject, selected.get(0).getItem());
                    dialog.show();
                    loadData();
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeploymentItem<File>> selected = getSelectedItems();
                e.getPresentation().setEnabled(selected.size() == 1 && selected.get(0).getItem().getName().endsWith(".json"));
            }
        });

        actionGroup.add(new AnAction("Make a Copy", "Make a copy of the selected package", AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                List<DeploymentItem<File>> selected = getSelectedItems();
                if (selected.size() == 1) {
                    File originalFile = selected.get(0).getItem();
                    VpkBuildManifest manifest = VpkBuildManifest.load(originalFile);
                    if (manifest != null) {
                        String newName = manifest.getName() != null ? manifest.getName() + "-COPY" : "COPY";
                        manifest.setName(newName);

                        File projectDir = new File(toolboxProject.getVpkDirectory(), newName);
                        File packagesDir = new File(projectDir, "packages");
                        packagesDir.mkdirs();

                        String newFileName = originalFile.getName().replace(".json", "-COPY.json");
                        File newFile = new File(packagesDir, newFileName);
                        manifest.save(newFile);

                        DesignPackageDialog dialog = new DesignPackageDialog(toolboxProject, newFile);
                        dialog.show();
                        loadData();
                    }
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeploymentItem<File>> selected = getSelectedItems();
                e.getPresentation().setEnabled(selected.size() == 1 && selected.get(0).getItem().getName().endsWith(".json"));
            }
        });

        actionGroup.add(new AnAction("Delete", "Delete selected package", AllIcons.Actions.GC) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                List<DeploymentItem<File>> selected = getSelectedItems();
                if (!selected.isEmpty()) {
                    int confirm = JOptionPane.showConfirmDialog(LocalPackagesPanel.this, "Are you sure you want to delete " + selected.size() + " package(s)?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        for (DeploymentItem<File> item : selected) {
                            File jsonFile = item.getItem();
                            File packagesDir = jsonFile.getParentFile();
                            File projectDir = packagesDir.getParentFile();

                            try {
                                FileUtils.deleteDirectory(projectDir);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }

                        VirtualFile vLogsDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(toolboxProject.getVpkDirectory());
                        if (vLogsDir != null) {
                            vLogsDir.refresh(false, true);
                        }

                        loadData();

                        if (toolboxProject != null) {
                            toolboxProject.refresh();
                        }
                    }
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(!getSelectedItems().isEmpty());
            }
        });

        actionGroup.addSeparator();

        actionGroup.add(new AnAction("Build", "Build selected package", ToolboxIcons.Hammer) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                List<DeploymentItem<File>> selected = getSelectedItems();
                if (selected.size() == 1) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        VirtualFile virtualFile = VfsUtil.findFileByIoFile(selected.get(0).getItem(), true);
                        BuildVpkTask task = new BuildVpkTask(toolboxProject.getProject(), virtualFile, false, () -> loadData());
                        task.queue();
                    });
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeploymentItem<File>> selected = getSelectedItems();
                e.getPresentation().setEnabled(selected.size() == 1 && selected.get(0).getItem().getName().endsWith(".json"));
            }
        });

        actionGroup.add(new AnAction("Validate", "Validate selected package", ToolboxIcons.Check) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (!toolboxProject.isConnected()) {
                    if (!toolboxProject.connectWithDialog()) {
                        return;
                    }
                }

                List<DeploymentItem<File>> selected = getSelectedItems();
                if (selected.size() == 1) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        String vpkFileName = selected.get(0).getItem().getName().replace(".json", ".vpk");
                        File vpkFile = new File(selected.get(0).getItem().getParentFile(), vpkFileName);
                        VirtualFile virtualFile = VfsUtil.findFileByIoFile(vpkFile, true);

                        ValidateVpkTask task = new ValidateVpkTask(toolboxProject.getProject(), virtualFile);
                        task.queue();
                    });
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeploymentItem<File>> selected = getSelectedItems();
                if (selected.size() == 1 && selected.get(0).getItem().getName().endsWith(".json")) {
                    String vpkFileName = selected.get(0).getItem().getName().replace(".json", ".vpk");
                    File vpkFile = new File(selected.get(0).getItem().getParentFile(), vpkFileName);
                    e.getPresentation().setEnabled(vpkFile.exists());
                } else {
                    e.getPresentation().setEnabled(false);
                }
            }
        });

        actionGroup.add(new AnAction("Deploy", "Deploy selected package", ToolboxIcons.Upload) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (!toolboxProject.isConnected()) {
                    if (!toolboxProject.connectWithDialog()) {
                        return;
                    }
                }

                List<DeploymentItem<File>> selected = getSelectedItems();
                if (selected.size() == 1) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        String vpkFileName = selected.get(0).getItem().getName().replace(".json", ".vpk");
                        File vpkFile = new File(selected.get(0).getItem().getParentFile(), vpkFileName);
                        VirtualFile virtualFile = VfsUtil.findFileByIoFile(vpkFile, true);
                        DeployVpkTask task = new DeployVpkTask(toolboxProject.getProject(), virtualFile);
                        task.queue();
                    });
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeploymentItem<File>> selected = getSelectedItems();
                if (selected.size() == 1 && selected.get(0).getItem().getName().endsWith(".json")) {
                    String vpkFileName = selected.get(0).getItem().getName().replace(".json", ".vpk");
                    File vpkFile = new File(selected.get(0).getItem().getParentFile(), vpkFileName);
                    e.getPresentation().setEnabled(vpkFile.exists());
                } else {
                    e.getPresentation().setEnabled(false);
                }
            }
        });

        actionGroup.add(new AnAction("Build and Deploy", "Build and deploy selected package", ToolboxIcons.DoubleRight) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (!toolboxProject.isConnected()) {
                    if (!toolboxProject.connectWithDialog()) {
                        return;
                    }
                }

                List<DeploymentItem<File>> selected = getSelectedItems();
                if (selected.size() == 1) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        VirtualFile virtualFile = VfsUtil.findFileByIoFile(selected.get(0).getItem(), true);
                        BuildVpkTask task = new BuildVpkTask(toolboxProject.getProject(), virtualFile, true, () -> loadData());
                        task.queue();
                    });
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                List<DeploymentItem<File>> selected = getSelectedItems();
                e.getPresentation().setEnabled(selected.size() == 1 && selected.get(0).getItem().getName().endsWith(".json"));
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
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<DeploymentItem<File>> newItems = new ArrayList<>();
            File vpkRoot = toolboxProject.getVpkDirectory();

            if (vpkRoot.exists() && vpkRoot.isDirectory()) {
                File[] projectDirs = vpkRoot.listFiles(File::isDirectory);
                if (projectDirs != null) {
                    for (File projectDir : projectDirs) {
                        File packagesDir = new File(projectDir, "packages");
                        if (packagesDir.exists() && packagesDir.isDirectory()) {
                            File[] files = packagesDir.listFiles((dir, name) -> name.endsWith(".json"));
                            if (files != null) {
                                for (File file : files) {
                                    newItems.add(new DeploymentItem<>(file, false, true));
                                }
                            }
                        }
                    }
                }
            }

            SwingUtilities.invokeLater(() -> {
                allItems.clear();
                tableModel.setRowCount(0);
                allItems.addAll(newItems);
                filterAndUpdateTable();
                packTableWithLimits();
            });
        });
    }

    /**
     * Populates a row in the table model for the given local package item.
     *
     * @param item The item to populate the row with.
     */
    @Override
    protected void populateRow(DeploymentItem<File> item) {
        File file = item.getItem();
        String name = file.getName();
        String summary = "";
        String description = "";
        boolean hasComponents = false;
        boolean hasSdk = false;
        boolean hasWebSdk = false;

        String vpkFileName = file.getName().replace(".json", ".vpk");
        File vpkFile = new File(file.getParentFile(), vpkFileName);
        Icon vpkIcon = vpkFile.exists() ? ToolboxIcons.Vpk : null;
        Icon locateIcon = vpkFile.exists() ? AllIcons.General.Locate : null;

        if (file.getName().endsWith(".json")) {
            VpkBuildManifest manifest = VpkBuildManifest.load(file);
            if (manifest != null) {
                name = manifest.getName() != null && !manifest.getName().isEmpty() ? manifest.getName() : file.getName();
                summary = manifest.getSummary() != null ? manifest.getSummary() : "";
                description = manifest.getDescription() != null ? manifest.getDescription() : "";
                hasComponents = manifest.getComponents() != null && !manifest.getComponents().isEmpty();
                hasSdk = manifest.getJavaSdk() != null;
                hasWebSdk = manifest.getWebSdk() != null;
            }
        }

        tableModel.addRow(new Object[]{
                false,
                vpkIcon,
                locateIcon,
                name,
                summary,
                description,
                hasComponents ? AllIcons.Actions.Checked : null,
                hasSdk ? AllIcons.Actions.Checked : null,
                hasWebSdk ? AllIcons.Actions.Checked : null
        });
    }
}
