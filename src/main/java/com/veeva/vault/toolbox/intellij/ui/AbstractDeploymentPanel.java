package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base panel for deployment-related UI components.
 * Provides a table-based interface for displaying and selecting items for deployment.
 *
 * @param <T> The type of item being deployed.
 */
public abstract class AbstractDeploymentPanel<T> extends JBPanel<AbstractDeploymentPanel<T>> {
    protected final ToolboxProject toolboxProject;
    protected JBTable deploymentTable;
    protected DefaultTableModel tableModel;
    protected final List<DeploymentItem<T>> allItems = new ArrayList<>();
    protected DefaultActionGroup actionGroup;

    private boolean allSelected = false;

    /**
     * Constructs an AbstractDeploymentPanel.
     *
     * @param toolboxProject The current toolbox project context.
     */
    public AbstractDeploymentPanel(ToolboxProject toolboxProject) {
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
                if ("Vault".equals(colName) || "Local".equals(colName) || "Loc".equals(colName) || "VPK".equals(colName) || 
                    "Components".equals(colName) || "SDK".equals(colName) || "WebSDK".equals(colName)) {
                    return Icon.class;
                }
                return super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        deploymentTable = new JBTable(tableModel);
        deploymentTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        deploymentTable.setAutoCreateRowSorter(true);

        final boolean[] isDragging = {false};
        final boolean[] dragSelectState = {false};
        final int[] dragStartRow = {-1};
        final Map<Integer, Boolean> dragOriginalStates = new HashMap<>();
        final Set<Integer> dragModifiedRows = new HashSet<>();

        deploymentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) return;
                int row = deploymentTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    int modelRow = deploymentTable.convertRowIndexToModel(row);
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

        deploymentTable.addMouseListener(new MouseAdapter() {
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
                int row = deploymentTable.rowAtPoint(e.getPoint());
                if (row < 0) return;
                int modelRow = deploymentTable.convertRowIndexToModel(row);
                if (!Boolean.TRUE.equals((Boolean) tableModel.getValueAt(modelRow, 0))) {
                    tableModel.setValueAt(true, modelRow, 0);
                }
                ActionPopupMenu popupMenu = ActionManager.getInstance()
                        .createActionPopupMenu("DeploymentContext", actionGroup);
                popupMenu.setTargetComponent(AbstractDeploymentPanel.this);
                popupMenu.getComponent().show(deploymentTable, e.getX(), e.getY());
            }
        });

        deploymentTable.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!isDragging[0] || dragStartRow[0] < 0) return;
                int currentRow = deploymentTable.rowAtPoint(e.getPoint());
                if (currentRow < 0) {
                    currentRow = e.getY() < 0 ? 0 : deploymentTable.getRowCount() - 1;
                }
                if (currentRow < 0) return;

                int rangeMin = Math.min(dragStartRow[0], currentRow);
                int rangeMax = Math.max(dragStartRow[0], currentRow);

                Set<Integer> toRestore = new HashSet<>(dragModifiedRows);
                for (int r = rangeMin; r <= rangeMax; r++) toRestore.remove(r);
                for (int r : toRestore) {
                    tableModel.setValueAt(dragOriginalStates.get(r), deploymentTable.convertRowIndexToModel(r), 0);
                    dragModifiedRows.remove(r);
                    dragOriginalStates.remove(r);
                }

                for (int r = rangeMin; r <= rangeMax; r++) {
                    int modelRow = deploymentTable.convertRowIndexToModel(r);
                    if (!dragModifiedRows.contains(r)) {
                        dragOriginalStates.put(r, (Boolean) tableModel.getValueAt(modelRow, 0));
                        dragModifiedRows.add(r);
                    }
                    tableModel.setValueAt(dragSelectState[0], modelRow, 0);
                }
            }
        });

        for (int i = 0; i < deploymentTable.getColumnCount(); i++) {
            TableColumn column = deploymentTable.getColumnModel().getColumn(i);
            String name = tableModel.getColumnName(i);
            if ("Select".equals(name) || "Vault".equals(name) || "Local".equals(name) || "Loc".equals(name) || "VPK".equals(name)) {
                column.setMaxWidth(50);
                column.setMinWidth(50);
                column.setPreferredWidth(50);
            }
        }

        setupColumnWidths(deploymentTable);

        JTableHeader header = deploymentTable.getTableHeader();
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

        JBScrollPane scrollPane = new JBScrollPane(deploymentTable);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        actionGroup = createActionGroup();
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("DeploymentToolbar", actionGroup, true);
        actionToolbar.setTargetComponent(this);

        add(actionToolbar.getComponent(), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Returns the column names for the deployment table.
     *
     * @return An array of column names.
     */
    protected abstract String[] getColumnNames();

    /**
     * Configures the column widths for the given table.
     *
     * @param table The table to configure.
     */
    protected abstract void setupColumnWidths(JBTable table);

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
     * Populates a row in the table model for the given item.
     *
     * @param item The item to populate the row with.
     */
    protected abstract void populateRow(DeploymentItem<T> item);

    /**
     * Filters and updates the table with the current items.
     */
    protected void filterAndUpdateTable() {
        tableModel.setRowCount(0);
        for (DeploymentItem<T> item : allItems) {
            populateRow(item);
        }
        TableUtils.autoResizeColumns(deploymentTable);
    }

    /**
     * Returns a list of items that are currently selected in the table.
     *
     * @return A list of selected DeploymentItems.
     */
    protected List<DeploymentItem<T>> getSelectedItems() {
        List<DeploymentItem<T>> selected = new ArrayList<>();
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
    protected Icon getVaultIcon(DeploymentItem<T> item) {
        return item.isInVault() ? AllIcons.Actions.Checked : null;
    }

    /**
     * Returns the icon representing the item's presence locally.
     *
     * @param item The item to check.
     * @return An Icon if the item is local, null otherwise.
     */
    protected Icon getLocalIcon(DeploymentItem<T> item) {
        return item.isLocal() ? AllIcons.FileTypes.Any_type : null;
    }
}
