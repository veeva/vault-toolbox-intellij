package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A dialog that shows a preview of files to be imported and lets the user confirm
 * or modify the Vault ID before importing a log archive.
 */
public class ImportLogArchiveDialog extends DialogWrapper {
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern SESSION_PATTERN = Pattern.compile("([^.]+)\\.([^.]+)");

    private final File archiveFile;
    private final DeveloperLogsDialog.LogType logType;
    private final File logsDirectory;

    private JBTextField vaultIdField;
    private DefaultTableModel previewModel;
    private JBLabel summaryLabel;

    public ImportLogArchiveDialog(@Nullable Project project, File archiveFile,
                                   DeveloperLogsDialog.LogType logType, File logsDirectory) {
        super(project);
        this.archiveFile = archiveFile;
        this.logType = logType;
        this.logsDirectory = logsDirectory;
        setTitle("Import Log Archive");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JBLabel("Archive File:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(new JBLabel(archiveFile.getName()), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        panel.add(new JBLabel("Vault ID:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        vaultIdField = new JBTextField();
        Matcher matcher = Pattern.compile("^(\\d+)").matcher(archiveFile.getName());
        if (matcher.find()) {
            vaultIdField.setText(matcher.group(1));
        }
        panel.add(vaultIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        summaryLabel = new JBLabel(" ");
        panel.add(summaryLabel, gbc);

        previewModel = new DefaultTableModel(new Object[]{"File", "Destination", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JBTable previewTable = new JBTable(previewModel);
        previewTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        previewTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        previewTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        previewTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected && "Overwrite".equals(previewModel.getValueAt(row, 2))) {
                    c.setForeground(JBColor.ORANGE);
                } else if (!isSelected) {
                    c.setForeground(table.getForeground());
                }
                return c;
            }
        });

        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        JBScrollPane scrollPane = new JBScrollPane(previewTable);
        scrollPane.setPreferredSize(new Dimension(520, 220));
        panel.add(scrollPane, gbc);

        vaultIdField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshPreview(); }
            public void removeUpdate(DocumentEvent e) { refreshPreview(); }
            public void changedUpdate(DocumentEvent e) { refreshPreview(); }
        });

        refreshPreview();

        return panel;
    }

    private void refreshPreview() {
        previewModel.setRowCount(0);
        String vaultId = vaultIdField != null ? vaultIdField.getText().trim() : "";
        if (vaultId.isEmpty()) {
            summaryLabel.setText(" ");
            return;
        }

        if (archiveFile.getName().toLowerCase().endsWith(".zip")) {
            refreshZipPreview(vaultId);
        } else {
            refreshSingleFilePreview(vaultId);
        }
    }

    private void refreshZipPreview(String vaultId) {
        int total = 0, overwrites = 0;
        try (ZipFile zip = new ZipFile(archiveFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String fileName = new File(entry.getName()).getName();
                String destination = computeRelativeDestination(fileName);
                boolean overwrite = destinationExists(vaultId, destination);
                previewModel.addRow(new Object[]{fileName, destination, overwrite ? "Overwrite" : "New"});
                total++;
                if (overwrite) overwrites++;
            }
        } catch (Exception e) {
            summaryLabel.setText("Unable to preview archive contents.");
            return;
        }
        setSummaryText(total, overwrites);
    }

    private void refreshSingleFilePreview(String vaultId) {
        String fileName = archiveFile.getName();
        String destination = computeRelativeDestination(fileName);
        boolean overwrite = destinationExists(vaultId, destination);
        previewModel.addRow(new Object[]{fileName, destination, overwrite ? "Overwrite" : "New"});
        setSummaryText(1, overwrite ? 1 : 0);
    }

    private void setSummaryText(int total, int overwrites) {
        if (overwrites > 0) {
            summaryLabel.setText(total + " file(s) to import, " + overwrites + " will be overwritten.");
        } else {
            summaryLabel.setText(total + " file(s) to import.");
        }
    }

    private boolean destinationExists(String vaultId, String destination) {
        if (logsDirectory == null) return false;
        return new File(logsDirectory, getBasePath() + vaultId + "/" + destination).exists();
    }

    private String getBasePath() {
        switch (logType) {
            case API_USAGE:  return "api/";
            case SDK_DEBUG:  return "debug/";
            case SDK_PROFILER: return "profiler/";
            case SDK_RUNTIME: return "runtime/";
            default: return "";
        }
    }

    private String computeRelativeDestination(String fileName) {
        if (logType == DeveloperLogsDialog.LogType.API_USAGE
                || logType == DeveloperLogsDialog.LogType.SDK_RUNTIME) {
            Matcher m = DATE_PATTERN.matcher(fileName);
            if (m.find()) {
                return m.group(1) + "/" + fileName;
            }
        } else {
            String baseName = archiveFile.getName();
            int dot = baseName.lastIndexOf('.');
            if (dot > 0) baseName = baseName.substring(0, dot);
            String sessionName = baseName;
            String sessionId = "imported";
            Matcher m = SESSION_PATTERN.matcher(baseName);
            if (m.find()) {
                sessionName = m.group(1);
                sessionId = m.group(2);
            }
            return sessionName + "." + sessionId + "/" + fileName;
        }
        return fileName;
    }

    public String getVaultId() {
        return vaultIdField.getText().trim();
    }
}
