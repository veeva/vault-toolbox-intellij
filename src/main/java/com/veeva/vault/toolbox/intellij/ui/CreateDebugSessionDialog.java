package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.model.response.SdkDebugSessionCreateResponse;
import com.veeva.vault.vapil.api.request.LogRequest;
import com.veeva.vault.vapil.api.request.QueryRequest;
import org.jetbrains.annotations.Nullable;
import org.jdesktop.swingx.JXComboBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dialog for creating a new SDK Debug Log Session in Veeva Vault.
 * Allows users to specify the session name, targeted user, log level, and class filters.
 */
public class CreateDebugSessionDialog extends DialogWrapper {
    private final ToolboxProject toolboxProject;
    private JTextField nameField;
    private JXComboBox userComboBox;
    private JXComboBox logLevelComboBox;
    private DefaultListModel<String> classFiltersModel;
    private JList<String> classFiltersList;
    private Map<String, String> userMap = new HashMap<>();

    /**
     * Initializes the dialog with the given project context.
     *
     * @param toolboxProject The toolbox project context.
     */
    public CreateDebugSessionDialog(ToolboxProject toolboxProject) {
        super(toolboxProject.getProject(), true);
        this.toolboxProject = toolboxProject;
        init();
        setTitle("Create SDK Debug Log Session");
        loadUsers();
    }

    /**
     * Creates the central panel of the dialog containing the form fields.
     *
     * @return The constructed JComponent for the center panel.
     */
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
        panel.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        nameField = new JTextField();
        panel.add(nameField, gbc);

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
        panel.add(new JLabel("Log Level:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        logLevelComboBox = new JXComboBox(new String[]{"ALL", "EXCEPTIONS", "ERROR", "WARNING", "INFO", "DEBUG"});
        panel.add(logLevelComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        panel.add(new JLabel("Class Filters:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        classFiltersModel = new DefaultListModel<>();
        classFiltersList = new JList<>(classFiltersModel);
        JScrollPane listScrollPane = new JScrollPane(classFiltersList);
        listScrollPane.setPreferredSize(new Dimension(0, 100));

        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.add(listScrollPane, BorderLayout.CENTER);

        JButton addButton = new JButton(AllIcons.General.Add);
        addButton.setToolTipText("Add Class Filter");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ClassFilterDialog dialog = new ClassFilterDialog(toolboxProject);
                if (dialog.showAndGet()) {
                    List<String> selected = dialog.getSelectedClasses();
                    for (String cls : selected) {
                        if (!classFiltersModel.contains(cls)) {
                            classFiltersModel.addElement(cls);
                        }
                    }
                }
            }
        });

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(addButton);
        listPanel.add(toolbar, BorderLayout.NORTH);

        panel.add(listPanel, gbc);

        return panel;
    }

    /**
     * Loads the list of active users from the Vault in a background thread to populate the user dropdown.
     */
    private void loadUsers() {
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

    /**
     * Validates the form fields before allowing the OK action to proceed.
     *
     * @return A ValidationInfo object containing the error message and component, or null if validation passes.
     */
    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (nameField.getText().trim().isEmpty()) {
            return new ValidationInfo("Name is required", nameField);
        }
        if (userComboBox.getSelectedItem() == null) {
            return new ValidationInfo("User is required", userComboBox);
        }
        return null;
    }

    /**
     * Performs the creation of the SDK Debug Session on Vault.
     * Extracts UI values on the main thread, then executes the API call on a background thread with visual feedback and validation.
     */
    @Override
    protected void doOKAction() {
        if (getOKAction().isEnabled()) {
            getOKAction().setEnabled(false);

            String sessionName = getSessionName();
            String userId = getUserId();
            String logLevel = getLogLevel();
            Set<String> classFilters = getClassFilters();

            new Thread(() -> {
                try {
                    LogRequest request = toolboxProject.getVaultClient().newRequest(LogRequest.class);
                    request.setLogLevel(logLevel);

                    if (!classFilters.isEmpty()) {
                        request.setClassFilters(classFilters);
                    }

                    SdkDebugSessionCreateResponse response = request.createDebugLog(sessionName, userId);

                    SwingUtilities.invokeLater(() -> {
                        getOKAction().setEnabled(true);
                        if (response != null && !response.isFailure()) {
                            JOptionPane.showMessageDialog(this.getContentPane(), "Session created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                            super.doOKAction();
                        } else {
                            String errorMessage = "Unknown error";
                            if (response != null) {
                                if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                                    errorMessage = response.getErrors().get(0).getMessage();
                                } else if (response.getResponseMessage() != null) {
                                    errorMessage = response.getResponseMessage();
                                }
                            }
                            JOptionPane.showMessageDialog(this.getContentPane(), "Failed to create session: " + errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        getOKAction().setEnabled(true);
                        JOptionPane.showMessageDialog(this.getContentPane(), "An error occurred while creating session.", "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        }
    }

    /**
     * Retrieves the session name entered by the user.
     * * @return The session name.
     */
    public String getSessionName() {
        return nameField.getText().trim();
    }

    /**
     * Retrieves the Vault user ID corresponding to the selected username.
     * * @return The Vault user ID.
     */
    public String getUserId() {
        String selectedUser = (String) userComboBox.getSelectedItem();
        return userMap.get(selectedUser);
    }

    /**
     * Maps the human-readable log level selection to the Vault API system name.
     *
     * @return The system name of the selected log level.
     */
    public String getLogLevel() {
        String selected = (String) logLevelComboBox.getSelectedItem();
        if (selected == null) return null;
        switch (selected) {
            case "ALL": return "all__sys";
            case "EXCEPTIONS": return "exceptions__sys";
            case "ERROR": return "error__sys";
            case "WARNING": return "warning__sys";
            case "INFO": return "info__sys";
            case "DEBUG": return "debug__sys";
            default: return null;
        }
    }

    /**
     * Retrieves the set of class filters added to the session configuration.
     * * @return A set of class filter strings.
     */
    public Set<String> getClassFilters() {
        Set<String> filters = new HashSet<>();
        for (int i = 0; i < classFiltersModel.size(); i++) {
            filters.add(classFiltersModel.getElementAt(i));
        }
        return filters;
    }
}