package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.request.QueryRequest;
import org.jdesktop.swingx.JXTable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for selecting Vault custom classes to filter.
 * Fetches available classes from Vault and allows the user to select them via a table.
 */
public class ClassFilterDialog extends DialogWrapper {
    private final ToolboxProject toolboxProject;
    private JXTable table;
    private DefaultTableModel tableModel;

    /**
     * Constructs a ClassFilterDialog.
     *
     * @param toolboxProject The current toolbox project context.
     */
    public ClassFilterDialog(ToolboxProject toolboxProject) {
        super(toolboxProject.getProject(), true);
        this.toolboxProject = toolboxProject;
        init();
        setTitle("Select Class Filters");
        loadClasses();
    }

    /**
     * Creates the center panel of the dialog containing the class selection table.
     *
     * @return The center panel component.
     */
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(800, 400));

        String[] columnNames = {"Select", "Name", "Code Type"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class;
                }
                return super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        table = new JXTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSortable(true);

        TableColumn selectColumn = table.getColumnModel().getColumn(0);
        selectColumn.setMaxWidth(50);
        selectColumn.setMinWidth(50);
        selectColumn.setPreferredWidth(50);
        table.getColumnExt(0).setSortable(false);

        table.getColumnModel().getColumn(1).setPreferredWidth(500);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Asynchronously loads the list of classes from Vault.
     */
    private void loadClasses() {
        new Thread(() -> {
            if (toolboxProject.prepareRequest()) {
                QueryRequest request = toolboxProject.getVaultClient().newRequest(QueryRequest.class);
                QueryResponse response = request.query("SELECT component_name__v, component_type__v FROM vault_component__v WHERE component_name__v LIKE 'com.veeva.vault.custom%' ORDER BY component_name__v");

                if (response != null && !response.isFailure()) {
                    SwingUtilities.invokeLater(() -> {
                        tableModel.setRowCount(0);
                        for (QueryResponse.QueryResult result : response.getData()) {
                            tableModel.addRow(new Object[]{
                                    false,
                                    result.getString("component_name__v"),
                                    result.getString("component_type__v")
                            });
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Returns a list of the class names that were selected by the user.
     *
     * @return A list of selected class names.
     */
    public List<String> getSelectedClasses() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
            if (Boolean.TRUE.equals(isSelected)) {
                selected.add((String) tableModel.getValueAt(i, 1));
            }
        }
        return selected;
    }
}
