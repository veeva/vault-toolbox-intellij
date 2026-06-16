package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.toolbox.intellij.metadata.MetadataIndex;
import com.veeva.vault.toolbox.intellij.metadata.MetadataService;
import com.veeva.vault.toolbox.intellij.metadata.model.FieldMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.RelationshipMeta;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Visual VQL query builder: pick an object, choose the fields to select, add filter
 * conditions, and optionally order the results — with a live VQL preview. Object and
 * field metadata come from the shared {@link MetadataService} cache (fields are lazily
 * loaded and the dialog repopulates when they arrive).
 */
public class QueryBuilderDialog extends DialogWrapper {

    private static final String[] OPERATORS =
            {"=", "!=", "<", ">", "<=", ">=", "LIKE", "IN", "IS NULL", "IS NOT NULL"};

    private final ToolboxProject toolboxProject;
    private final MetadataService metadataService;
    private final Runnable metadataListener = this::onMetadataChanged;

    private final ComboBox<String> objectCombo = new ComboBox<>();
    private JTextField objectEditorField;
    private final JCheckBox selectAllCheck = new JCheckBox("Select all");
    private final JCheckBox includeRelatedCheck = new JCheckBox("Include related fields");
    private final CheckBoxList<String> fieldList = new CheckBoxList<>();
    private final DefaultTableModel conditionsModel =
            new DefaultTableModel(new Object[]{"And/Or", "Field", "Operator", "Value"}, 0);
    private final JBTable conditionsTable = new JBTable(conditionsModel);
    private final ComboBox<String> fieldEditorCombo = new ComboBox<>();
    private final ComboBox<String> orderByCombo = new ComboBox<>();
    private final ComboBox<String> orderDirCombo = new ComboBox<>(new String[]{"ASC", "DESC"});
    private final JBTextArea preview = new JBTextArea(5, 60);

    /** All object names available to query, used to filter the object picker. */
    private final List<String> allObjects = new ArrayList<>();
    /** Field names currently shown in {@link #fieldList}, in display order. */
    private final List<String> currentFields = new ArrayList<>();
    /** Field name → data type for the current object, used to decide value quoting. */
    private final Map<String, String> fieldTypes = new LinkedHashMap<>();

    private boolean populating = false;
    private boolean comboMutating = false;
    private String resultVql;

