package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.toolbox.intellij.metadata.MetadataService;
import com.veeva.vault.toolbox.intellij.language.vql.VqlFormatter;
import com.veeva.vault.toolbox.intellij.language.vql.VqlLanguage;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.settings.AppSettings;
import com.veeva.vault.toolbox.intellij.tasks.ExportVqlCsvTask;
import com.veeva.vault.toolbox.intellij.tasks.RunVqlQueryTask;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import icons.ToolboxIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Interactive console for running read-only VQL queries against the connected Vault
 * and viewing the results in a grid. The editor (with VQL syntax highlighting and
 * object/field completion) sits above the results in a vertical split; pagination is
 * driven by the response's {@code nextPage}/{@code previousPage} details. VQL cannot
 * mutate data, so this panel is safe by construction.
 */
public class VqlConsolePanel extends ToolboxPanel {

    private static final int MAX_HISTORY = 25;
    private static final String TEMPLATE = "SELECT id, name__v\nFROM <object__v>\nWHERE ...";

    private final LanguageTextField editor;
    private final ComboBox<String> historyCombo = new ComboBox<>();
    private final JButton buildQueryButton = new JButton("Build Query", ToolboxIcons.Hammer);
    private final JButton formatButton = new JButton(ToolboxIcons.Code);
    private final JButton runButton = new JButton("Run", ToolboxIcons.Terminal);
    private final JButton cancelButton = new JButton("Cancel", ToolboxIcons.Close);
    private final Component cancelStrut = Box.createHorizontalStrut(6);
    private final JButton revealSchemaButton = new JButton("Schema", ToolboxIcons.Stack);
    private final JButton exportButton = new JButton("Export CSV", ToolboxIcons.Download);
    private final JBLabel errorBanner = new JBLabel();

    private final JButton prevButton = new JButton("◀ Prev");
    private final JButton nextButton = new JButton("Next ▶");
    private final JButton saveButton = new JButton("Save Changes", ToolboxIcons.Upload);

    private final JPanel resultsContainer = new JPanel(new BorderLayout());
    private final VqlResultsTable resultsTable = new VqlResultsTable();


    private final List<String> history = new ArrayList<>();
    private boolean updatingHistory = false;

    private String activeVql = "";
    private List<String> currentColumns = new ArrayList<>();
    private List<QueryResponse.QueryResult> currentRows = new ArrayList<>();
    private String nextPageUrl;
    private String prevPageUrl;
    private int currentPage = 1;

    private long lastElapsedMillis = -1;
    private boolean running = false;
    private RunVqlQueryTask activeTask;

    /**
     * Builds the VQL Console UI for the given toolbox project.
     *
     * @param project the toolbox project context
     */
    public VqlConsolePanel(ToolboxProject project) {
        super(project);
        editor = new LanguageTextField(VqlLanguage.INSTANCE, project.getProject(), VqlFormatter.format(TEMPLATE), false);
        initUI();
        loadHistory();
        setRunning(false);
        updatePager();
        VqlConsoleService.getInstance(project.getProject()).register(this);
    }

