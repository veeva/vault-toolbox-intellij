package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.project.Project;
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

/**
 * A dialog for viewing the contents of a CSV file in a read-only table format.
 * Loads a limited number of rows as specified in the application settings to ensure performance.
 */
public class CsvDataViewerDialog extends DialogWrapper {
    private final File csvFile;
    private final int maxRows;

    /**
     * Initializes the CSV data viewer dialog.
     *
     * @param csvFile The CSV file to display.
     */
    public CsvDataViewerDialog(Project project, File csvFile) {
        super(project, true);
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
        TableUtils.autoResizeColumns(table);
        
        JBScrollPane scrollPane = new JBScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JBLabel infoLabel = new JBLabel("Only the first " + maxRows + " values are loaded");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(infoLabel, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(800, 600));
        return panel;
    }

    /**
     * Configures the dialog actions, showing only the OK button for closing.
     *
     * @return The array of actions for the dialog.
     */
    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }
}