    public QueryBuilderDialog(ToolboxProject toolboxProject) {
        super(toolboxProject.getProject(), false);
        this.toolboxProject = toolboxProject;
        this.metadataService = MetadataService.getInstance(toolboxProject.getProject());
        setTitle("Build VQL Query");

        objectCombo.setEditable(true);
        objectCombo.addActionListener(e -> {
            if (!populating && !comboMutating) {
                onObjectChanged();
            }
        });
        if (objectCombo.getEditor().getEditorComponent() instanceof JTextField textField) {
            objectEditorField = textField;
            textField.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent e) {
                    if (!populating && !comboMutating) {
                        SwingUtilities.invokeLater(() -> applyObjectFilter(textField.getText()));
                    }
                }
            });
        }
        fieldList.setCheckBoxListListener((index, value) -> {
            updatePreview();
            updateSelectAllState();
        });
        selectAllCheck.addActionListener(e -> setAllFieldsSelected(selectAllCheck.isSelected()));
        updateSelectAllState();
        conditionsModel.addTableModelListener(e -> updatePreview());
        orderByCombo.addActionListener(e -> updatePreview());
        orderDirCombo.addActionListener(e -> updatePreview());
        includeRelatedCheck.addActionListener(e -> {
            loadFields();
            updatePreview();
        });

        // Commit an in-progress cell edit (e.g. a typed value) when focus leaves the table,
        // so clicking OK / Preview includes the condition rather than dropping it.
        conditionsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        conditionsTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new ComboBox<>(new String[]{"AND", "OR"})));
        conditionsTable.getColumnModel().getColumn(0).setMaxWidth(80);
        conditionsTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(fieldEditorCombo));
        conditionsTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new ComboBox<>(OPERATORS)));

        preview.setEditable(false);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        preview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, preview.getFont().getSize()));

        metadataService.addChangeListener(metadataListener);
        if (!metadataService.getIndex().isReady()) {
            metadataService.refreshAsync(false);
        }
        populateObjects();
        init();
        onObjectChanged();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel objectRow = new JPanel(new BorderLayout(8, 0));
        objectRow.add(new JBLabel("Object:"), BorderLayout.WEST);
        objectRow.add(objectCombo, BorderLayout.CENTER);
        objectRow.setBorder(JBUI.Borders.emptyBottom(8));

        JPanel listWithToggle = new JPanel(new BorderLayout());
        listWithToggle.add(selectAllCheck, BorderLayout.NORTH);
        listWithToggle.add(new JBScrollPane(fieldList), BorderLayout.CENTER);

        JPanel fieldsPanel = new JPanel(new BorderLayout());
        fieldsPanel.setBorder(IdeBorderFactory.createTitledBorder("Fields to select", false));
        fieldsPanel.add(includeRelatedCheck, BorderLayout.NORTH);
        fieldsPanel.add(listWithToggle, BorderLayout.CENTER);

        JPanel filtersPanel = new JPanel(new BorderLayout());
        filtersPanel.setBorder(IdeBorderFactory.createTitledBorder("Filters", false));
        filtersPanel.add(ToolbarDecorator.createDecorator(conditionsTable)
                .setAddAction(b -> conditionsModel.addRow(new Object[]{"AND", firstField(), "=", ""}))
                .setRemoveAction(b -> removeSelectedCondition())
                .createPanel(), BorderLayout.CENTER);

        // Fixed prototype width so long related-field names don't resize/wrap the row.
        orderByCombo.setPrototypeDisplayValue("wwwwwwwwwwwwwwwwwwwwww");
        JPanel orderRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        orderRow.add(new JBLabel("Order by:"));
        orderRow.add(orderByCombo);
        orderRow.add(orderDirCombo);
        filtersPanel.add(orderRow, BorderLayout.SOUTH);

        JBSplitter center = new JBSplitter(false, 0.4f);
        center.setFirstComponent(fieldsPanel);
        center.setSecondComponent(filtersPanel);

        JBScrollPane previewScroll = new JBScrollPane(preview);
        previewScroll.setPreferredSize(new Dimension(700, 120));
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(IdeBorderFactory.createTitledBorder("Preview", false));
        previewPanel.add(previewScroll, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.add(objectRow, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(previewPanel, BorderLayout.SOUTH);
        root.setPreferredSize(new Dimension(720, 520));
        return root;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (selectedObjectName() == null) {
            return new ValidationInfo("Select an object to query.", objectCombo);
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        if (conditionsTable.isEditing()) {
            conditionsTable.getCellEditor().stopCellEditing();
        }
        resultVql = generateVql();
        super.doOKAction();
    }

    @Override
    protected void dispose() {
        metadataService.removeChangeListener(metadataListener);
        super.dispose();
    }

    /** @return the built VQL, or {@code null} if the dialog was cancelled. */
    public String getQuery() {
        return resultVql;
    }

    // --- Metadata wiring -------------------------------------------------------

    private void onMetadataChanged() {
        populateObjects();
        loadFields();
        updatePreview();
    }

    private void populateObjects() {
        populating = true;
        comboMutating = true;
        try {
            String previous = selectedObjectName();
            allObjects.clear();
            allObjects.addAll(metadataService.getIndex().objectNames());
            Collections.sort(allObjects);
            objectCombo.setModel(new DefaultComboBoxModel<>(allObjects.toArray(new String[0])));
            String toSelect = previous != null ? previous : (allObjects.isEmpty() ? "" : allObjects.get(0));
            if (objectEditorField != null) {
                objectEditorField.setText(toSelect);
            }
            objectCombo.setSelectedItem(allObjects.contains(toSelect) ? toSelect : null);
            objectCombo.setPopupVisible(false);
        } finally {
            comboMutating = false;
            populating = false;
        }
    }

    /** Filters the object dropdown to names containing the typed text, keeping the typed text intact. */
    private void applyObjectFilter(String text) {
        if (objectEditorField == null) {
            return;
        }
        comboMutating = true;
        boolean exact = false;
        try {
            String query = text == null ? "" : text;
            String lower = query.toLowerCase().trim();
            List<String> matches = new ArrayList<>();
            for (String name : allObjects) {
                if (name.toLowerCase().contains(lower)) {
                    matches.add(name);
                }
            }
            int caret = objectEditorField.getCaretPosition();
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(matches.toArray(new String[0]));
            model.setSelectedItem(query);
            objectCombo.setModel(model);
            objectEditorField.setText(query);
            objectEditorField.setCaretPosition(Math.min(caret, query.length()));
            exact = allObjects.contains(query.trim());
            objectCombo.setPopupVisible(!matches.isEmpty() && !exact && objectEditorField.hasFocus());
        } finally {
            comboMutating = false;
        }
        if (exact) {
            onObjectChanged();
        }
    }

    /** @return the typed/selected object name if it is a real object, else {@code null}. */
    private String selectedObjectName() {
        String text = objectEditorField != null ? objectEditorField.getText().trim() : "";
        return allObjects.contains(text) ? text : null;
    }

    private void onObjectChanged() {
        loadFields();
        updatePreview();
    }

    private void loadFields() {
        try {
            String object = selectedObjectName();
            fieldList.clear();
            currentFields.clear();
            fieldTypes.clear();
            if (object == null) {
                refreshFieldDependentCombos();
                return;
            }

            MetadataIndex index = metadataService.getIndex();
            if (!index.fieldsLoaded(object)) {
                com.intellij.openapi.progress.ProgressManager.getInstance().run(
                    new com.intellij.openapi.progress.Task.Modal(toolboxProject.getProject(), "Loading Metadata", false) {
                        @Override
                        public void run(@org.jetbrains.annotations.NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                            indicator.setIndeterminate(true);
                            indicator.setText("Retrieving metadata for " + object + "...");
                            metadataService.ensureObjectFieldsLoadedSync(object);
                        }
                    }
                );
                refreshFieldDependentCombos();
                return; // repopulated by onMetadataChanged() once fields arrive
            }

            List<FieldMeta> fields = new ArrayList<>(index.fieldsFor(object));
            fields.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (FieldMeta field : fields) {
                String name = field.getName();
                String type = field.getType() != null ? field.getType() : "";
                currentFields.add(name);
                fieldTypes.put(name, type);
                String label = type.isEmpty() ? name : name + "  (" + type + ")";
                boolean preselect = name.equals("id") || name.equals("name__v");
                fieldList.addItem(name, label, preselect);
            }

            if (includeRelatedCheck.isSelected()) {
                addRelatedFields(index, object);
            }
            refreshFieldDependentCombos();
        } finally {

            fieldList.setModel(fieldList.getModel());
            fieldList.revalidate();
            fieldList.repaint();
        }
    }

    /**
     * Adds one level of {@code relationship.field} entries for the object's relationships.
     * Referenced-object fields are lazily fetched; entries appear once they load (the
     * dialog repopulates via the metadata change listener).
     */
    private void addRelatedFields(MetadataIndex index, String object) {
        List<RelationshipMeta> relationships = new ArrayList<>(index.relationshipsFor(object));
        relationships.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (RelationshipMeta relationship : relationships) {
            String referenced = relationship.getReferencedObject();
            if (referenced == null || referenced.isEmpty()) {
                continue;
            }
            if (!index.fieldsLoaded(referenced)) {
                metadataService.ensureObjectFieldsLoaded(referenced);
                continue;
            }
            List<FieldMeta> relFields = new ArrayList<>(index.fieldsFor(referenced));
            relFields.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (FieldMeta field : relFields) {
                String dotted = relationship.getName() + "." + field.getName();
                String type = field.getType() != null ? field.getType() : "";
                currentFields.add(dotted);
                fieldTypes.put(dotted, type);
                fieldList.addItem(dotted, dotted + (type.isEmpty() ? "" : "  (" + type + ")"), false);
            }
        }
    }

    private void refreshFieldDependentCombos() {
        fieldEditorCombo.setModel(new DefaultComboBoxModel<>(currentFields.toArray(new String[0])));

        List<String> orderOptions = new ArrayList<>();
        orderOptions.add(""); // none
        orderOptions.addAll(currentFields);
        orderByCombo.setModel(new DefaultComboBoxModel<>(orderOptions.toArray(new String[0])));

        updateSelectAllState();
    }

    private void setAllFieldsSelected(boolean selected) {
        for (int i = 0; i < currentFields.size(); i++) {
            fieldList.setItemSelected(fieldList.getItemAt(i), selected);
        }
        fieldList.repaint();
        updatePreview();
        updateSelectAllState();
    }

    private boolean allFieldsSelected() {
        if (currentFields.isEmpty()) {
            return false;
        }
        for (int i = 0; i < currentFields.size(); i++) {
            if (!fieldList.isItemSelected(i)) {
                return false;
            }
        }
        return true;
    }

    /** Syncs the select-all checkbox state/label with the current field selection. */
    private void updateSelectAllState() {
        boolean all = allFieldsSelected();
        selectAllCheck.setEnabled(!currentFields.isEmpty());
        selectAllCheck.setSelected(all);
        selectAllCheck.setText(all ? "Deselect all" : "Select all");
    }

    private String firstField() {
        return currentFields.isEmpty() ? "" : currentFields.get(0);
    }

    private void removeSelectedCondition() {
        int row = conditionsTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        if (conditionsTable.isEditing()) {
            conditionsTable.getCellEditor().stopCellEditing();
        }
        conditionsModel.removeRow(conditionsTable.convertRowIndexToModel(row));
    }

    // --- Query generation ------------------------------------------------------

    private void updatePreview() {
        preview.setText(generateVql());
    }

    private String generateVql() {
        String object = selectedObjectName();
        if (object == null) {
            return "";
        }

        List<String> selected = selectedFields();
        if (selected.isEmpty()) {
            selected.add("id");
        }

        StringBuilder sb = new StringBuilder("SELECT ");
        sb.append(String.join(", ", selected)).append(" FROM ").append(object);

        String where = whereExpression();
        if (!where.isEmpty()) {
            sb.append(" WHERE ").append(where);
        }

        Object orderField = orderByCombo.getSelectedItem();
        if (orderField instanceof String s && !s.isEmpty()) {
            sb.append(" ORDER BY ").append(s).append(' ').append(orderDirCombo.getSelectedItem());
        }
        return sb.toString();
    }

    private List<String> selectedFields() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < currentFields.size(); i++) {
            if (fieldList.isItemSelected(i)) {
                selected.add(currentFields.get(i));
            }
        }
        return selected;
    }

    /** Builds the {@code WHERE} expression, joining conditions with each row's AND/OR connector. */
    private String whereExpression() {
        StringBuilder where = new StringBuilder();
        boolean first = true;
        for (int r = 0; r < conditionsModel.getRowCount(); r++) {
            String connector = asString(conditionsModel.getValueAt(r, 0));
            String field = asString(conditionsModel.getValueAt(r, 1));
            String operator = asString(conditionsModel.getValueAt(r, 2));
            String value = asString(conditionsModel.getValueAt(r, 3));
            if (field.isEmpty() || operator.isEmpty()) {
                continue;
            }
            String clause = buildClause(field, operator, value);
            if (first) {
                where.append(clause);
                first = false;
            } else {
                where.append(' ').append(connector.isEmpty() ? "AND" : connector).append(' ').append(clause);
            }
        }
        return where.toString();
    }

    private String buildClause(String field, String operator, String value) {
        String type = fieldTypes.getOrDefault(field, "");
        switch (operator) {
            case "IS NULL":
                return field + " IS NULL";
            case "IS NOT NULL":
                return field + " IS NOT NULL";
            case "IN":
                return field + " IN (" + quoteList(value, type) + ")";
            case "LIKE":
                return field + " LIKE " + quote(value, "String");
            default:
                return field + " " + operator + " " + quote(value, type);
        }
    }

    private static String quoteList(String value, String type) {
        List<String> parts = new ArrayList<>();
        for (String item : value.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                parts.add(quote(trimmed, type));
            }
        }
        return String.join(", ", parts);
    }

    private static String quote(String value, String type) {
        String v = value.trim();
        if (v.isEmpty()) {
            return "''";
        }
        if (v.startsWith("'") && v.endsWith("'")) {
            return v;
        }
        if ("Number".equalsIgnoreCase(type)) {
            return v;
        }
        if ("Boolean".equalsIgnoreCase(type)) {
            return v.toLowerCase();
        }
        return "'" + v.replace("'", "\\'") + "'";
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
