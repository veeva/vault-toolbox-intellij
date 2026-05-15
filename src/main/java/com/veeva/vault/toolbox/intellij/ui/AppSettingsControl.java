package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.toolbox.intellij.credentials.VaultCredentialManager;
import com.veeva.vault.toolbox.intellij.settings.SavedCredential;
import com.veeva.vault.toolbox.intellij.settings.Vault;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Provides the UI control for managing application settings within the Vault Toolbox plugin.
 * This class builds and manages the panel displayed in the settings dialog.
 */
public class AppSettingsControl {

    private final JPanel mainPanel;
    private final JBCheckBox autoConnectField = new JBCheckBox("Auto Connect to Vault");
    private final JBTextField vaultDnsField = new JBTextField();
    private final JBTextField usernameField = new JBTextField();
    private final JBTextField csvMaxRowsField = new JBTextField(6);
    private final JSpinner connectionTimeoutField = new JSpinner(new SpinnerNumberModel(15, 1, 300, 1));
    private final JBRadioButton basicAuthField = new JBRadioButton("Basic");
    private final JBRadioButton sessionIdField = new JBRadioButton("Session");
    private final JBPanel authenticationTypePanel = new JBPanel();
    private final ButtonGroup authGroup = new ButtonGroup();
    private final JBCheckBox allowAllCertificatesField = new JBCheckBox("Allow All Certificates");

    private final List<SavedCredential> credentials = new ArrayList<>();
    private final CredentialTableModel tableModel = new CredentialTableModel();
    private final JXTable credentialsTable = new JXTable(tableModel);

    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;
    private JButton prevPageButton;
    private JButton nextPageButton;
    private JLabel pageLabel;

    private int sortColumn = -1;
    private boolean sortAscending = true;
    private boolean sortingInProgress = false;

    /**
     * Constructs the AppSettingsControl and initializes the UI components.
     */
    public AppSettingsControl() {
        authGroup.add(basicAuthField);
        authGroup.add(sessionIdField);

        authenticationTypePanel.setLayout(new GridLayout(1, 2));
        authenticationTypePanel.add(basicAuthField);
        authenticationTypePanel.add(sessionIdField);

        basicAuthField.addActionListener(e -> changeAuthenticationType());
        sessionIdField.addActionListener(e -> changeAuthenticationType());

        JPanel settingsPanel = FormBuilder.createFormBuilder()
                .addComponent(autoConnectField, 1)
                .addComponent(allowAllCertificatesField, 1)
                .addLabeledComponent(new JBLabel("Default Vault DNS"), vaultDnsField, 1, false)
                .addLabeledComponent(new JBLabel("Default Authentication Type"), authenticationTypePanel)
                .addLabeledComponent(new JBLabel("Default Username"), usernameField, 1, false)
                .addLabeledComponent(new JBLabel("Connection Timeout (seconds)"), connectionTimeoutField, 1, false)
                .getPanel();

        JPanel csvFieldWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        csvFieldWrapper.add(csvMaxRowsField);
        JPanel csvPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("CSV Max Rows"), csvFieldWrapper, 1, false)
                .getPanel();

        mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.add(settingsPanel, BorderLayout.NORTH);
        mainPanel.add(buildCredentialsSection(), BorderLayout.CENTER);
        mainPanel.add(csvPanel, BorderLayout.SOUTH);
    }

    /**
     * Builds the credentials section of the settings panel.
     *
     * @return The credentials section JPanel.
     */
    private JPanel buildCredentialsSection() {
        credentialsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        credentialsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        credentialsTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        credentialsTable.getColumnModel().getColumn(0).setMaxWidth(60);
        credentialsTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        credentialsTable.getColumnModel().getColumn(4).setMaxWidth(80);
        credentialsTable.setRowHeight(credentialsTable.getRowHeight() + 4);

        credentialsTable.getRowSorter().addRowSorterListener(new RowSorterListener() {
            @Override
            public void sorterChanged(RowSorterEvent e) {
                if (e.getType() != RowSorterEvent.Type.SORT_ORDER_CHANGED || sortingInProgress) return;
                List<? extends RowSorter.SortKey> keys = credentialsTable.getRowSorter().getSortKeys();
                if (!keys.isEmpty()) {
                    RowSorter.SortKey key = keys.get(0);
                    sortColumn = key.getColumn();
                    sortAscending = key.getSortOrder() == SortOrder.ASCENDING;
                } else {
                    sortColumn = -1;
                }
                sortingInProgress = true;
                currentPage = 0;
                sortCredentials();
                refreshPage();
                sortingInProgress = false;
            }
        });

        JScrollPane scrollPane = new JScrollPane(credentialsTable);
        scrollPane.setPreferredSize(new Dimension(-1, 150));

        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton removeButton = new JButton("Remove");
        JButton setDefaultButton = new JButton("Set as Default");

        editButton.setEnabled(false);
        removeButton.setEnabled(false);
        setDefaultButton.setEnabled(false);

        credentialsTable.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = credentialsTable.getSelectedRow() >= 0;
            editButton.setEnabled(selected);
            removeButton.setEnabled(selected);
            setDefaultButton.setEnabled(selected);
        });

        addButton.addActionListener(e -> onAddCredential());
        editButton.addActionListener(e -> onEditCredential());
        removeButton.addActionListener(e -> onRemoveCredential());
        setDefaultButton.addActionListener(e -> onSetDefault());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(Box.createHorizontalStrut(8));
        buttonPanel.add(setDefaultButton);

        JLabel sectionLabel = new JLabel("Saved Credentials:");

        prevPageButton = new JButton(AllIcons.Actions.Back);
        nextPageButton = new JButton(AllIcons.Actions.Forward);
        pageLabel = new JLabel("1 / 1", SwingConstants.CENTER);
        prevPageButton.setMargin(JBUI.emptyInsets());
        nextPageButton.setMargin(JBUI.emptyInsets());
        prevPageButton.addActionListener(e -> { currentPage--; refreshPage(); });
        nextPageButton.addActionListener(e -> { currentPage++; refreshPage(); });

        JPanel pagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        pagePanel.add(prevPageButton);
        pagePanel.add(pageLabel);
        pagePanel.add(nextPageButton);

        JPanel southPanel = new JPanel(new BorderLayout(0, 2));
        southPanel.add(pagePanel, BorderLayout.NORTH);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setBorder(JBUI.Borders.emptyTop(4));
        section.add(sectionLabel, BorderLayout.NORTH);
        section.add(scrollPane, BorderLayout.CENTER);
        section.add(southPanel, BorderLayout.SOUTH);
        return section;
    }

    /**
     * Handles the action of adding a new credential.
     */
    private void onAddCredential() {
        SavedCredentialDialog dialog = new SavedCredentialDialog(mainPanel, null);
        if (!dialog.showAndGet()) return;

        SavedCredential newCred = dialog.getCredential();
        int dupIdx = indexOfCredentialByLabel(newCred.label, null);
        if (dupIdx >= 0) {
            int choice = JOptionPane.showConfirmDialog(mainPanel,
                    "A credential with label \"" + newCred.label + "\" already exists. Overwrite it?",
                    "Duplicate Label", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;

            SavedCredential existing = credentials.get(dupIdx);
            existing.vaultDNS = newCred.vaultDNS;
            existing.username = newCred.username;
            existing.authenticationType = newCred.authenticationType;
            existing.isDefault = newCred.isDefault;

            String plainPassword = dialog.getPlaintextPassword();
            String plainSessionId = dialog.getPlaintextSessionId();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                if (existing.authenticationType == Vault.AuthenticationType.BASIC) {
                    if (!plainPassword.isEmpty())
                        VaultCredentialManager.setUsernamePasswordById(existing.id, existing.username, plainPassword);
                } else {
                    if (!plainSessionId.isEmpty())
                        VaultCredentialManager.setSessionIdById(existing.id, plainSessionId);
                }
            });

            currentPage = dupIdx / PAGE_SIZE;
            refreshPage();
            credentialsTable.setRowSelectionInterval(dupIdx % PAGE_SIZE, dupIdx % PAGE_SIZE);
            return;
        }

        if (newCred.isDefault) credentials.forEach(c -> c.isDefault = false);
        credentials.add(newCred);
        currentPage = (credentials.size() - 1) / PAGE_SIZE;
        refreshPage();
        int newRow = (credentials.size() - 1) % PAGE_SIZE;
        credentialsTable.setRowSelectionInterval(newRow, newRow);
    }

    /**
     * Handles the action of editing an existing credential.
     */
    private void onEditCredential() {
        int tableRow = credentialsTable.getSelectedRow();
        if (tableRow < 0) return;
        int credIdx = credIdx(credentialsTable.convertRowIndexToModel(tableRow));
        SavedCredential cred = credentials.get(credIdx);
        String oldLabel = cred.label;
        SavedCredentialDialog dialog = new SavedCredentialDialog(mainPanel, cred);
        if (!dialog.showAndGet()) return;

        if (!cred.label.equalsIgnoreCase(oldLabel)) {
            int dupIdx = indexOfCredentialByLabel(cred.label, cred);
            if (dupIdx >= 0) {
                int choice = JOptionPane.showConfirmDialog(mainPanel,
                        "A credential with label \"" + cred.label + "\" already exists. Replace it?",
                        "Duplicate Label", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) {
                    cred.label = oldLabel;
                    refreshPage();
                    return;
                }
                SavedCredential replaced = credentials.remove(dupIdx);
                if (dupIdx < credIdx) credIdx--;
                ApplicationManager.getApplication().executeOnPooledThread(() ->
                        VaultCredentialManager.deleteCredentialById(replaced.id));
            }
        }

        if (cred.isDefault) credentials.forEach(c -> { if (c != cred) c.isDefault = false; });
        currentPage = credIdx / PAGE_SIZE;
        refreshPage();
        credentialsTable.setRowSelectionInterval(credIdx % PAGE_SIZE, credIdx % PAGE_SIZE);
    }

    /**
     * Finds the index of a credential by its label, optionally excluding a specific credential.
     *
     * @param label   The label to search for.
     * @param exclude The credential to exclude from the search, or null.
     * @return The index of the credential, or -1 if not found.
     */
    private int indexOfCredentialByLabel(String label, SavedCredential exclude) {
        for (int i = 0; i < credentials.size(); i++) {
            SavedCredential c = credentials.get(i);
            if (c != exclude && label.equalsIgnoreCase(c.label)) return i;
        }
        return -1;
    }

    /**
     * Handles the action of removing a credential.
     */
    private void onRemoveCredential() {
        int tableRow = credentialsTable.getSelectedRow();
        if (tableRow < 0) return;
        SavedCredential cred = credentials.remove(credIdx(credentialsTable.convertRowIndexToModel(tableRow)));
        VaultCredentialManager.deleteCredentialById(cred.id);
        int totalPages = Math.max(1, (int) Math.ceil((double) credentials.size() / PAGE_SIZE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        refreshPage();
    }

    /**
     * Handles the action of setting a credential as default.
     */
    private void onSetDefault() {
        int tableRow = credentialsTable.getSelectedRow();
        if (tableRow < 0) return;
        int target = credIdx(credentialsTable.convertRowIndexToModel(tableRow));
        for (int i = 0; i < credentials.size(); i++) credentials.get(i).isDefault = (i == target);
        refreshPage();
    }

    /**
     * Calculates the actual credential index from the table row index.
     *
     * @param tableRow The table row index.
     * @return The actual credential index.
     */
    private int credIdx(int tableRow) {
        return currentPage * PAGE_SIZE + tableRow;
    }

    /**
     * Refreshes the currently displayed page of credentials in the table.
     */
    private void refreshPage() {
        tableModel.setRowCount(0);
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, credentials.size());
        for (int i = start; i < end; i++) tableModel.addRow(toRow(credentials.get(i)));
        updatePageControls();
    }

    /**
     * Updates the state and visibility of the pagination controls.
     */
    private void updatePageControls() {
        if (prevPageButton == null) return;
        boolean hasPaging = credentials.size() > PAGE_SIZE;
        int totalPages = hasPaging ? (int) Math.ceil((double) credentials.size() / PAGE_SIZE) : 1;
        prevPageButton.setEnabled(currentPage > 0);
        nextPageButton.setEnabled(currentPage < totalPages - 1);
        pageLabel.setText((currentPage + 1) + " / " + totalPages);
        prevPageButton.setVisible(hasPaging);
        nextPageButton.setVisible(hasPaging);
        pageLabel.setVisible(hasPaging);
    }

    /**
     * Sorts the credentials list based on the selected column and sort order.
     */
    private void sortCredentials() {
        if (sortColumn < 0) return;
        Comparator<SavedCredential> cmp = getColumnComparator(sortColumn);
        credentials.sort(sortAscending ? cmp : cmp.reversed());
    }

    /**
     * Returns a comparator for the given column index.
     *
     * @param col The column index.
     * @return The comparator for the column.
     */
    private Comparator<SavedCredential> getColumnComparator(int col) {
        switch (col) {
            case 0: return Comparator.comparing((SavedCredential c) -> !c.isDefault);
            case 1: return Comparator.comparing((SavedCredential c) -> c.label != null ? c.label.toLowerCase() : "");
            case 2: return Comparator.comparing((SavedCredential c) -> c.vaultDNS != null ? c.vaultDNS.toLowerCase() : "");
            case 3: return Comparator.comparing((SavedCredential c) -> c.username != null ? c.username.toLowerCase() : "");
            case 4: return Comparator.comparing((SavedCredential c) -> authTypeLabel(c.authenticationType).toLowerCase());
            default: return Comparator.comparing((SavedCredential c) -> c.label != null ? c.label.toLowerCase() : "");
        }
    }

    /**
     * Refreshes all rows in the credentials table.
     */
    private void refreshAllRows() {
        refreshPage();
    }

    /**
     * Updates a specific row in the credentials table.
     *
     * @param credIdx The index of the credential.
     * @param cred    The credential object.
     */
    private void updateRow(int credIdx, SavedCredential cred) {
        int tableRow = credIdx - currentPage * PAGE_SIZE;
        if (tableRow >= 0 && tableRow < tableModel.getRowCount()) {
            tableModel.setValueAt(cred.isDefault, tableRow, 0);
            tableModel.setValueAt(cred.label, tableRow, 1);
            tableModel.setValueAt(cred.vaultDNS, tableRow, 2);
            tableModel.setValueAt(cred.username, tableRow, 3);
            tableModel.setValueAt(authTypeLabel(cred.authenticationType), tableRow, 4);
        }
    }

    /**
     * Converts a credential object to an array of objects for the table row.
     *
     * @param cred The credential object.
     * @return The array of objects for the table row.
     */
    private static Object[] toRow(SavedCredential cred) {
        return new Object[]{cred.isDefault, cred.label, cred.vaultDNS, cred.username, authTypeLabel(cred.authenticationType)};
    }

    /**
     * Returns a label string for the given authentication type.
     *
     * @param type The authentication type.
     * @return The label string.
     */
    private static String authTypeLabel(Vault.AuthenticationType type) {
        return type == Vault.AuthenticationType.SESSION_ID ? "Session" : "Basic";
    }

    /**
     * Gets the list of saved credentials.
     *
     * @return The list of saved credentials.
     */
    public List<SavedCredential> getSavedCredentials() {
        return new ArrayList<>(credentials);
    }

    /**
     * Sets the list of saved credentials.
     *
     * @param savedCredentials The list of saved credentials to set.
     */
    public void setSavedCredentials(List<SavedCredential> savedCredentials) {
        credentials.clear();
        currentPage = 0;
        credentials.addAll(savedCredentials);
        refreshPage();
    }

    /**
     * Returns the main settings panel.
     *
     * @return The {@link JPanel} containing settings components.
     */
    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * Returns the component that should receive initial focus.
     *
     * @return The {@link JComponent} for Vault DNS.
     */
    public JComponent getPreferredFocusedComponent() {
        return vaultDnsField;
    }

    /**
     * Gets the default username.
     *
     * @return The username string.
     */
    public String getUsername() {
        return usernameField.getText();
    }

    /**
     * Sets the default username.
     *
     * @param username The username to set.
     */
    public void setUsername(String username) {
        usernameField.setText(username);
    }

    /**
     * Gets the maximum number of CSV rows to process or display.
     *
     * @return The maximum CSV rows as an integer.
     */
    public int getCsvMaxRows() {
        try {
            return Integer.parseInt(csvMaxRowsField.getText());
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    /**
     * Sets the maximum number of CSV rows to process or display.
     *
     * @param csvMaxRows The maximum number of rows.
     */
    public void setCsvMaxRows(int csvMaxRows) {
        csvMaxRowsField.setText(String.valueOf(csvMaxRows));
    }

    /**
     * Gets the connection timeout value in seconds.
     *
     * @return The timeout in seconds.
     */
    public int getConnectionTimeout() {
        return (Integer) connectionTimeoutField.getValue();
    }

    /**
     * Sets the connection timeout value in seconds.
     *
     * @param connectionTimeout The timeout in seconds.
     */
    public void setConnectionTimeout(int connectionTimeout) {
        connectionTimeoutField.setValue(connectionTimeout);
    }

    /**
     * Gets the default Vault DNS.
     *
     * @return The Vault DNS string.
     */
    public String getVaultDns() {
        return vaultDnsField.getText();
    }

    /**
     * Sets the default Vault DNS.
     *
     * @param vaultDNS The Vault DNS to set.
     */
    public void setVaultDns(String vaultDNS) {
        vaultDnsField.setText(vaultDNS);
    }

    /**
     * Gets the auto-connect setting.
     *
     * @return true if auto-connect is enabled, false otherwise.
     */
    public boolean getAutoConnectField() {
        return autoConnectField.isSelected();
    }

    /**
     * Sets the auto-connect setting.
     *
     * @param autoConnect true to enable auto-connect, false otherwise.
     */
    public void setAutoConnectField(boolean autoConnect) {
        autoConnectField.setSelected(autoConnect);
    }

    /**
     * Gets whether to allow all SSL certificates.
     *
     * @return true if all certificates are allowed, false otherwise.
     */
    public boolean getAllowAllCertificates() {
        return allowAllCertificatesField.isSelected();
    }

    /**
     * Sets whether to allow all SSL certificates.
     *
     * @param allowAllCertificates true to allow all certificates, false otherwise.
     */
    public void setAllowAllCertificates(boolean allowAllCertificates) {
        allowAllCertificatesField.setSelected(allowAllCertificates);
    }

    /**
     * Gets the selected authentication type.
     *
     * @return The selected {@link Vault.AuthenticationType}.
     */
    public Vault.AuthenticationType getAuthenticationType() {
        if (basicAuthField.isSelected()) {
            return Vault.AuthenticationType.BASIC;
        } else if (sessionIdField.isSelected()) {
            return Vault.AuthenticationType.SESSION_ID;
        }
        return Vault.AuthenticationType.BASIC;
    }

    /**
     * Sets the authentication type and updates related UI components.
     *
     * @param authenticationType The {@link Vault.AuthenticationType} to set.
     */
    public void setAuthenticationType(Vault.AuthenticationType authenticationType) {
        if (authenticationType == null || authenticationType == Vault.AuthenticationType.BASIC) {
            basicAuthField.setSelected(true);
        } else if (authenticationType == Vault.AuthenticationType.SESSION_ID) {
            sessionIdField.setSelected(true);
        }
        changeAuthenticationType();
    }

    /**
     * Adjusts visibility and labels of UI components based on the selected authentication type.
     */
    private void changeAuthenticationType() {
        if (basicAuthField.isSelected()) {
            usernameField.setEnabled(true);
            usernameField.setVisible(true);
        } else if (sessionIdField.isSelected()) {
            usernameField.setEnabled(false);
            usernameField.setText("");
            usernameField.setVisible(false);
        }
    }

    private static class CredentialTableModel extends DefaultTableModel {
        static final String[] COLUMNS = {"Default", "Label", "Vault DNS", "Username", "Auth Type"};

        /**
         * Constructs a CredentialTableModel with default columns.
         */
        CredentialTableModel() {
            super(COLUMNS, 0);
        }

        /**
         * Returns whether the cell at the specified row and column is editable.
         *
         * @param row    The row index.
         * @param column The column index.
         * @return false, as cells are not editable.
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        /**
         * Returns the class of the data in the specified column.
         *
         * @param columnIndex The column index.
         * @return The class of the data.
         */
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }
    }
}
