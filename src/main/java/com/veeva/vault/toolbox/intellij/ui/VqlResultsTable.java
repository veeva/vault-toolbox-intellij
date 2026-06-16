package com.veeva.vault.toolbox.intellij.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.vapil.api.model.VaultModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Results grid for the VQL Console. Wraps a sortable {@link JBTable} and adds the
 * interactions a query console needs: a live quick-filter over loaded rows, copy
 * (selected rows or a single cell) to the clipboard, a double-click cell detail viewer
 * (useful for long or flattened multi-value cells), and "Open in Vault" for id cells.
 */
public class VqlResultsTable extends JPanel {

    private final DefaultTableModel model = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            if (!canEditInVault()) return false;
            String colName = columns.get(column);
            return !"id".equalsIgnoreCase(colName);
        }
    };
    
    private final java.util.Map<Integer, java.util.Map<String, String>> pendingEdits = new java.util.HashMap<>();
    private java.util.function.Consumer<Boolean> onDirtyStateChanged = null;

    /**
     * Checks if editing in Vault is permitted.
     *
     * @return true if permitted, false otherwise
     */
    private boolean canEditInVault() {
        return objectName != null && !objectName.isEmpty() && columns.contains("id");
    }

    /**
     * Checks if there are any pending edits.
     *
     * @return true if there are pending edits, false otherwise
     */
    public boolean hasPendingEdits() {
        return !pendingEdits.isEmpty();
    }

    /**
     * Retrieves the map of pending edits.
     *
     * @return the map of pending edits
     */
    public java.util.Map<Integer, java.util.Map<String, String>> getPendingEdits() {
        return pendingEdits;
    }

    /**
     * Stops the current cell editing, if any.
     */
    public void stopCellEditing() {
        if (table.isEditing() && table.getCellEditor() != null) {
            table.getCellEditor().stopCellEditing();
        }
    }

    /**
     * Commits pending edits by applying them to the original rows, effectively making the
     * edited values the new baseline.
     */
    public void commitPendingEdits() {
        for (java.util.Map.Entry<Integer, java.util.Map<String, String>> rowEdit : pendingEdits.entrySet()) {
            int modelRow = rowEdit.getKey();
            if (modelRow >= 0 && modelRow < originalRows.size()) {
                for (java.util.Map.Entry<String, String> colEdit : rowEdit.getValue().entrySet()) {
                    int colIdx = columns.indexOf(colEdit.getKey());
                    if (colIdx >= 0) {
                        originalRows.get(modelRow)[colIdx] = colEdit.getValue();
                    }
                }
            }
        }
        clearPendingEdits();
    }

    /**
     * Clears all pending edits and refreshes the table.
     */
    public void clearPendingEdits() {
        pendingEdits.clear();
        if (onDirtyStateChanged != null) {
            onDirtyStateChanged.accept(false);
        }
        table.repaint();
    }

    /**
     * Sets the listener for dirty state changes.
     *
     * @param listener the listener to set
     */
    public void setOnDirtyStateChanged(java.util.function.Consumer<Boolean> listener) {
        this.onDirtyStateChanged = listener;
    }

    private final JBTable table = new JBTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final SearchTextField filterField = new SearchTextField();
    private final JBLabel countLabel = new JBLabel();

    private static final ObjectMapper JSON = new ObjectMapper();

    private List<String> columns = new ArrayList<>();
    private List<? extends VaultModel> rawRows = new ArrayList<>();
    private List<String[]> originalRows = new ArrayList<>();
    private String objectName;
    private String baseUrl;
    private String timing = "";

    /**
     * Constructs a new VqlResultsTable.
     */
    public VqlResultsTable() {
        super(new BorderLayout());

        table.setRowSorter(sorter);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setCellSelectionEnabled(true);
        

        javax.swing.table.JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setFont(header.getFont().deriveFont(java.awt.Font.BOLD));
        }

        filterField.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                applyFilter();
            }
        });

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(JBUI.Borders.empty(2, 6));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.add(new JBLabel("Filter:"));
        left.add(filterField);
        top.add(left, BorderLayout.WEST);
        top.add(countLabel, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(new JBScrollPane(table), BorderLayout.CENTER);

        model.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (row >= 0 && col >= 0 && row < originalRows.size()) {
                    String colName = columns.get(col);
                    Object value = model.getValueAt(row, col);
                    String strVal = value != null ? value.toString() : "";
                    
                    String originalVal = originalRows.get(row)[col];
                    if (originalVal == null) originalVal = "";
                    
                    if (strVal.equals(originalVal)) {
                        if (pendingEdits.containsKey(row)) {
                            pendingEdits.get(row).remove(colName);
                            if (pendingEdits.get(row).isEmpty()) {
                                pendingEdits.remove(row);
                            }
                        }
                    } else {
                        pendingEdits.computeIfAbsent(row, k -> new java.util.HashMap<>()).put(colName, strVal);
                    }
                    
                    if (onDirtyStateChanged != null) {
                        onDirtyStateChanged.accept(!pendingEdits.isEmpty());
                    }
                }
            }
        });

        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                int modelCol = table.convertColumnIndexToModel(column);
                if (pendingEdits.containsKey(modelRow) && pendingEdits.get(modelRow).containsKey(columns.get(modelCol))) {
                    if (!isSelected) {
                        c.setBackground(com.intellij.ui.JBColor.namedColor("Table.modifiedItemBackground", new com.intellij.ui.JBColor(new Color(255, 250, 205), new Color(80, 70, 40))));
                        c.setForeground(com.intellij.ui.JBColor.namedColor("Table.modifiedItemForeground", com.intellij.ui.JBColor.foreground()));
                    }
                } else if (!isSelected) {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
                return c;
            }
        });

        installInteractions();
    }

    /**
     * Replaces the displayed data.
     *
     * @param columns    ordered column names
     * @param rows       row values flattened to strings (one array per row, same order as {@code rawRows})
     * @param rawRows    the original result rows, used by the cell viewer to show structured values
     * @param objectName the object queried in {@code FROM}, for "Open in Vault" (may be null)
     * @param baseUrl    the Vault base URL (e.g. {@code https://dns}), for "Open in Vault" (may be null)
     */
    public void setData(List<String> columns, List<String[]> rows, List<? extends VaultModel> rawRows,
                        String objectName, String baseUrl) {
        this.columns = columns;
        this.rawRows = rawRows;
        this.originalRows = new ArrayList<>(rows);
        this.objectName = objectName;
        this.baseUrl = baseUrl;

        clearPendingEdits();
        
        model.setColumnIdentifiers(columns.toArray());
        model.setRowCount(0);
        for (String[] row : rows) {
            model.addRow(row);
        }
        filterField.setText("");
        TableUtils.autoResizeColumns(table);
        applyFilter();
    }

    /**
     * Applies the current filter text to the table rows.
     */
    private void applyFilter() {
        String text = filterField.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        updateCount();
    }

    /** Sets the elapsed query time shown beside the row count, or clears it when negative.
     *
     * @param millis the query time in milliseconds
     */
    public void setQueryTime(long millis) {
        timing = millis >= 0 ? String.format("  ·  %.2fs", millis / 1000.0) : "";
        updateCount();
    }

    /**
     * Updates the row count label to reflect the current table state.
     */
    private void updateCount() {
        int shown = table.getRowCount();
        int loaded = model.getRowCount();
        String count = shown == loaded ? loaded + " rows" : shown + " / " + loaded + " shown";
        countLabel.setText(count + timing);
    }

    /**
     * Installs mouse and keyboard interactions for the table.
     */
    private void installInteractions() {

        table.getActionMap().put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelectedRows();
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }
        });
    }

    /**
     * Shows a popup menu if the given mouse event is a trigger.
     *
     * @param e the mouse event
     */
    private void maybeShowPopup(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int row = table.rowAtPoint(e.getPoint());
        int col = table.columnAtPoint(e.getPoint());
        if (row < 0) {
            return;
        }
        if (!table.isRowSelected(row)) {
            table.setRowSelectionInterval(row, row);
            if (col >= 0) {
                table.setColumnSelectionInterval(col, col);
            }
        }

        JPopupMenu menu = new JPopupMenu();
        if (col >= 0) {
            menu.add(new AbstractAction("Copy Cell") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    copyCell(row, col);
                }
            });
        }
        if (canOpenInVault()) {
            if (menu.getComponentCount() > 0) {
                menu.addSeparator();
            }
            menu.add(new AbstractAction("Open in Vault") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openInVault(row);
                }
            });
        }
        if (menu.getComponentCount() > 0) {
            menu.show(table, e.getX(), e.getY());
        }
    }

    /**
     * Copies the selected rows to the clipboard.
     */
    private void copySelectedRows() {
        int[] viewRows = table.getSelectedRows();
        if (viewRows.length == 0) {
            viewRows = new int[table.getRowCount()];
            for (int i = 0; i < viewRows.length; i++) {
                viewRows[i] = i;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.join("\t", columns)).append('\n');
        for (int viewRow : viewRows) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            for (int c = 0; c < columns.size(); c++) {
                if (c > 0) {
                    sb.append('\t');
                }
                Object value = model.getValueAt(modelRow, c);
                sb.append(value == null ? "" : value.toString());
            }
            sb.append('\n');
        }
        copyToClipboard(sb.toString());
    }

    /**
     * Copies the value of a specific cell to the clipboard.
     *
     * @param viewRow the row index in the view
     * @param viewCol the column index in the view
     */
    private void copyCell(int viewRow, int viewCol) {
        Object value = model.getValueAt(table.convertRowIndexToModel(viewRow), table.convertColumnIndexToModel(viewCol));
        copyToClipboard(value == null ? "" : value.toString());
    }

    /**
     * Copies the given text to the system clipboard.
     *
     * @param text the text to copy
     */
    private static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    /**
     * Checks if the query context supports opening records in Vault.
     *
     * @return true if supported, false otherwise
     */
    private boolean canOpenInVault() {
        return baseUrl != null && !baseUrl.isEmpty()
                && objectName != null && !objectName.isEmpty()
                && columns.contains("id");
    }

    /**
     * Opens the record corresponding to the specified row in Vault.
     *
     * @param viewRow the row index in the view
     */
    private void openInVault(int viewRow) {
        if (!canOpenInVault()) {
            return;
        }
        int idColumn = columns.indexOf("id");
        Object id = model.getValueAt(table.convertRowIndexToModel(viewRow), idColumn);
        if (id == null || id.toString().isEmpty()) {
            return;
        }
        BrowserUtil.browse(baseUrl + "/ui/#object/" + objectName + "/" + id);
    }
}
