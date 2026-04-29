package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.vapil.api.model.metadata.VaultObjectField;
import com.veeva.vault.vapil.api.model.response.MetaDataObjectResponse;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.request.MetaDataRequest;
import com.veeva.vault.vapil.api.request.QueryRequest;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FindComponentDialog extends DialogWrapper {
    private final ToolboxProject toolboxProject;
    private final String type;
    private final String parentObject;
    private final String initialValue;
    private JBList<ComponentItem> itemList;
    private DefaultListModel<ComponentItem> listModel;
    private SearchTextField searchTextField;
    private final List<ComponentItem> allItems = new ArrayList<>();
    private String selectedValue;

    public FindComponentDialog(ToolboxProject toolboxProject, String type, @Nullable String parentObject, @Nullable String initialValue) {
        super(toolboxProject.getProject());
        this.toolboxProject = toolboxProject;
        this.type = type;
        this.parentObject = parentObject;
        this.initialValue = initialValue;
        init();
        setTitle("Find " + type);
        loadData();
    }

    private void loadData() {
        if (!toolboxProject.prepareRequest()) return;

        allItems.clear();

        if ("Object".equals(type)) {
            QueryResponse response = toolboxProject.getVaultClient().newRequest(QueryRequest.class)
                    .query("SELECT component_name__v, name__v FROM vault_component__v WHERE component_type__v = 'Object' ORDER by component_name__v");
            
            if (response != null && !response.isFailure()) {
                for (QueryResponse.QueryResult data : response.getData()) {
                    String componentName = data.getString("component_name__v");
                    String name = data.getString("name__v");
                    allItems.add(new ComponentItem(componentName, componentName + " (" + name + ")"));
                }
            }
        } else if ("Field".equals(type) && parentObject != null) {
            MetaDataObjectResponse response = toolboxProject.getVaultClient().newRequest(MetaDataRequest.class)
                    .retrieveObjectMetadata(parentObject);
            
            if (response != null && !response.isFailure()) {
                List<VaultObjectField> fields = response.getObject().getFields();
                fields.sort(Comparator.comparing(VaultObjectField::getName));
                
                for (VaultObjectField field : fields) {
                    if (field.getUnique() != null && field.getUnique()) {
                        allItems.add(new ComponentItem(field.getName(), field.getName()));
                    }
                }
            }
        }

        filterData();
    }

    private void filterData() {
        String filter = searchTextField.getText().toLowerCase();
        listModel.clear();
        
        ComponentItem preselectedItem = null;
        for (ComponentItem item : allItems) {
            if (item.getLabel().toLowerCase().contains(filter)) {
                listModel.addElement(item);
                if (initialValue != null && initialValue.equals(item.getValue())) {
                    preselectedItem = item;
                }
            }
        }

        if (preselectedItem != null) {
            itemList.setSelectedValue(preselectedItem, true);
        } else {
            itemList.clearSelection();
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        int row = 0;

        // Search Field
        gbc.gridx = 0; gbc.gridy = row;
        gbc.weighty = 0.0;
        panel.add(new JLabel("Search:"), gbc);
        row++;
        
        gbc.gridy = row;
        searchTextField = new SearchTextField();
        searchTextField.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterData(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterData(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterData(); }
        });
        panel.add(searchTextField, gbc);
        row++;

        // List
        gbc.gridy = row;
        panel.add(new JLabel("Select " + type + ":"), gbc);
        row++;

        gbc.gridy = row;
        gbc.weighty = 1.0;
        listModel = new DefaultListModel<>();
        itemList = new JBList<>(listModel);
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JBScrollPane scrollPane = new JBScrollPane(itemList);
        panel.add(scrollPane, gbc);

        panel.setPreferredSize(new Dimension(400, 300));
        return panel;
    }

    @Override
    protected void doOKAction() {
        ComponentItem selected = itemList.getSelectedValue();
        if (selected != null) {
            selectedValue = selected.getValue();
        }
        super.doOKAction();
    }

    public String getSelectedValue() {
        return selectedValue;
    }

    private static class ComponentItem {
        private final String value;
        private final String label;

        public ComponentItem(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
