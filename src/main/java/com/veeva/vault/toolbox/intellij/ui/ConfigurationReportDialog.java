package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.toolbox.core.config.ConfigurationReport;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigurationReportDialog extends DialogWrapper {

    private JBCheckBox includeVaultSettingsCb;
    private JBCheckBox includeInactiveComponentsCb;
    private JBCheckBox includeDocBinderTemplatesCb;
    private JBCheckBox suppressEmptyResultsCb;
    private DateTimePickerControl modifiedSincePicker;
    private JBCheckBox enableModifiedSinceCb;
    private JBTextField componentTypesField;
    private ComboBox<String> outputFormatComboBox;

    public ConfigurationReportDialog(@Nullable Project project) {
        super(project);
        setTitle("Download Configuration Report");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        includeVaultSettingsCb = new JBCheckBox("Include Vault Settings", true);
        includeVaultSettingsCb.setToolTipText("If checked, Vault Settings are included. Enabled by default.");
        panel.add(includeVaultSettingsCb, gbc);

        gbc.gridy++;
        includeInactiveComponentsCb = new JBCheckBox("Include Inactive Components", false);
        includeInactiveComponentsCb.setToolTipText("If checked, inactive components and subcomponents are included in the report.");
        panel.add(includeInactiveComponentsCb, gbc);

        gbc.gridy++;
        includeDocBinderTemplatesCb = new JBCheckBox("Include Doc Binder Templates", true);
        includeDocBinderTemplatesCb.setToolTipText("If checked, document and binder templates are included. Enabled by default.");
        panel.add(includeDocBinderTemplatesCb, gbc);

        gbc.gridy++;
        suppressEmptyResultsCb = new JBCheckBox("Suppress Empty Results", false);
        suppressEmptyResultsCb.setToolTipText("If checked, Vault excludes tabs with only header rows from the report.");
        panel.add(suppressEmptyResultsCb, gbc);

        gbc.gridy++;
        enableModifiedSinceCb = new JBCheckBox("Include Components Modified Since:");
        enableModifiedSinceCb.setToolTipText("Only include components modified since the specified date. Not available for subcomponents.");
        panel.add(enableModifiedSinceCb, gbc);

        gbc.gridy++;
        modifiedSincePicker = new DateTimePickerControl(null, null);
        modifiedSincePicker.setEnabled(false);
        modifiedSincePicker.setTimeVisible(false);
        enableModifiedSinceCb.addActionListener(e -> modifiedSincePicker.setEnabled(enableModifiedSinceCb.isSelected()));
        panel.add(modifiedSincePicker, gbc);

        gbc.gridy++;
        JBLabel componentTypesLabel = new JBLabel("Component Types (comma-separated):");
        componentTypesLabel.setToolTipText("Add a comma-separated list of component types to include (e.g., Doclifecycle,Workflow). If empty, includes all.");
        panel.add(componentTypesLabel, gbc);

        gbc.gridy++;
        componentTypesField = new JBTextField();
        componentTypesField.setToolTipText("Add a comma-separated list of component types to include (e.g., Doclifecycle,Workflow). If empty, includes all.");
        panel.add(componentTypesField, gbc);

        gbc.gridy++;
        JBLabel outputFormatLabel = new JBLabel("Output Format:");
        outputFormatLabel.setToolTipText("Output report as Excel (XSLX) or Excel Macro-Enabled (XLSM). Defaults to Excel Macro-Enabled.");
        panel.add(outputFormatLabel, gbc);

        gbc.gridy++;
        outputFormatComboBox = new ComboBox<>(new String[]{"Excel Macro-Enabled (XLSM)", "Excel (XLSX)"});
        outputFormatComboBox.setToolTipText("Output report as Excel (XSLX) or Excel Macro-Enabled (XLSM). Defaults to Excel Macro-Enabled.");
        panel.add(outputFormatComboBox, gbc);

        return panel;
    }

    public ConfigurationReport.Options getOptions() {
        ConfigurationReport.Options options = new ConfigurationReport.Options();
        
        options.includeVaultSettings = includeVaultSettingsCb.isSelected();
        options.includeInactiveComponents = includeInactiveComponentsCb.isSelected();
        options.includeDocBinderTemplates = includeDocBinderTemplatesCb.isSelected();
        options.suppressEmptyResults = suppressEmptyResultsCb.isSelected();
        
        if (enableModifiedSinceCb.isSelected()) {
            LocalDateTime dt = modifiedSincePicker.getSelectedDateTime();
            if (dt != null) {
                options.includeComponentsModifiedSince = dt.atZone(ZoneId.systemDefault());
            }
        }
        
        String types = componentTypesField.getText().trim();
        if (!types.isEmpty()) {
            List<String> typeList = Arrays.stream(types.split(","))
                                          .map(String::trim)
                                          .filter(s -> !s.isEmpty())
                                          .collect(Collectors.toList());
            if (!typeList.isEmpty()) {
                options.componentTypes = typeList;
            }
        }
        
        String format = (String) outputFormatComboBox.getSelectedItem();
        if ("Excel (XLSX)".equals(format)) {
            options.outputFormat = com.veeva.vault.vapil.api.request.ConfigurationMigrationRequest.OutputFormat.EXCEL;
        } else if ("Excel Macro-Enabled (XLSM)".equals(format)) {
            options.outputFormat = com.veeva.vault.vapil.api.request.ConfigurationMigrationRequest.OutputFormat.EXCEL_MACRO_ENABLED;
        }
        
        return options;
    }
}
