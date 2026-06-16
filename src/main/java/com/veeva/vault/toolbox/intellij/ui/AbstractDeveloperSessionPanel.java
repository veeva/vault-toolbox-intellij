package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.veeva.vault.toolbox.core.logs.LogArchiveImporter;
import com.intellij.openapi.ui.Messages;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.ImportLogArchiveTask;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base panel for developer session-related UI components.
 * Provides a table-based interface for displaying and selecting developer logs or session items.
 *
 * @param <T> The type of item being managed in the session.
 */
public abstract class AbstractDeveloperSessionPanel<T> extends JBPanel<AbstractDeveloperSessionPanel<T>> {
    protected final ToolboxProject toolboxProject;
    protected JBTable sessionTable;
    protected DefaultTableModel tableModel;
    protected final List<DeveloperLogItem<T>> allItems = new ArrayList<>();
    protected DefaultActionGroup actionGroup;
    protected JComboBox<String> vaultSelector;

    private boolean allSelected = false;
    private boolean updatingVaultSelector = false;

    /**
     * Constructs an AbstractDeveloperSessionPanel.
     *
     * @param toolboxProject The current toolbox project context.
     */
    public AbstractDeveloperSessionPanel(ToolboxProject toolboxProject) {
        this.toolboxProject = toolboxProject;
        setLayout(new BorderLayout());
    }

