package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.request.QueryRequest;
import org.jetbrains.annotations.Nullable;
import org.jdesktop.swingx.JXComboBox;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CreateProfilerSessionDialog extends DialogWrapper {
    private final ToolboxProject toolboxProject;
    private JTextField labelField;
    private JXComboBox userComboBox;
    private JTextArea descriptionArea;
    private Map<String, String> userMap = new HashMap<>();

    public CreateProfilerSessionDialog(ToolboxProject toolboxProject) {
        super(true);
        this.toolboxProject = toolboxProject;
        init();
        setTitle("Create SDK Profiler Session");
        loadUsers();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Label
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Label:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        labelField = new JTextField();
        panel.add(labelField, gbc);

        // User
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("User:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        userComboBox = new JXComboBox();
        panel.add(userComboBox, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Description:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        descriptionArea = new JTextArea(5, 20);
        panel.add(new JScrollPane(descriptionArea), gbc);

        return panel;
    }

    private void loadUsers() {
        userComboBox.addItem("All Users");
        userMap.put("All Users", null);

        new Thread(() -> {
            if (toolboxProject.prepareRequest()) {
                QueryRequest request = toolboxProject.getVaultClient().newRequest(QueryRequest.class);
                QueryResponse response = request.query("SELECT id, username__sys FROM user__sys WHERE status__v = 'active__v'");

                if (response != null && !response.isFailure()) {
                    SwingUtilities.invokeLater(() -> {
                        for (QueryResponse.QueryResult result : response.getData()) {
                            String username = result.getString("username__sys");
                            String id = result.getString("id");
                            userComboBox.addItem(username);
                            userMap.put(username, id);
                        }
                    });
                }
            }
        }).start();
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (labelField.getText().trim().isEmpty()) {
            return new ValidationInfo("Label is required", labelField);
        }
        return null;
    }

    public String getLabel() {
        return labelField.getText().trim();
    }

    public String getUserId() {
        String selectedUser = (String) userComboBox.getSelectedItem();
        return userMap.get(selectedUser);
    }

    public String getDescription() {
        return descriptionArea.getText().trim();
    }
}
