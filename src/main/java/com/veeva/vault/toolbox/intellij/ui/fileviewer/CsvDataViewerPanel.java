package com.veeva.vault.toolbox.intellij.ui.fileviewer;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.opencsv.CSVReader;
import com.veeva.vault.toolbox.intellij.settings.AppSettings;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileReader;

/**
 * A panel that displays CSV data in a read-only table.
 * The number of loaded rows is restricted by application settings.
 */
public class CsvDataViewerPanel extends JBPanel<CsvDataViewerPanel> {

    /**
     * Initializes the CSV data viewer panel.
     *
     * @param csvFile The CSV file to display.
     */
    public CsvDataViewerPanel(File csvFile) {
        setLayout(new BorderLayout());
        int maxRows = AppSettings.getInstance().getState().csvMaxRows;

        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] headers = reader.readNext();
            if (headers != null) {
                for (String header : headers) {
                    model.addColumn(header);
                }

                String[] nextLine;
                int count = 0;
                while ((nextLine = reader.readNext()) != null && count < maxRows) {
                    model.addRow(nextLine);
                    count++;
                }
            }
        } catch (Exception e) {
            add(new JBLabel("Error loading CSV: " + e.getMessage()), BorderLayout.NORTH);
        }

        JBTable table = new JBTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        JBScrollPane scrollPane = new JBScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JBLabel infoLabel = new JBLabel("Only the first " + maxRows + " values are loaded");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(infoLabel, BorderLayout.SOUTH);
    }
}
