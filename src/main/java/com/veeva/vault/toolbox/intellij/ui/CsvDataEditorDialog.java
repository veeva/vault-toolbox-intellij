package com.veeva.vault.toolbox.intellij.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.veeva.vault.toolbox.core.models.CsvDataStep;
import com.veeva.vault.toolbox.core.models.CsvManifest;
import com.veeva.vault.toolbox.core.utils.Checksum;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CsvDataEditorDialog extends DialogWrapper {
    private final ToolboxProject toolboxProject;
    private final File csvFile;
    private final File xmlFile;
    private CsvManifest csvManifest;

    private JTextField labelField;
    private JCheckBox stepRequiredCheckbox;
    private JTextField checksumField;
    private JTextField objectField;
    private JTextField idParamField;
    private JComboBox<String> dataTypeField;
    private JComboBox<String> actionField;
    private JCheckBox recordMigrationModeCheckbox;
    private JTextField recordCountField;
    private JButton objectSearchButton;
    private JButton idParamSearchButton;

    public CsvDataEditorDialog(ToolboxProject toolboxProject, File csvFile) {
        super(toolboxProject.getProject());
        this.toolboxProject = toolboxProject;
        this.csvFile = csvFile;
        String xmlPath = csvFile.getAbsolutePath().substring(0, csvFile.getAbsolutePath().lastIndexOf(".")) + ".xml";
        this.xmlFile = new File(xmlPath);
        
        loadManifest();
        validateDataMatch();
        init();
        setTitle("CSV Data Editor: " + csvFile.getName());
    }

    private void validateDataMatch() {
        if (xmlFile.exists() && csvFile.exists()) {
            String calculatedMd5 = Checksum.getMd5(csvFile);
            int calculatedRowCount = FileIO.getCsvRowCount(csvFile) - 1;

            String manifestMd5 = csvManifest.getChecksum();
            Integer manifestRowCount = csvManifest.getCsvDataStep().getRecordCount();

            if (!calculatedMd5.equals(manifestMd5) || (manifestRowCount != null && !manifestRowCount.equals(calculatedRowCount))) {
                int exitCode = Messages.showYesNoDialog(
                        toolboxProject.getProject(),
                        "The data manifest does not match the CSV data. Do you want to update the manifest with the current data values?",
                        "Data Mismatch",
                        Messages.getQuestionIcon()
                );

                if (exitCode == Messages.YES) {
                    csvManifest.setChecksum(calculatedMd5);
                    csvManifest.getCsvDataStep().setRecordCount(calculatedRowCount);
                }
            }
        }
    }

    private void loadManifest() {
        if (xmlFile.exists()) {
            try {
                String xmlContent = new String(Files.readAllBytes(xmlFile.toPath()), StandardCharsets.UTF_8);
                ObjectMapper objectMapper = new XmlMapper();
                csvManifest = objectMapper.readValue(xmlContent, CsvManifest.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (csvManifest == null) {
            csvManifest = new CsvManifest();
            csvManifest.setLabel(csvFile.getName());
            csvManifest.setCsvDataStep(new CsvDataStep());
            
            if (csvFile.exists()) {
                csvManifest.setChecksum(Checksum.getMd5(csvFile));
                csvManifest.getCsvDataStep().setRecordCount(FileIO.getCsvRowCount(csvFile) - 1);
            }
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);

        int row = 0;

        // Label
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Label:"), gbc);
        gbc.gridx = 1;
        labelField = new JTextField(csvManifest.getLabel());
        panel.add(labelField, gbc);
        row++;

        // Step Required
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Step Required:"), gbc);
        gbc.gridx = 1;
        stepRequiredCheckbox = new JCheckBox("", csvManifest.getStepRequired() != null && csvManifest.getStepRequired());
        panel.add(stepRequiredCheckbox, gbc);
        row++;

        // Data Step Header separator
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(new JSeparator(JSeparator.HORIZONTAL), gbc);
        row++;
        gbc.gridwidth = 1;

        CsvDataStep dataStep = csvManifest.getCsvDataStep();
        if (dataStep == null) {
            dataStep = new CsvDataStep();
            csvManifest.setCsvDataStep(dataStep);
        }

        // Data Type
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Data Type:"), gbc);
        gbc.gridx = 1;
        dataTypeField = new JComboBox<>(new String[]{"", "Object", "Groups"});
        dataTypeField.setSelectedItem(csvManifest.getCsvDataStep().getDataType() != null ? csvManifest.getCsvDataStep().getDataType() : "");
        dataTypeField.addActionListener(e -> {
            String selectedType = (String) dataTypeField.getSelectedItem();
            if ("Groups".equals(selectedType)) {
                objectField.setText("Groups");
                idParamField.setText("name__v");
            } else if ("Object".equals(selectedType)) {
                // Only clear if it wasn't already something else (to avoid clearing on load)
                // However, the requirement said "when a user changes the data type, the object field and id param field are cleared"
                // So we'll clear them here.
                objectField.setText("");
                idParamField.setText("");
            } else {
                objectField.setText("");
                idParamField.setText("");
            }
            updateFieldState();
        });
        panel.add(dataTypeField, gbc);
        row++;

        // Action
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Action:"), gbc);
        gbc.gridx = 1;
        actionField = new JComboBox<>(new String[]{"Create", "Delete", "Update", "Upsert"});
        actionField.setSelectedItem(csvManifest.getCsvDataStep().getAction());
        panel.add(actionField, gbc);
        row++;

        // Object
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Object:"), gbc);
        gbc.gridx = 1;
        JPanel objectPanel = new JPanel(new BorderLayout(5, 0));
        objectField = new JTextField(csvManifest.getCsvDataStep().getObject());
        objectPanel.add(objectField, BorderLayout.CENTER);
        objectSearchButton = new JButton(AllIcons.Actions.Search);
        objectSearchButton.setPreferredSize(new Dimension(30, 25));
        objectSearchButton.addActionListener(e -> findObject());
        objectPanel.add(objectSearchButton, BorderLayout.EAST);
        panel.add(objectPanel, gbc);
        row++;

        // ID Param
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("ID Param:"), gbc);
        gbc.gridx = 1;
        JPanel idParamPanel = new JPanel(new BorderLayout(5, 0));
        idParamField = new JTextField(csvManifest.getCsvDataStep().getIdParam());
        idParamPanel.add(idParamField, BorderLayout.CENTER);
        idParamSearchButton = new JButton(AllIcons.Actions.Search);
        idParamSearchButton.setPreferredSize(new Dimension(30, 25));
        idParamSearchButton.addActionListener(e -> findField());
        idParamPanel.add(idParamSearchButton, BorderLayout.EAST);
        panel.add(idParamPanel, gbc);
        row++;

        updateFieldState();

        // Record Migration Mode
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Record Migration Mode:"), gbc);
        gbc.gridx = 1;
        recordMigrationModeCheckbox = new JCheckBox("", csvManifest.getCsvDataStep().getRecordMigrationMode() != null && csvManifest.getCsvDataStep().getRecordMigrationMode());
        panel.add(recordMigrationModeCheckbox, gbc);
        row++;

        // Metadata separator
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(new JSeparator(JSeparator.HORIZONTAL), gbc);
        row++;
        gbc.gridwidth = 1;

        // Checksum
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Checksum:"), gbc);
        gbc.gridx = 1;
        checksumField = new JTextField(csvManifest.getChecksum());
        checksumField.setEditable(false);
        panel.add(checksumField, gbc);
        row++;

        // Record Count
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Record Count:"), gbc);
        gbc.gridx = 1;
        recordCountField = new JTextField(dataStep.getRecordCount() != null ? dataStep.getRecordCount().toString() : "");
        recordCountField.setEditable(false);
        panel.add(recordCountField, gbc);

        return panel;
    }

    @Override
    protected void doOKAction() {
        calculateMetadata();
        saveManifest();
        super.doOKAction();
    }

    private void findObject() {
        FindComponentDialog dialog = new FindComponentDialog(toolboxProject, "Object", null, objectField.getText());
        if (dialog.showAndGet()) {
            objectField.setText(dialog.getSelectedValue());
        }
    }

    private void findField() {
        String objectName = objectField.getText();
        if (objectName == null || objectName.isEmpty()) {
            Messages.showErrorDialog(toolboxProject.getProject(), "Please select an object first.", "Error");
            return;
        }
        FindComponentDialog dialog = new FindComponentDialog(toolboxProject, "Field", objectName, idParamField.getText());
        if (dialog.showAndGet()) {
            idParamField.setText(dialog.getSelectedValue());
        }
    }

    private void updateFieldState() {
        String selectedType = (String) dataTypeField.getSelectedItem();
        boolean isObject = "Object".equals(selectedType);
        boolean isGroups = "Groups".equals(selectedType);
        
        if (isObject) {
            objectField.setEditable(true);
            idParamField.setEditable(true);
            if (objectSearchButton != null) objectSearchButton.setVisible(true);
            if (idParamSearchButton != null) idParamSearchButton.setVisible(true);
        } else {
            objectField.setEditable(false);
            idParamField.setEditable(false);
            if (objectSearchButton != null) objectSearchButton.setVisible(false);
            if (idParamSearchButton != null) idParamSearchButton.setVisible(false);
            
            if (isGroups) {
                objectField.setText("Groups");
                idParamField.setText("name__v");
            }
        }
    }

    private void calculateMetadata() {
        if (csvFile.exists()) {
            String md5 = Checksum.getMd5(csvFile);
            csvManifest.setChecksum(md5);
            checksumField.setText(md5);

            int rowCount = FileIO.getCsvRowCount(csvFile) - 1;
            csvManifest.getCsvDataStep().setRecordCount(rowCount);
            recordCountField.setText(String.valueOf(rowCount));
        }
    }

    private void saveManifest() {
        csvManifest.setLabel(labelField.getText());
        csvManifest.setStepRequired(stepRequiredCheckbox.isSelected());
        // checksum is set in calculateMetadata()

        CsvDataStep dataStep = csvManifest.getCsvDataStep();
        dataStep.setObject(objectField.getText());
        dataStep.setIdParam(idParamField.getText());
        dataStep.setDataType((String) dataTypeField.getSelectedItem());
        dataStep.setAction((String) actionField.getSelectedItem());
        dataStep.setRecordMigrationMode(recordMigrationModeCheckbox.isSelected());
        // recordCount is set in calculateMetadata()
        
        try {

			XmlMapper xmlMapper = new XmlMapper();
			xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
			String xml = xmlMapper.writeValueAsString(csvManifest);
			FileUtils.writeStringToFile(new File(xmlFile.getAbsolutePath()), xml,"UTF-8");
			ApplicationManager.getApplication().invokeLater(() -> {

			});
            
            // Refresh the file system so IntelliJ sees the new/updated file
            com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(xmlFile);
        } catch (Exception e) {
            Messages.showErrorDialog(toolboxProject.getProject(), "Failed to save manifest: " + e.getMessage(), "Save Error");
            e.printStackTrace();
        }
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        super.createDefaultActions();
        return new Action[] { getOKAction(), getCancelAction() };
    }
}
