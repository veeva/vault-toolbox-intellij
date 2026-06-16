package com.veeva.vault.toolbox.intellij.ui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;

public class TableUtils {
    /**
     * Resizes the columns of a table based on the content and header sizes.
     * Sets the minimum width to the header size, and the preferred width to the maximum
     * of the header size and the longest row content size.
     *
     * @param table The table to resize.
     */
    public static void autoResizeColumns(JTable table) {
        if (table == null) return;
        
        TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            TableColumn tableColumn = columnModel.getColumn(column);
            
            // Fixed width columns like "Select", "Vault", "Local" shouldn't be overridden if they have strict bounds
            String headerValue = tableColumn.getHeaderValue() != null ? tableColumn.getHeaderValue().toString() : "";
            
            // Calculate header width
            int headerWidth = 0;
            TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            if (headerRenderer != null) {
                Component headerComp = headerRenderer.getTableCellRendererComponent(
                        table, tableColumn.getHeaderValue(), false, false, 0, column);
                headerWidth = headerComp.getPreferredSize().width + 10;
            }
            
            int maxCellWidth = headerWidth;
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
                Component cellComp = table.prepareRenderer(cellRenderer, row, column);
                maxCellWidth = Math.max(maxCellWidth, cellComp.getPreferredSize().width + 10);
            }
            
            if (tableColumn.getMaxWidth() > 0 && tableColumn.getMaxWidth() < maxCellWidth) {
                tableColumn.setMaxWidth(maxCellWidth);
            }
            
            tableColumn.setMinWidth(headerWidth);
            tableColumn.setPreferredWidth(maxCellWidth);
        }
    }
}