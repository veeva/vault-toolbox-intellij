package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Base panel for deployment-related UI components.
 * Provides a table-based interface for displaying and selecting items for deployment.
 *
 * @param <T> The type of item being deployed.
 */
public abstract class AbstractDeploymentPanel<T> extends JPanel {
    protected final ToolboxProject toolboxProject;
    protected JXTable deploymentTable;
    protected DefaultTableModel tableModel;
    protected final List<DeploymentItem<T>> allItems = new ArrayList<>();
    
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

        deploymentTable = new JXTable(tableModel);
        deploymentTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        for (int i = 0; i < deploymentTable.getColumnCount(); i++) {
            TableColumn column = deploymentTable.getColumnModel().getColumn(i);
            String name = tableModel.getColumnName(i);
            if ("Select".equals(name) || "Vault".equals(name) || "Local".equals(name) || "Loc".equals(name) || "VPK".equals(name)) {
                column.setMaxWidth(50);
                column.setMinWidth(50);
                column.setPreferredWidth(50);
                deploymentTable.getColumnExt(i).setSortable(false);
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

        JScrollPane scrollPane = new JScrollPane(deploymentTable);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        DefaultActionGroup actionGroup = createActionGroup();
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
    protected abstract void setupColumnWidths(JXTable table);

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