    /**
     * Prefills the given VQL into the editor and brings the console into view (selects its tab and
     * reveals the tool window). Invoked by other surfaces such as the Schema Explorer. The query is
     * not auto-run — the user reviews and runs it.
     */
    public void openWithQuery(String vql) {
        if (vql != null && !vql.isBlank()) {
            editor.setText(VqlFormatter.format(vql));
        }
        JTabbedPane tabs = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, this);
        if (tabs != null) {
            tabs.setSelectedComponent(this);
        }
        if (toolboxProject.getToolWindow() != null) {
            toolboxProject.getToolWindow().activate(null);
        }
    }

    /**
     * Reveals the object the user is working with in the Schema Explorer: the selected text if it
     * names an object/picklist, otherwise the object in the query's {@code FROM} clause. The Schema
     * Explorer decides whether the name resolves and surfaces a hint if it does not.
     */
    private void revealInSchema() {
        if (!toolboxProject.isConnected() && !toolboxProject.connectWithDialog()) {
            return;
        }

        Editor activeEditor = editor.getEditor();
        String selected = activeEditor != null && activeEditor.getSelectionModel().getSelectedText() != null
                ? activeEditor.getSelectionModel().getSelectedText().trim() : null;
        String target = selected != null && !selected.isBlank() ? selected : objectInFrom(editor.getText());
        if (target == null || target.isBlank()) {
            Messages.showInfoMessage(toolboxProject.getProject(),
                    "Select an object/picklist name, or include a FROM clause, to reveal it in the Schema Explorer.",
                    "Reveal in Schema");
            return;
        }
        SchemaExplorerService.getInstance(toolboxProject.getProject()).reveal(target);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JBSplitter splitter = new JBSplitter(true, 0.4f);
        splitter.setFirstComponent(buildEditorPanel());
        splitter.setSecondComponent(buildResultsPanel());

        add(buildToolbar(), BorderLayout.NORTH);
        add(splitter, BorderLayout.CENTER);
        add(buildPager(), BorderLayout.SOUTH);
    }

    private JComponent buildToolbar() {
        historyCombo.setToolTipText("Recent queries");

        historyCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String query) {
                    setText(previewText(query));
                    setToolTipText(tooltipHtml(query));
                }
                return this;
            }
        });
        historyCombo.addActionListener(e -> {
            if (updatingHistory) {
                return;
            }
            Object selected = historyCombo.getSelectedItem();
            if (selected instanceof String && !((String) selected).isEmpty()) {
                editor.setText(VqlFormatter.format((String) selected));
            }
        });

        buildQueryButton.setToolTipText("Build a query visually");
        buildQueryButton.addActionListener(e -> openQueryBuilder());

        runButton.setToolTipText("Run query, or the selected text (" + shortcutText() + ")");
        runButton.addActionListener(e -> runQuery(null));
        cancelButton.addActionListener(e -> {
            if (activeTask != null) {
                activeTask.requestCancel();
            }
        });
        revealSchemaButton.setToolTipText("Reveal the selected or queried object in the Schema Explorer");
        revealSchemaButton.addActionListener(e -> revealInSchema());

        JPanel historyRow = new JPanel(new BorderLayout(6, 0));
        historyRow.add(new JBLabel("History:"), BorderLayout.WEST);
        historyRow.add(historyCombo, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonRow.add(buildQueryButton);
        buttonRow.add(Box.createHorizontalStrut(6));
        buttonRow.add(runButton);
        buttonRow.add(cancelStrut);
        buttonRow.add(cancelButton);
        buttonRow.add(Box.createHorizontalStrut(6));
        buttonRow.add(revealSchemaButton);

        JPanel toolbar = new JPanel(new BorderLayout(0, 4));
        toolbar.setBorder(JBUI.Borders.empty(2, 4));
        toolbar.add(historyRow, BorderLayout.NORTH);
        toolbar.add(buttonRow, BorderLayout.CENTER);
        return toolbar;
    }

    private void openQueryBuilder() {
        if (!toolboxProject.isConnected() && !toolboxProject.connectWithDialog()) {
            return;
        }
        MetadataService service = MetadataService.getInstance(toolboxProject.getProject());
        if (service.getIndex().isReady() && !service.getIndex().objectNames().isEmpty()) {
            showQueryBuilder();
            return;
        }

        new Task.Modal(toolboxProject.getProject(), "Loading Vault Objects", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                service.refreshAsync(false);
                long deadline = System.currentTimeMillis() + 60_000;
                while (!service.getIndex().isReady()) {
                    indicator.checkCanceled();
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (System.currentTimeMillis() > deadline) {
                        return;
                    }
                }
            }

            @Override
            public void onSuccess() {
                if (service.getIndex().objectNames().isEmpty()) {
                    Messages.showWarningDialog(toolboxProject.getProject(),
                            "Could not load Vault objects. Please try again.", "Build Query");
                } else {
                    showQueryBuilder();
                }
            }
        }.queue();
    }

    private void showQueryBuilder() {
        QueryBuilderDialog dialog = new QueryBuilderDialog(toolboxProject);
        if (dialog.showAndGet()) {
            String vql = dialog.getQuery();
            if (vql != null && !vql.isBlank()) {
                editor.setText(VqlFormatter.format(vql));
            }
        }
    }

    /**
     * Builds the editor panel component.
     *
     * @return the editor panel component
     */
    private JComponent buildEditorPanel() {
        editor.setOneLineMode(false);
        editor.setPreferredSize(new Dimension(600, 120));

        editor.addSettingsProvider(editorEx -> {
            editorEx.setVerticalScrollbarVisible(true);
            editorEx.setHorizontalScrollbarVisible(true);
        });

        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                runQuery(null);
            }
        }.registerCustomShortcutSet(
                new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, menuMask)), editor);


        formatButton.setText(null);
        formatButton.setToolTipText("Format");
        formatButton.setFocusable(false);
        formatButton.setMargin(JBUI.insets(2));
        formatButton.putClientProperty("JButton.buttonType", "square");
        formatButton.setPreferredSize(new Dimension(28, 28));
        formatButton.addActionListener(e -> {
            editor.setText(VqlFormatter.format(editor.getText()));
        });


        JPanel panel = new JPanel(null) {
            @Override
            public boolean isOptimizedDrawingEnabled() {
                return false;
            }

            @Override
            public void doLayout() {
                editor.setBounds(0, 0, getWidth(), getHeight());
                Dimension pref = formatButton.getPreferredSize();
                formatButton.setBounds(getWidth() - pref.width - JBUI.scale(25), JBUI.scale(4),
                        pref.width, pref.height);
            }

            @Override
            public Dimension getPreferredSize() {
                return editor.getPreferredSize();
            }
        };
        panel.add(formatButton);
        panel.add(editor);
        panel.setComponentZOrder(formatButton, 0);
        return panel;
    }

    private JComponent buildResultsPanel() {
        errorBanner.setForeground(JBColor.RED);
        errorBanner.setBorder(JBUI.Borders.empty(4, 6));
        errorBanner.setVisible(false);

        JLabel placeholder = new JLabel("Run a query to see results (" + shortcutText() + ")", SwingConstants.CENTER);
        placeholder.setForeground(JBColor.GRAY);
        resultsContainer.add(placeholder, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(errorBanner, BorderLayout.NORTH);
        panel.add(resultsContainer, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Builds the pager component.
     *
     * @return the pager component
     */
    private JComponent buildPager() {
        JPanel pager = new JPanel(new BorderLayout());
        pager.setBorder(JBUI.Borders.empty(2, 6));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        prevButton.addActionListener(e -> {
            if (prevPageUrl != null) {
                loadPage(prevPageUrl, currentPage - 1);
            }
        });
        nextButton.addActionListener(e -> {
            if (nextPageUrl != null) {
                loadPage(nextPageUrl, currentPage + 1);
            }
        });
        left.add(prevButton);
        left.add(nextButton);

        exportButton.setToolTipText("Export results to CSV");
        exportButton.addActionListener(e -> exportCsv());
        
        saveButton.setToolTipText("Save edited cells to Vault");
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveChanges());
        
        resultsTable.setOnDirtyStateChanged(dirty -> saveButton.setEnabled(dirty));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.add(saveButton);
        right.add(exportButton);

        pager.add(left, BorderLayout.WEST);
        pager.add(right, BorderLayout.EAST);
        return pager;
    }

    private void saveChanges() {
        if (toolboxProject.isProductionVault()) {
            Message message = toolboxProject.newMessage();
            message.append("This tool cannot be run in a Production domain.");
            message.showError();
            return;
        }

        resultsTable.stopCellEditing();
        if (!resultsTable.hasPendingEdits()) return;
        
        String objectName = objectInFrom(activeVql);
        if (objectName == null || objectName.isEmpty()) {
            showError("Could not determine the object name from the query.");
            return;
        }

        List<java.util.Map<String, String>> recordsToUpdate = new ArrayList<>();
        java.util.Map<Integer, java.util.Map<String, String>> edits = resultsTable.getPendingEdits();
        int idColumn = currentColumns.indexOf("id");
        if (idColumn < 0) {
            showError("Query must include the 'id' column to update records.");
            return;
        }

        java.util.Set<String> allEditedKeys = new java.util.LinkedHashSet<>();
        for (java.util.Map<String, String> rowEdit : edits.values()) {
            allEditedKeys.addAll(rowEdit.keySet());
        }

        for (java.util.Map.Entry<Integer, java.util.Map<String, String>> rowEdit : edits.entrySet()) {
            int modelRow = rowEdit.getKey();
            QueryResponse.QueryResult row = currentRows.get(modelRow);
            String recordId = ExportVqlCsvTask.cellString(row.get("id"));
            
            java.util.Map<String, String> rowUpdates = new java.util.HashMap<>();
            rowUpdates.put("id", recordId);
            
            for (String key : allEditedKeys) {
                if (rowEdit.getValue().containsKey(key)) {
                    rowUpdates.put(key, rowEdit.getValue().get(key));
                } else {
                    rowUpdates.put(key, ExportVqlCsvTask.cellString(row.get(key)));
                }
            }
            recordsToUpdate.add(rowUpdates);
        }

        setRunning(true);
        new com.veeva.vault.toolbox.intellij.tasks.UpdateRecordsTask(toolboxProject.getProject(), objectName, recordsToUpdate,
            msg -> {
                for (java.util.Map.Entry<Integer, java.util.Map<String, String>> rowEdit : edits.entrySet()) {
                    int modelRow = rowEdit.getKey();
                    if (modelRow >= 0 && modelRow < currentRows.size()) {
                        QueryResponse.QueryResult row = currentRows.get(modelRow);
                        for (java.util.Map.Entry<String, String> colEdit : rowEdit.getValue().entrySet()) {
                            row.set(colEdit.getKey(), colEdit.getValue());
                        }
                    }
                }
                resultsTable.commitPendingEdits();
                JOptionPane.showMessageDialog(this, msg, "Update Successful", JOptionPane.INFORMATION_MESSAGE);
            },
            err -> {
                showError(err);
            },
            () -> setRunning(false)
        ).queue();
    }



    private void runQuery(String vqlOverride) {
        if (running) {
            return;
        }
        String vql = vqlOverride != null ? vqlOverride : currentQueryText();
        if (vql.isEmpty()) {
            return;
        }
        if (!toolboxProject.isConnected() && !toolboxProject.connectWithDialog()) {
            return;
        }

        activeVql = vql;
        addToHistory(vql);
        clearError();
        setRunning(true);

        activeTask = new RunVqlQueryTask(
                toolboxProject.getProject(), vql, null,
                (response, elapsed) -> showResults(response, 1, elapsed),
                this::showError,
                () -> setRunning(false));
        activeTask.queue();
    }

    /** The selected text in the editor if there is a selection, otherwise the whole query. */
    private String currentQueryText() {
        Editor activeEditor = editor.getEditor();
        if (activeEditor != null) {
            String selected = activeEditor.getSelectionModel().getSelectedText();
            if (selected != null && !selected.isBlank()) {
                return selected.trim();
            }
        }
        return editor.getText().trim();
    }

    private void loadPage(String pageUrl, int pageNumber) {
        if (running || pageUrl == null) {
            return;
        }
        clearError();
        setRunning(true);

        activeTask = new RunVqlQueryTask(
                toolboxProject.getProject(), null, pageUrl,
                (response, elapsed) -> showResults(response, pageNumber, elapsed),
                this::showError,
                () -> setRunning(false));
        activeTask.queue();
    }

    private void showResults(QueryResponse response, int pageNumber, long elapsedMillis) {
        lastElapsedMillis = elapsedMillis;
        currentRows = response.getData() != null ? response.getData() : new ArrayList<>();
        currentColumns = deriveColumns(activeVql, currentRows);
        currentPage = pageNumber;

        QueryResponse.ResponseDetails details = response.getResponseDetails();
        nextPageUrl = details != null ? details.getNextPage() : null;
        prevPageUrl = details != null ? details.getPreviousPage() : null;

        renderGrid();
        updatePager();
    }

    private void renderGrid() {
        resultsContainer.removeAll();
        if (currentRows.isEmpty()) {
            JLabel empty = new JLabel("No rows returned", SwingConstants.CENTER);
            empty.setForeground(JBColor.GRAY);
            resultsContainer.add(empty, BorderLayout.CENTER);
        } else {
            List<String[]> rows = new ArrayList<>(currentRows.size());
            for (QueryResponse.QueryResult row : currentRows) {
                String[] cells = new String[currentColumns.size()];
                for (int i = 0; i < currentColumns.size(); i++) {
                    cells[i] = ExportVqlCsvTask.cellString(row.get(currentColumns.get(i)));
                }
                rows.add(cells);
            }
            String object = objectInFrom(activeVql);
            String dns = toolboxProject.getVaultDNS();
            String baseUrl = (dns != null && !dns.isEmpty()) ? "https://" + dns : null;
            resultsTable.setData(currentColumns, rows, currentRows, object, baseUrl);
            resultsTable.setQueryTime(lastElapsedMillis);
            resultsContainer.add(resultsTable, BorderLayout.CENTER);
        }
        resultsContainer.revalidate();
        resultsContainer.repaint();
        exportButton.setEnabled(!currentRows.isEmpty());
    }

    private void updatePager() {
        prevButton.setEnabled(!running && prevPageUrl != null);
        nextButton.setEnabled(!running && nextPageUrl != null);
    }



    private void setRunning(boolean isRunning) {
        this.running = isRunning;
        runButton.setEnabled(!isRunning);
        cancelButton.setVisible(isRunning);
        cancelStrut.setVisible(isRunning);
        editor.setEnabled(!isRunning);
        historyCombo.setEnabled(!isRunning);
        buildQueryButton.setEnabled(!isRunning);
        exportButton.setEnabled(!isRunning && !currentRows.isEmpty());
        prevButton.setEnabled(!isRunning && prevPageUrl != null);
        nextButton.setEnabled(!isRunning && nextPageUrl != null);
    }

    /**
     * Displays an error message in the console.
     *
     * @param message the error message to display
     */
    private void showError(String message) {
        String safeMessage = message != null ? message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", " ") : "Unknown Error";
        errorBanner.setText("<html>" + safeMessage.trim() + "</html>");
        errorBanner.setToolTipText(message);
        errorBanner.setVisible(true);
    }

    private void clearError() {
        errorBanner.setVisible(false);
        errorBanner.setText("");
    }



    private void loadHistory() {
        List<String> saved = AppSettings.getInstance().getState().vqlHistory;
        if (saved != null) {
            history.addAll(saved);
        }
        rebuildHistoryCombo(null);
    }

    /**
     * Adds a VQL query to the history.
     *
     * @param vql the VQL query to add
     */
    private void addToHistory(String vql) {
        history.remove(vql);
        history.add(0, vql);
        while (history.size() > MAX_HISTORY) {
            history.remove(history.size() - 1);
        }
        AppSettings.getInstance().getState().vqlHistory = new ArrayList<>(history);
        rebuildHistoryCombo(vql);
    }

    private void rebuildHistoryCombo(String selected) {
        updatingHistory = true;
        historyCombo.removeAllItems();
        for (String item : history) {
            historyCombo.addItem(item);
        }
        if (selected != null) {
            historyCombo.setSelectedItem(selected);
        } else {
            historyCombo.setSelectedIndex(-1);
        }
        updatingHistory = false;
    }



    private void exportCsv() {
        if (running || currentRows.isEmpty()) {
            return;
        }
        FileSaverDescriptor descriptor = new FileSaverDescriptor("Export VQL Results", "Save results to a CSV file", "csv");
        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, toolboxProject.getProject());
        VirtualFileWrapper wrapper = dialog.save((com.intellij.openapi.vfs.VirtualFile) null, "vql_results.csv");
        if (wrapper == null) {
            return;
        }
        File file = wrapper.getFile();
        int maxRows = AppSettings.getInstance().getState().vqlMaxExportRows;
        List<String> columns = new ArrayList<>(currentColumns);

        clearError();
        setRunning(true);
        new ExportVqlCsvTask(
                toolboxProject.getProject(), activeVql, columns, file, maxRows,
                count -> JOptionPane.showMessageDialog(this,
                        "Exported " + count + " rows to " + file.getName(),
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE),
                this::showError,
                () -> setRunning(false)).queue();
    }



    /**
     * Derives the result columns in {@code SELECT}-clause order. Falls back to the
     * union of field names across the returned rows when the clause cannot be parsed
     * reliably (e.g. {@code SELECT *}, functions, or relationship fields), since the
     * underlying model is map-backed and does not preserve column order on its own.
     */
    static List<String> deriveColumns(String vql, List<? extends QueryResponse.QueryResult> rows) {
        List<String> parsed = parseSelectColumns(vql);
        if (parsed != null && !parsed.isEmpty()) {
            return parsed;
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (QueryResponse.QueryResult row : rows) {
            keys.addAll(row.getFieldNames());
        }
        return new ArrayList<>(keys);
    }

    /**
     * Parses the top-level field list between {@code SELECT} and the matching
     * {@code FROM} (ignoring sub-selects). Returns {@code null} to signal that the
     * caller should fall back to row-key derivation.
     */
    static List<String> parseSelectColumns(String vql) {
        if (vql == null) {
            return null;
        }
        String lower = vql.toLowerCase();
        int select = indexOfWord(lower, "select", 0);
        if (select < 0) {
            return null;
        }
        int start = select + "select".length();

        int depth = 0;
        int fromIdx = -1;
        for (int i = start; i < vql.length(); i++) {
            char c = vql.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && isWordAt(lower, "from", i)) {
                fromIdx = i;
                break;
            }
        }
        if (fromIdx < 0) {
            return null;
        }

        String fieldPart = vql.substring(start, fromIdx).trim();
        if (fieldPart.isEmpty() || fieldPart.equals("*")) {
            return null;
        }

        List<String> columns = new ArrayList<>();
        for (String token : splitTopLevel(fieldPart)) {
            String column = columnNameFromToken(token.trim());
            if (column == null) {
                return null;
            }
            columns.add(column);
        }
        return columns.isEmpty() ? null : columns;
    }

    /**
     * Extracts the output column name from a single SELECT token. Handles a trailing
     * {@code AS alias}; otherwise accepts only a simple identifier. Anything more
     * complex (functions, relationship paths) yields {@code null} so the whole query
     * falls back to row-key derivation.
     */
    private static String columnNameFromToken(String token) {
        if (token.isEmpty()) {
            return null;
        }
        int as = indexOfWord(token.toLowerCase(), "as", 0);
        if (as >= 0) {
            String alias = stripQuotes(token.substring(as + "as".length()).trim());
            return alias.isEmpty() ? null : alias;
        }
        return token.matches("[A-Za-z_][A-Za-z0-9_]*") ? token : null;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("'") && value.endsWith("'"))
                || (value.startsWith("\"") && value.endsWith("\"")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /** @return the object referenced by the first top-level {@code FROM}, or {@code null}. */
    private static String objectInFrom(String vql) {
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("(?i)\\bfrom\\s+([A-Za-z_][A-Za-z0-9_]*)").matcher(vql);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** Splits on top-level commas, ignoring commas inside parentheses. */
    private static List<String> splitTopLevel(String input) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int last = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(input.substring(last, i));
                last = i + 1;
            }
        }
        parts.add(input.substring(last));
        return parts;
    }

    /** Finds the index of {@code word} as a whole word in {@code lower} at or after {@code from}. */
    private static int indexOfWord(String lower, String word, int from) {
        int idx = lower.indexOf(word, from);
        while (idx >= 0) {
            if (isWordAt(lower, word, idx)) {
                return idx;
            }
            idx = lower.indexOf(word, idx + 1);
        }
        return -1;
    }

    /** Whether {@code word} appears at {@code idx} in {@code lower} bounded by non-word characters. */
    private static boolean isWordAt(String lower, String word, int idx) {
        if (!lower.startsWith(word, idx)) {
            return false;
        }
        boolean leftOk = idx == 0 || !isWordChar(lower.charAt(idx - 1));
        int after = idx + word.length();
        boolean rightOk = after >= lower.length() || !isWordChar(lower.charAt(after));
        return leftOk && rightOk;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** Collapses a (possibly multi-line) query into a single trimmed, truncated preview line. */
    private static String previewText(String query) {
        return query.replaceAll("\\s+", " ").trim();
    }

    /** Renders the full query as an HTML tooltip, preserving line breaks. */
    private static String tooltipHtml(String query) {
        String escaped = query
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html><body style='font-family:monospace'>" + escaped + "</body></html>";
    }

    private static String shortcutText() {
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        return (menuMask == InputEvent.META_DOWN_MASK ? "⌘" : "Ctrl") + "+↵";
    }
}