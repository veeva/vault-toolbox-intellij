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

/**
 * Dialog for creating a new SDK Profiler Session in Veeva Vault.
 * Allows users to specify a session label, a targeted user (or all users), and an optional description.
 */
public class CreateProfilerSessionDialog extends DialogWrapper {
    private final ToolboxProject toolboxProject;
    private JTextField labelField;
    private JXComboBox userComboBox;
    private JTextArea descriptionArea;
    private Map<String, String> userMap = new HashMap<>();

    /**
     * Initializes the dialog with the given project context.
     *
     * @param toolboxProject The toolbox project context.
     */
    public CreateProfilerSessionDialog(ToolboxProject toolboxProject) {
        super(toolboxProject.getProject(), true);
        this.toolboxProject = toolboxProject;
        init();
        setTitle("Create SDK Profiler Session");
        loadUsers();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(500, 400));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Label:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        labelField = new JTextField();
        panel.add(labelField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("User:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        userComboBox = new JXComboBox();
        panel.add(userComboBox, gbc);

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

    /**
     * Loads the list of active users from the Vault in a background thread to populate the user dropdown.
     * Adds an "All Users" option to the top of the list.
     */
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

    /**
     * @return The session label entered by the user.
     */
    public String getLabel() {
        return labelField.getText().trim();
    }

    /**
     * @return The Vault user ID corresponding to the selected username, or null if "All Users" is selected.
     */
    public String getUserId() {
        String selectedUser = (String) userComboBox.getSelectedItem();
        return userMap.get(selectedUser);
    }

    /**
     * @return The session description entered by the user.
     */
    public String getDescription() {
        return descriptionArea.getText().trim();
    }
}
