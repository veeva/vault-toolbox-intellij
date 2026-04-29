package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.opencsv.CSVReader;
import com.veeva.vault.toolbox.intellij.settings.AppSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileReader;

public class CsvDataViewerDialog extends DialogWrapper {
    private final File csvFile;
    private final int maxRows;

    public CsvDataViewerDialog(File csvFile) {
        super(true);
        this.csvFile = csvFile;
        this.maxRows = AppSettings.getInstance().getState().csvMaxRows;
        init();
        setTitle("CSV Data Viewer: " + csvFile.getName());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
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
            panel.add(new JBLabel("Error loading CSV: " + e.getMessage()), BorderLayout.NORTH);
        }

        JBTable table = new JBTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        JBScrollPane scrollPane = new JBScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JBLabel infoLabel = new JBLabel("Only the first " + maxRows + " values are loaded");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(infoLabel, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(800, 600));
        return panel;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }
}