    /**
     * Initializes the UI components, including the table and toolbar.
     */
    protected void initUI() {
        tableModel = new DefaultTableModel(getColumnNames(), 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class;
                }
                String colName = getColumnName(columnIndex);
                if ("Vault".equals(colName) || "Local".equals(colName) || "View".equals(colName) || "Locate".equals(colName)) {
                    return Icon.class;
                }
                return super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        sessionTable = new JBTable(tableModel);
        sessionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sessionTable.setAutoCreateRowSorter(true);

        final boolean[] isDragging = {false};
        final boolean[] dragSelectState = {false};
        final int[] dragStartRow = {-1};
        final Map<Integer, Boolean> dragOriginalStates = new HashMap<>();
        final Set<Integer> dragModifiedRows = new HashSet<>();

        sessionTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) return;
                int row = sessionTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    int modelRow = sessionTable.convertRowIndexToModel(row);
                    Boolean current = (Boolean) tableModel.getValueAt(modelRow, 0);
                    dragSelectState[0] = !Boolean.TRUE.equals(current);
                    dragStartRow[0] = row;
                    isDragging[0] = true;
                    dragOriginalStates.clear();
                    dragModifiedRows.clear();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging[0] = false;
                dragStartRow[0] = -1;
                dragOriginalStates.clear();
                dragModifiedRows.clear();
            }
        });

        sessionTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }

            /**
             * Ensures the right-clicked row is checked, then displays the action group as a context menu.
             *
             * @param e The mouse event that triggered the context menu.
             */
            private void showContextMenu(MouseEvent e) {
                int row = sessionTable.rowAtPoint(e.getPoint());
                if (row < 0) return;
                int modelRow = sessionTable.convertRowIndexToModel(row);
                if (!Boolean.TRUE.equals((Boolean) tableModel.getValueAt(modelRow, 0))) {
                    tableModel.setValueAt(true, modelRow, 0);
                }
                ActionPopupMenu popupMenu = ActionManager.getInstance()
                        .createActionPopupMenu("DeveloperSessionContext", actionGroup);
                popupMenu.setTargetComponent(AbstractDeveloperSessionPanel.this);
                popupMenu.getComponent().show(sessionTable, e.getX(), e.getY());
            }
        });

        sessionTable.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!isDragging[0] || dragStartRow[0] < 0) return;
                int currentRow = sessionTable.rowAtPoint(e.getPoint());
                if (currentRow < 0) {
                    currentRow = e.getY() < 0 ? 0 : sessionTable.getRowCount() - 1;
                }
                if (currentRow < 0) return;

                int rangeMin = Math.min(dragStartRow[0], currentRow);
                int rangeMax = Math.max(dragStartRow[0], currentRow);

                Set<Integer> toRestore = new HashSet<>(dragModifiedRows);
                for (int r = rangeMin; r <= rangeMax; r++) toRestore.remove(r);
                for (int r : toRestore) {
                    tableModel.setValueAt(dragOriginalStates.get(r), sessionTable.convertRowIndexToModel(r), 0);
                    dragModifiedRows.remove(r);
                    dragOriginalStates.remove(r);
                }

                for (int r = rangeMin; r <= rangeMax; r++) {
                    int modelRow = sessionTable.convertRowIndexToModel(r);
                    if (!dragModifiedRows.contains(r)) {
                        dragOriginalStates.put(r, (Boolean) tableModel.getValueAt(modelRow, 0));
                        dragModifiedRows.add(r);
                    }
                    tableModel.setValueAt(dragSelectState[0], modelRow, 0);
                }
            }
        });

        for (int i = 0; i < sessionTable.getColumnCount(); i++) {
            TableColumn column = sessionTable.getColumnModel().getColumn(i);
            String name = tableModel.getColumnName(i);
            if ("Select".equals(name) || "Vault".equals(name) || "Local".equals(name) || "View".equals(name) || "Locate".equals(name)) {
                column.setMaxWidth(50);
                column.setMinWidth(50);
                column.setPreferredWidth(50);
            }
        }

        JTableHeader header = sessionTable.getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = header.columnAtPoint(e.getPoint());
                if (col == 0) {
                    allSelected = !allSelected;
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        tableModel.setValueAt(allSelected, i, 0);
                    }
                }
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(sessionTable);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        vaultSelector = new JComboBox<>();
        refreshVaultSelector();
        vaultSelector.addActionListener(e -> {
            if (!updatingVaultSelector) {
                loadData();
            }
        });

        JPanel vaultRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        vaultRow.add(new JBLabel("Vault:"));
        vaultRow.add(vaultSelector);

        actionGroup = createActionGroup();
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("DeveloperSessionToolbar", actionGroup, true);
        actionToolbar.setTargetComponent(this);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(vaultRow, BorderLayout.WEST);
        northPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);

        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Returns the column names for the session table.
     *
     * @return An array of column names.
     */
    protected abstract String[] getColumnNames();

    /**
     * Returns the subdirectory name under the logs root for this log type (e.g. "api", "debug").
     */
    protected abstract String getLogTypeSubdir();

    /**
     * Creates the action group for the toolbar.
     *
     * @return The created DefaultActionGroup.
     */
    protected abstract DefaultActionGroup createActionGroup();

    /**
     * Loads the data to be displayed in the panel.
     */
    public abstract void loadData();

    /**
     * Downloads the logs for the selected items in the session table.
     */
    protected abstract void downloadSelectedLogs();

    /**
     * Populates a row in the table model for the given item.
     *
     * @param item The item to populate the row with.
     */
    protected abstract void populateRow(DeveloperLogItem<T> item);

    /**
     * Returns the vault ID currently selected in the vault selector, falling back to the connected vault.
     */
    protected String getSelectedVaultId() {
        String selected = (String) vaultSelector.getSelectedItem();
        return selected != null ? selected : String.valueOf(toolboxProject.getVaultId());
    }

    /**
     * Returns true when the selected vault matches the currently connected session's vault.
     */
    protected boolean isOnConnectedVault() {
        return String.valueOf(toolboxProject.getVaultId()).equals(getSelectedVaultId());
    }

    /**
     * Rescans the log type directory for vault ID subdirectories and refreshes the selector.
     * Preserves the current selection when possible; defaults to the connected vault otherwise.
     */
    public void refreshVaultSelector() {
        updatingVaultSelector = true;
        try {
            String connectedVaultId = String.valueOf(toolboxProject.getVaultId());
            String currentSelection = (String) vaultSelector.getSelectedItem();

            vaultSelector.removeAllItems();
            vaultSelector.addItem(connectedVaultId);

            File logTypeDir = new File(toolboxProject.getLogsDirectory(), getLogTypeSubdir());
            if (logTypeDir.exists()) {
                File[] subdirs = logTypeDir.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        String name = subdir.getName();
                        if (!name.equals(connectedVaultId)) {
                            vaultSelector.addItem(name);
                        }
                    }
                }
            }

            boolean restored = false;
            if (currentSelection != null) {
                for (int i = 0; i < vaultSelector.getItemCount(); i++) {
                    if (currentSelection.equals(vaultSelector.getItemAt(i))) {
                        vaultSelector.setSelectedItem(currentSelection);
                        restored = true;
                        break;
                    }
                }
            }
            if (!restored) {
                vaultSelector.setSelectedItem(connectedVaultId);
            }
        } finally {
            updatingVaultSelector = false;
        }
    }

    /**
     * Filters and updates the table with the current items.
     */
    protected void filterAndUpdateTable() {
        tableModel.setRowCount(0);
        for (DeveloperLogItem<T> item : allItems) {
            populateRow(item);
        }
        TableUtils.autoResizeColumns(sessionTable);
    }

    /**
     * Returns a list of items that are currently selected in the table.
     *
     * @return A list of selected DeveloperLogItems.
     */
    protected List<DeveloperLogItem<T>> getSelectedItems() {
        List<DeveloperLogItem<T>> selected = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
            if (Boolean.TRUE.equals(isSelected) && i < allItems.size()) {
                selected.add(allItems.get(i));
            }
        }
        return selected;
    }

    /**
     * Returns the icon representing the item's presence in Vault.
     *
     * @param item The item to check.
     * @return An Icon if the item is in Vault, null otherwise.
     */
    protected Icon getVaultIcon(DeveloperLogItem<T> item) {
        return item.isInVault() ? AllIcons.Actions.Checked : null;
    }

    /**
     * Returns the icon representing the item's presence locally.
     *
     * @param item The item to check.
     * @return An Icon if the item is local, null otherwise.
     */
    protected Icon getLocalIcon(DeveloperLogItem<T> item) {
        return item.isLocal() ? AllIcons.FileTypes.Any_type : null;
    }

    /**
     * Creates an AnAction for importing log archives.
     *
     * @param logType The log type to import.
     * @return The import AnAction.
     */
    protected AnAction createImportAction(DeveloperLogsDialog.LogType logType) {
        return new AnAction("Import Logs", "Import log archive from zip", AllIcons.Actions.Upload) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                        .withFileFilter(virtualFile -> {
                            String ext = virtualFile.getExtension();
                            return "zip".equalsIgnoreCase(ext) || "csv".equalsIgnoreCase(ext)
                                    || "log".equalsIgnoreCase(ext) || "txt".equalsIgnoreCase(ext);
                        });
                descriptor.setTitle("Select Log File");
                descriptor.setDescription("Select a Vault log archive (.zip) or a single log file (.csv, .log, .txt)");

                VirtualFile selectedFile = FileChooser.chooseFile(descriptor, toolboxProject.getProject(), null);
                if (selectedFile != null) {
                    File file = new File(selectedFile.getPath());
                    if ("zip".equalsIgnoreCase(selectedFile.getExtension())) {
                        String validationError = LogArchiveImporter.validateLogArchive(file);
                        if (validationError != null) {
                            Messages.showErrorDialog(toolboxProject.getProject(), validationError, "Invalid Archive");
                            return;
                        }
                    }
                    ImportLogArchiveDialog dialog = new ImportLogArchiveDialog(toolboxProject.getProject(), file, logType, toolboxProject.getLogsDirectory());
                    if (dialog.showAndGet()) {
                        String vaultId = dialog.getVaultId();
                        if (vaultId != null && !vaultId.isEmpty()) {
                            new ImportLogArchiveTask(toolboxProject.getProject(), file, vaultId, logType, () -> {
                                SwingUtilities.invokeLater(() -> {
                                    refreshVaultSelector();
                                    loadData();
                                });
                            }).queue();
                        }
                    }
                }
            }
        };
    }
}
