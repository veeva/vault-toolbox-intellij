package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestPanel;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.CompareEnvironmentsTask;
import com.veeva.vault.toolbox.intellij.ui.CompareEnvironmentsDialog.ComparisonType;
import com.veeva.vault.toolbox.intellij.ui.CompareEnvironmentsDialog.MdlFilter;
import com.veeva.vault.vapil.api.model.response.MdlExecuteResponse;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.response.DomainResponse;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.DomainRequest;
import com.veeva.vault.vapil.api.request.MetaDataRequest;
import com.veeva.vault.vapil.api.request.SDKRequest;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Panel to manage comparison settings and display difference results between two Vaults.
 */
public class CompareEnvironmentsPanel extends JPanel implements Disposable {

    private static final Logger logger = LoggerFactory.getLogger(CompareEnvironmentsPanel.class);
    private static final String FILTER_ALL = "All Files";
    private static final String FILTER_DIFFS = "All Differences";
    private static final String STATUS_IDENTICAL = "Identical";

    /**
     * Data record for a single difference entry.
     */
    private record DiffEntry(ComparisonType comparisonType, String status, String componentType, String name,
                             File sourceFile, File targetFile) {}

    /**
     * Data record for a section node in the comparison tree.
     */
    private record SectionNode(ComparisonType type, int count) {}

    /**
     * Data record for a group node in the comparison tree.
     */
    private record GroupNode(String name, int count) {}

    private final ToolboxProject toolboxProject;

    private JBLabel sourceAuthStatus;
    private JBLabel targetAuthStatus;
    private JBLabel sourceDnsLabel;
    private JBLabel targetDnsLabel;
    private JButton sourceSignInBtn;
    private JButton targetSignInBtn;
    private VaultClient sourceClient;
    private VaultClient targetClient;
    private String sourceClientDns = null;
    private String targetClientDns = null;
    private JCheckBox mdlCheckBox;
    private JComboBox<MdlFilter> mdlFilterCombo;
    private JCheckBox sdkCheckBox;
    private JButton compareButton;
    private JButton cancelButton;
    private JProgressBar loadingBar;
    private ProgressIndicator runningIndicator;

    private final List<DiffEntry> entries = new ArrayList<>();
    private File lastComparisonBaseDir;
    private Tree resultsTree;
    private DefaultTreeModel treeModel;
    private DiffRequestPanel diffPanel;
    private JBLabel statusLabel;
    private JComboBox<String> resultsFilterCombo;
    private SearchTextField searchField;

    private JCheckBox enableApplyBtn;
    private JPanel applyActionStrip;
    private JPanel appliedBanner;
    private JBLabel appliedBannerLabel;
    private JButton applyToTargetBtn;
    private JButton applyToSourceBtn;
    private JButton revertBtn;
    private JComboBox<String> viewModeCombo;
    private boolean applyChangesEnabled = false;
    private boolean applyInProgress = false;
    private boolean isUpdatingViewMode = false;
    private DiffEntry selectedEntry;
    private final Set<String> recentlyApplied = new LinkedHashSet<>();
    private final Map<String, File> backups = new LinkedHashMap<>();
    private final Map<String, Boolean> applyDirections = new LinkedHashMap<>();
    private Map<ComparisonType, File[]> comparisonDirs = new LinkedHashMap<>();


    /**
     * Constructs the panel.
     *
     * @param toolboxProject the current toolbox project
     */
    public CompareEnvironmentsPanel(ToolboxProject toolboxProject) {
        super(new BorderLayout());
        this.toolboxProject = toolboxProject;
        init();
    }

    /**
     * Initializes the panel components.
     */
    private void init() {
        loadingBar = new JProgressBar();
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);

        JPanel northWrapper = new JPanel();
        northWrapper.setLayout(new BoxLayout(northWrapper, BoxLayout.Y_AXIS));
        northWrapper.add(buildSettingsPanel());
        northWrapper.add(loadingBar);

        add(northWrapper, BorderLayout.NORTH);
        add(buildContentSplit(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    /**
     * Builds the settings panel.
     *
     * @return the settings panel component
     */
    private JPanel buildSettingsPanel() {
        sourceDnsLabel = new JBLabel(" ");
        sourceDnsLabel.setForeground(JBColor.namedColor("Label.infoForeground", JBColor.GRAY));
        sourceSignInBtn = new JButton("Sign In");

        sourceAuthStatus = new JBLabel(" ");
        sourceAuthStatus.setFont(sourceAuthStatus.getFont().deriveFont(sourceAuthStatus.getFont().getSize() - 1f));

        targetDnsLabel = new JBLabel(" ");
        targetDnsLabel.setForeground(JBColor.namedColor("Label.infoForeground", JBColor.GRAY));
        targetSignInBtn = new JButton("Sign In");

        targetAuthStatus = new JBLabel(" ");
        targetAuthStatus.setFont(targetAuthStatus.getFont().deriveFont(targetAuthStatus.getFont().getSize() - 1f));

        sourceSignInBtn.addActionListener(e -> {
            VaultAuthDialog dlg = new VaultAuthDialog(toolboxProject.getProject(), toolboxProject);
            if (dlg.showAndGet()) {
                String dns = dlg.getAuthenticatedDns();
                setClient(dlg.getAuthenticatedClient(), true, dns);
                setAuthStatus(sourceAuthStatus, true, "Authenticated");
                sourceDnsLabel.setText(dns);
                sourceDnsLabel.setForeground(UIManager.getColor("Label.foreground"));
                sourceSignInBtn.setText("Change");
                updateCompareButton();
                resetApplyState();
            }
        });
        targetSignInBtn.addActionListener(e -> {
            VaultAuthDialog dlg = new VaultAuthDialog(toolboxProject.getProject(), toolboxProject);
            if (dlg.showAndGet()) {
                String dns = dlg.getAuthenticatedDns();
                setClient(dlg.getAuthenticatedClient(), false, dns);
                setAuthStatus(targetAuthStatus, true, "Authenticated");
                targetDnsLabel.setText(dns);
                targetDnsLabel.setForeground(UIManager.getColor("Label.foreground"));
                targetSignInBtn.setText("Change");
                updateCompareButton();
                resetApplyState();
            }
        });

        mdlCheckBox = new JCheckBox("MDL", true);
        mdlFilterCombo = new JComboBox<>(MdlFilter.values());
        mdlFilterCombo.setSelectedItem(MdlFilter.ALL);
        mdlCheckBox.addActionListener(e -> {
            mdlFilterCombo.setEnabled(mdlCheckBox.isSelected());
            applyResultsFilter();
        });
        mdlFilterCombo.addActionListener(e -> applyResultsFilter());

        sdkCheckBox = new JCheckBox("SDK", false);
        sdkCheckBox.addActionListener(e -> applyResultsFilter());

        compareButton = new JButton("Compare", AllIcons.Actions.Execute);
        compareButton.setEnabled(false);
        compareButton.addActionListener(e -> runComparison());

        cancelButton = new JButton("Stop", AllIcons.Actions.Suspend);
        cancelButton.setVisible(false);
        cancelButton.addActionListener(e -> cancelComparison());

        JPanel sourceRow = buildVaultRow("Source:", sourceDnsLabel, sourceSignInBtn, sourceAuthStatus);
        JPanel targetRow = buildVaultRow("Target:", targetDnsLabel, targetSignInBtn, targetAuthStatus);

        JButton swapBtn = new JButton("⇅ Swap");
        swapBtn.setToolTipText("Swap Source and Target vaults");
        swapBtn.addActionListener(e -> swapVaults());

        JPanel optionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        optionsRow.setOpaque(false);
        optionsRow.add(swapBtn);
        optionsRow.add(Box.createHorizontalStrut(8));
        optionsRow.add(new JBLabel("Compare:"));
        optionsRow.add(mdlCheckBox);
        optionsRow.add(mdlFilterCombo);
        optionsRow.add(Box.createHorizontalStrut(4));
        optionsRow.add(sdkCheckBox);
        optionsRow.add(Box.createHorizontalStrut(8));
        optionsRow.add(compareButton);
        optionsRow.add(cancelButton);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
                JBUI.Borders.empty(8, 10, 8, 10)));

        GridBagConstraints pc = new GridBagConstraints();
        pc.fill = GridBagConstraints.HORIZONTAL;
        pc.weightx = 1.0; pc.gridx = 0;
        pc.gridy = 0; pc.insets = JBUI.insetsBottom(6); panel.add(sourceRow, pc);
        pc.gridy = 1; pc.insets = JBUI.insetsBottom(6); panel.add(targetRow, pc);
        pc.gridy = 2; pc.insets = new Insets(0, -4, 0, 0); panel.add(optionsRow, pc);
        return panel;
    }

    /**
     * Builds a single row for Vault configuration.
     *
     * @param label      the label for the row
     * @param dnsLabel   the label displaying DNS
     * @param signInBtn  the sign in button
     * @param authStatus the label displaying auth status
     * @return the row panel
     */
    private JPanel buildVaultRow(String label, JBLabel dnsLabel, JButton signInBtn, JBLabel authStatus) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.WEST; g.gridy = 0;
        g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE; g.insets = JBUI.insetsRight(6);
        row.add(new JBLabel(label), g);
        g.gridx = 1; g.weightx = 0; g.fill = GridBagConstraints.NONE; g.insets = JBUI.insetsRight(8);
        row.add(signInBtn, g);
        g.gridx = 2; g.weightx = 1.0; g.fill = GridBagConstraints.HORIZONTAL; g.insets = JBUI.insetsRight(8);
        row.add(dnsLabel, g);
        g.gridx = 3; g.weightx = 0; g.fill = GridBagConstraints.NONE; g.insets = JBUI.emptyInsets();
        row.add(authStatus, g);
        return row;
    }

    /**
     * Builds the content split pane comprising the tree and diff viewer.
     *
     * @return the split pane
     */
    private JComponent buildContentSplit() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        treeModel   = new DefaultTreeModel(root);
        resultsTree = new Tree(treeModel);
        resultsTree.setRootVisible(false);
        resultsTree.setShowsRootHandles(true);
        resultsTree.setCellRenderer(new DiffTreeCellRenderer());

        resultsTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) resultsTree.getLastSelectedPathComponent();
            if (node != null && node.isLeaf() && node.getUserObject() instanceof DiffEntry entry) {
                selectedEntry = entry;
                showInlineDiff(entry);
                updateActionBar();
            } else {
                selectedEntry = null;
                updateActionBar();
            }
        });

        diffPanel = DiffManager.getInstance().createRequestPanel(
                toolboxProject.getProject(), this, null);

        appliedBannerLabel = new JBLabel();
        appliedBanner = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        appliedBanner.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0));
        appliedBanner.add(appliedBannerLabel);
        appliedBanner.setVisible(false);

        applyToSourceBtn = new JButton("Target to Source", AllIcons.General.ArrowLeft);
        applyToSourceBtn.setEnabled(false);
        applyToSourceBtn.addActionListener(e -> { if (selectedEntry != null) applyChange(selectedEntry, false); });

        revertBtn = new JButton("Revert", AllIcons.Actions.Rollback);
        revertBtn.setEnabled(false);
        revertBtn.addActionListener(e -> { if (selectedEntry != null) revertChange(selectedEntry); });

        applyToTargetBtn = new JButton("Source to Target", AllIcons.General.ArrowRight);
        applyToTargetBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        applyToTargetBtn.setEnabled(false);
        applyToTargetBtn.addActionListener(e -> { if (selectedEntry != null) applyChange(selectedEntry, true); });

        viewModeCombo = new JComboBox<>(new String[]{"Raw MDL View", "Semantic JSON View"});
        viewModeCombo.addActionListener(e -> { 
            if (!isUpdatingViewMode && selectedEntry != null) {
                showInlineDiff(selectedEntry); 
            }
        });
        viewModeCombo.setEnabled(false);

        enableApplyBtn = new JCheckBox("Enable Apply Changes");
        enableApplyBtn.setToolTipText("Enable applying changes between Vaults (irreversible)");
        enableApplyBtn.addActionListener(e -> {
            if (enableApplyBtn.isSelected()) {
                enableApplyChanges();
            } else {
                disableApplyChanges();
            }
        });

        JPanel viewModeStrip = new JPanel(new BorderLayout(4, 4));
        viewModeStrip.setBorder(JBUI.Borders.empty(2, 6));
        viewModeStrip.add(enableApplyBtn, BorderLayout.WEST);
        
        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        comboPanel.add(viewModeCombo);
        viewModeStrip.add(comboPanel, BorderLayout.EAST);

        applyActionStrip = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        applyActionStrip.setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0));
        applyActionStrip.add(applyToSourceBtn);
        applyActionStrip.add(revertBtn);
        applyActionStrip.add(applyToTargetBtn);
        applyActionStrip.setVisible(false);

        JPanel topBannerPanel = new JPanel(new BorderLayout());
        topBannerPanel.add(viewModeStrip, BorderLayout.NORTH);
        topBannerPanel.add(appliedBanner, BorderLayout.SOUTH);

        JPanel diffWrapper = new JPanel(new BorderLayout());
        diffWrapper.add(topBannerPanel, BorderLayout.NORTH);
        diffWrapper.add(diffPanel.getComponent(), BorderLayout.CENTER);
        diffWrapper.add(applyActionStrip, BorderLayout.SOUTH);

        resultsFilterCombo = new JComboBox<>(new String[]{
                FILTER_DIFFS, FILTER_ALL, "Modified", "Source Only", "Target Only"});
        resultsFilterCombo.addActionListener(e -> applyResultsFilter());

        searchField = new SearchTextField(false);
        searchField.getTextEditor().getEmptyText().setText("Search files…");
        searchField.getTextEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { applyResultsFilter(); }
            @Override public void removeUpdate(DocumentEvent e)  { applyResultsFilter(); }
            @Override public void changedUpdate(DocumentEvent e) {}
        });

        JBLabel treeTitle = new JBLabel("Files");
        treeTitle.setFont(treeTitle.getFont().deriveFont(Font.BOLD));

        JPanel treeHeader = new JPanel(new GridBagLayout());
        treeHeader.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0),
                JBUI.Borders.empty(4, 8, 6, 8)));
        GridBagConstraints hc = new GridBagConstraints();
        hc.fill = GridBagConstraints.HORIZONTAL; hc.weightx = 1.0; hc.gridx = 0;
        hc.gridy = 0; hc.insets = JBUI.insetsBottom(4); treeHeader.add(treeTitle, hc);
        hc.gridy = 1; hc.insets = JBUI.insetsBottom(4); treeHeader.add(resultsFilterCombo, hc);
        hc.gridy = 2; hc.insets = JBUI.emptyInsets();   treeHeader.add(searchField, hc);

        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.add(treeHeader, BorderLayout.NORTH);
        treePanel.add(new JBScrollPane(resultsTree), BorderLayout.CENTER);
        treePanel.setMinimumSize(JBUI.size(400, -1));

        OnePixelSplitter split = new OnePixelSplitter(false, 0.22f);
        split.setFirstComponent(treePanel);
        split.setSecondComponent(diffWrapper);
        split.setBorder(JBUI.Borders.empty());
        return split;
    }


    /**
     * Builds the status bar component.
     *
     * @return the status bar component
     */
    private JComponent buildStatusBar() {
        statusLabel = new JBLabel(" ");
        statusLabel.setForeground(JBColor.namedColor("Label.infoForeground", JBColor.GRAY));
        statusLabel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0),
                JBUI.Borders.empty(4, 10)));
        return statusLabel;
    }

    /**
     * Sets the active Vault client for source or target.
     *
     * @param client   the Vault client
     * @param isSource whether it is the source client
     * @param dns      the DNS string
     */
    private void setClient(VaultClient client, boolean isSource, String dns) {
        if (isSource) { sourceClient = client; sourceClientDns = client != null ? dns : null; }
        else          { targetClient = client; targetClientDns = client != null ? dns : null; }
    }

    /**
     * Sets the text and color of the authentication status label.
     *
     * @param label the label to update
     * @param ok    whether auth is ok
     * @param text  the status text
     */
    private void setAuthStatus(JBLabel label, Boolean ok, String text) {
        if (ok == null) {
            label.setForeground(JBColor.namedColor("Label.infoForeground", JBColor.GRAY));
        } else if (ok) {
            label.setForeground(new JBColor(new Color(0, 128, 0), new Color(98, 150, 85)));
        } else {
            label.setForeground(JBColor.RED);
        }
        label.setText(text);
    }

    /**
     * Updates the enable state of the compare button based on authentication.
     */
    private void updateCompareButton() {
        compareButton.setEnabled(sourceClient != null && targetClient != null);
    }

    /**
     * Initiates the comparison task.
     */
    private void runComparison() {
        String sourceDns = getSourceDns();
        String targetDns = getTargetDns();
        if (sourceDns == null || sourceDns.isBlank()) { setStatus("Please select a source vault."); return; }
        if (targetDns == null || targetDns.isBlank()) { setStatus("Please select a target vault."); return; }
        if (sourceDns.equalsIgnoreCase(targetDns)) {
            com.intellij.openapi.ui.Messages.showErrorDialog(
                    toolboxProject.getProject(),
                    "Source and target environments must be different.\n\nBoth are currently signed in as: " + sourceDns,
                    "Same Environment Selected");
            return;
        }
        if (sourceClient == null || targetClient == null) {
            setStatus("Both vaults must be authenticated before comparing."); return;
        }

        Set<ComparisonType> types = EnumSet.noneOf(ComparisonType.class);
        if (mdlCheckBox.isSelected()) types.add(ComparisonType.MDL);
        if (sdkCheckBox.isSelected()) types.add(ComparisonType.SDK);
        if (types.isEmpty()) {
            setStatus("Please select at least one comparison type."); return;
        }

        if (runningIndicator != null) {
            runningIndicator.cancel();
            runningIndicator = null;
        }

        compareButton.setEnabled(false);
        cancelButton.setVisible(true);
        loadingBar.setVisible(true);
        revalidate();
        setStatus("Downloading and comparing — this may take a moment...");
        entries.clear();
        treeModel.setRoot(new DefaultMutableTreeNode("root"));
        diffPanel.setRequest(null);
        deleteTempDir(lastComparisonBaseDir);
        lastComparisonBaseDir = null;
        resetApplyState();

        CompareEnvironmentsTask task = new CompareEnvironmentsTask(
                toolboxProject.getProject(), types, MdlFilter.ALL,
                sourceDns, sourceClient, targetDns, targetClient,
                this::onResultsReady, this::onComparisonCancelled);
        runningIndicator = new BackgroundableProcessIndicator(task);
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, runningIndicator);
    }

    /**
     * Cancels the currently running comparison task and triggers cleanup.
     */
    private void cancelComparison() {
        if (runningIndicator != null) {
            runningIndicator.cancel();
        }
    }

    /**
     * Callback invoked on the EDT when the task is cancelled by the user.
     * Restores UI state; temp file cleanup is handled by the task itself.
     */
    private void onComparisonCancelled() {
        runningIndicator = null;
        loadingBar.setVisible(false);
        cancelButton.setVisible(false);
        compareButton.setEnabled(sourceClient != null && targetClient != null);
        revalidate();
        setStatus("Comparison cancelled.");
    }

    /**
     * Callback invoked when comparison results are ready.
     *
     * @param results the comparison results
     */
    private void onResultsReady(Map<ComparisonType, File[]> results) {
        ApplicationManager.getApplication().invokeLater(() -> {
            runningIndicator = null;
            entries.clear();
            comparisonDirs = results;
            recentlyApplied.clear();
            deleteAllBackups();
            backups.clear();

            for (var e : results.entrySet()) {
                collectDiffs(e.getKey(), e.getValue()[0], e.getValue()[1]);
                if (lastComparisonBaseDir == null && e.getValue()[0] != null) {
                    lastComparisonBaseDir = e.getValue()[0].getParentFile().getParentFile();
                }
            }

            loadingBar.setVisible(false);
            cancelButton.setVisible(false);
            revalidate();
            resultsFilterCombo.setSelectedItem(FILTER_DIFFS);
            applyResultsFilter();
            compareButton.setEnabled(true);

            if (results.isEmpty()) {
                setStatus("Comparison could not complete. Check the error notification.");
            } else if (entries.isEmpty()) {
                setStatus("No files found.");
            } else {
                long diffCount = entries.stream().filter(e -> !STATUS_IDENTICAL.equals(e.status())).count();
                setStatus(entries.size() + " file(s) compared — "
                        + (diffCount == 0 ? "no differences found." : diffCount + " difference(s). Select a file to view the diff."));
            }
        });
    }

    /**
     * Collects file differences into the entries list.
     *
     * @param type   the comparison type
     * @param srcDir the source directory
     * @param tgtDir the target directory
     */
    private void collectDiffs(ComparisonType type, File srcDir, File tgtDir) {
        if (!srcDir.exists() || !tgtDir.exists()) return;

        String[] extensions = type == ComparisonType.MDL ? new String[]{"mdl"} : new String[]{"java"};

        Collection<File> srcFiles = FileUtils.listFiles(srcDir, extensions, true);
        for (File srcFile : srcFiles) {
            String rel     = srcDir.toURI().relativize(srcFile.toURI()).getPath();
            File   tgtFile = new File(tgtDir, rel);
            String[] parts = parseParts(rel);
            if (!tgtFile.exists()) {
                entries.add(new DiffEntry(type, "Source Only", parts[0], parts[1], srcFile, null));
            } else {
                try {
                    String status = FileUtils.contentEquals(srcFile, tgtFile) ? STATUS_IDENTICAL : "Modified";
                    entries.add(new DiffEntry(type, status, parts[0], parts[1], srcFile, tgtFile));
                } catch (IOException ex) {
                    logger.error("Error comparing {}", rel, ex);
                }
            }
        }

        Collection<File> tgtFiles = FileUtils.listFiles(tgtDir, extensions, true);
        for (File tgtFile : tgtFiles) {
            String rel = tgtDir.toURI().relativize(tgtFile.toURI()).getPath();
            if (!new File(srcDir, rel).exists()) {
                String[] parts = parseParts(rel);
                entries.add(new DiffEntry(type, "Target Only", parts[0], parts[1], null, tgtFile));
            }
        }
    }
    /**
     * Applies UI filters to the collected diff entries and updates the tree.
     */
    private void applyResultsFilter() {
        String statusFilter = (String) resultsFilterCombo.getSelectedItem();
        Object sel = mdlFilterCombo.getSelectedItem();
        MdlFilter mdlFilter = sel instanceof MdlFilter mf ? mf : MdlFilter.ALL;
        boolean showMdl = mdlCheckBox.isSelected();
        boolean showSdk = sdkCheckBox.isSelected();

        String search = searchField.getText().trim().toLowerCase();

        List<DiffEntry> filtered = entries.stream()
                .filter(e -> e.comparisonType() == ComparisonType.MDL ? showMdl : showSdk)
                .filter(e -> e.comparisonType() != ComparisonType.MDL || mdlFilter.accepts(e.name()))
                .filter(e -> {
                    if (recentlyApplied.contains(entryKey(e))) return true;
                    if (FILTER_ALL.equals(statusFilter)) return true;
                    if (FILTER_DIFFS.equals(statusFilter)) return !STATUS_IDENTICAL.equals(e.status());
                    return e.status().equals(statusFilter);
                })
                .filter(e -> search.isEmpty() || e.name().toLowerCase().contains(search))
                .collect(Collectors.toList());
        buildResultsTree(filtered);
    }

    /**
     * Builds the result tree model from filtered entries.
     *
     * @param toShow the list of entries to display
     */
    private void buildResultsTree(List<DiffEntry> toShow) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");

        Map<ComparisonType, List<DiffEntry>> bySection = new LinkedHashMap<>();
        for (DiffEntry e : toShow)
            bySection.computeIfAbsent(e.comparisonType(), k -> new ArrayList<>()).add(e);

        for (var sectionEntry : bySection.entrySet()) {
            List<DiffEntry> sectionEntries = sectionEntry.getValue();
            DefaultMutableTreeNode sectionNode = new DefaultMutableTreeNode(
                    new SectionNode(sectionEntry.getKey(), sectionEntries.size()));

            Map<String, List<DiffEntry>> byType = new LinkedHashMap<>();
            for (DiffEntry e : sectionEntries)
                byType.computeIfAbsent(e.componentType(), k -> new ArrayList<>()).add(e);

            for (var typeEntry : byType.entrySet()) {
                List<DiffEntry> group = typeEntry.getValue();
                DefaultMutableTreeNode typeNode =
                        new DefaultMutableTreeNode(new GroupNode(typeEntry.getKey(), group.size()));
                group.stream()
                     .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                     .forEach(diff -> typeNode.add(new DefaultMutableTreeNode(diff)));
                sectionNode.add(typeNode);
            }
            root.add(sectionNode);
        }

        treeModel.setRoot(root);
        for (int i = 0; i < resultsTree.getRowCount(); i++) resultsTree.expandRow(i);
    }

    /**
     * Parses the relative path to extract type and file name.
     *
     * @param relative the relative file path
     * @return a string array containing type and name
     */
    private String[] parseParts(String relative) {
        int slash = relative.indexOf('/');
        if (slash > 0) {
            String type     = relative.substring(0, slash);
            String fileName = relative.substring(slash + 1);
            String name     = fileName.endsWith(".mdl")
                    ? fileName.substring(0, fileName.length() - 4) : fileName;
            return new String[]{type, name};
        }
        int last = relative.lastIndexOf('/');
        return new String[]{
                last > 0 ? relative.substring(0, last).replace('/', '.') : "SDK",
                last > 0 ? relative.substring(last + 1) : relative
        };
    }

    /**
     * Shows the diff for the selected entry in the diff panel.
     *
     * @param entry the selected diff entry
     */
    private void showInlineDiff(DiffEntry entry) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                if (recentlyApplied.contains(entryKey(entry))) {
                    Boolean toTarget = applyDirections.get(entryKey(entry));
                    String dns = toTarget == null ? "vault"
                            : toTarget ? getTargetDns() : getSourceDns();
                    diffPanel.setRequest(new com.intellij.diff.requests.MessageDiffRequest(
                            entry.name(), "Changes successfully applied to " + dns));
                    return;
                }

                File srcFile = entry.sourceFile();
                File tgtFile = entry.targetFile();

                boolean isJsonAvailable = entry.comparisonType() == ComparisonType.MDL &&
                        ((srcFile != null && new File(srcFile.getParentFile(), srcFile.getName() + ".json").exists()) ||
                         (tgtFile != null && new File(tgtFile.getParentFile(), tgtFile.getName() + ".json").exists()));

                viewModeCombo.setEnabled(isJsonAvailable);
                if (!isJsonAvailable) {
                    isUpdatingViewMode = true;
                    viewModeCombo.setSelectedItem("Raw MDL View");
                    isUpdatingViewMode = false;
                }

                String src = "";
                String tgt = "";

                if (isJsonAvailable && "Semantic JSON View".equals(viewModeCombo.getSelectedItem())) {
                    if (srcFile != null) {
                        File jsonFile = new File(srcFile.getParentFile(), srcFile.getName() + ".json");
                        if (jsonFile.exists()) {
                            src = FileUtils.readFileToString(jsonFile, "UTF-8");
                            try {
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                Object json = mapper.readValue(src, Object.class);
                                src = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                            } catch (Exception e) {}
                        } else {
                            src = FileUtils.readFileToString(srcFile, "UTF-8");
                        }
                    }
                    if (tgtFile != null) {
                        File jsonFile = new File(tgtFile.getParentFile(), tgtFile.getName() + ".json");
                        if (jsonFile.exists()) {
                            tgt = FileUtils.readFileToString(jsonFile, "UTF-8");
                            try {
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                Object json = mapper.readValue(tgt, Object.class);
                                tgt = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                            } catch (Exception e) {}
                        } else {
                            tgt = FileUtils.readFileToString(tgtFile, "UTF-8");
                        }
                    }
                } else {
                    src = srcFile != null ? FileUtils.readFileToString(srcFile, "UTF-8") : "";
                    tgt = tgtFile != null ? FileUtils.readFileToString(tgtFile, "UTF-8") : "";
                }

                DocumentContent srcContent;
                DocumentContent tgtContent;

                if (isJsonAvailable && "Semantic JSON View".equals(viewModeCombo.getSelectedItem())) {
                    srcContent = DiffContentFactory.getInstance().create(src);
                    tgtContent = DiffContentFactory.getInstance().create(tgt);
                } else {
                    com.intellij.openapi.vfs.VirtualFile srcVf = srcFile != null ? com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(srcFile) : null;
                    com.intellij.openapi.vfs.VirtualFile tgtVf = tgtFile != null ? com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tgtFile) : null;
                    
                    if (srcVf != null) com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider.allowWriting(com.intellij.util.containers.ContainerUtil.createMaybeSingletonList(srcVf));
                    if (tgtVf != null) com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider.allowWriting(com.intellij.util.containers.ContainerUtil.createMaybeSingletonList(tgtVf));

                    srcContent = srcVf != null ? (DocumentContent) DiffContentFactory.getInstance().createDocument(toolboxProject.getProject(), srcVf) : DiffContentFactory.getInstance().create(src);
                    tgtContent = tgtVf != null ? (DocumentContent) DiffContentFactory.getInstance().createDocument(toolboxProject.getProject(), tgtVf) : DiffContentFactory.getInstance().create(tgt);
                }

                diffPanel.setRequest(new SimpleDiffRequest(
                        entry.name(), srcContent, tgtContent,
                        getSourceDns(), getTargetDns()));
            } catch (Exception ex) {
                logger.error("Failed to show diff for {}", entry.name(), ex);
            }
        });
    }

    /**
     * Returns a stable string key for a diff entry, used to track apply/revert state.
     *
     * @param entry the diff entry
     * @return a composite key string
     */
    private static String entryKey(DiffEntry entry) {
        return entry.comparisonType() + "|" + entry.componentType() + "|" + entry.name();
    }

    /**
     * Checks whether there are any applied changes with revertable backups.
     *
     * @return true if there are pending backups
     */
    public boolean hasAppliedChanges() {
        return !backups.isEmpty();
    }

    /**
     * Resets apply state: clears applied markers, deletes backups, disables apply mode.
     */
    private void resetApplyState() {
        deleteAllBackups();
        backups.clear();
        recentlyApplied.clear();
        applyDirections.clear();
        applyChangesEnabled = false;
        applyInProgress = false;
        selectedEntry = null;
        if (enableApplyBtn != null) enableApplyBtn.setSelected(false);
        updateActionBar();
    }

    /**
     * Disables apply changes mode without resetting applied history.
     */
    private void disableApplyChanges() {
        applyChangesEnabled = false;
        updateActionBar();
    }

    /**
     * Handles the "Enable Apply Changes" toggle. Shows a warning dialog and enables apply mode if confirmed.
     */
    private void enableApplyChanges() {
        if (sourceClient == null || targetClient == null) {
            Messages.showErrorDialog(toolboxProject.getProject(),
                    "Both vaults must be authenticated before enabling apply changes.",
                    "Apply Changes");
            enableApplyBtn.setSelected(false);
            return;
        }

        if (checkProductionVault(sourceClient, sourceClientDns) || checkProductionVault(targetClient, targetClientDns)) {
            Message message = toolboxProject.newMessage();
            message.append("Apply changes cannot be enabled on a production vault.");
            message.showError();
            enableApplyBtn.setSelected(false);
            return;
        }

        int result = Messages.showOkCancelDialog(
                toolboxProject.getProject(),
                "<html><b>Warning: Apply Changes is Irreversible</b><br><br>" +
                "Enabling this feature allows you to deploy configurations directly between " +
                "Vault environments. These changes will immediately affect the target Vault.<br><br>" +
                "Do you want to enable apply changes?</html>",
                "Enable Apply Changes",
                "Enable",
                "Cancel",
                Messages.getWarningIcon());

        if (result != Messages.OK) {
            enableApplyBtn.setSelected(false);
            return;
        }

        applyChangesEnabled = true;
        updateActionBar();
        setStatus("Apply changes enabled. Select a file and choose a direction to apply.");
    }

    /**
     * Swaps the source and target vault configurations.
     */
    private void swapVaults() {
        VaultClient tmpClient = sourceClient;
        String tmpDns = sourceClientDns;
        String tmpDnsText = sourceDnsLabel.getText();
        String tmpBtnText = sourceSignInBtn.getText();
        Color tmpDnsColor = sourceDnsLabel.getForeground();
        String tmpAuthText = sourceAuthStatus.getText();
        Color tmpAuthColor = sourceAuthStatus.getForeground();

        sourceClient = targetClient;
        sourceClientDns = targetClientDns;
        sourceDnsLabel.setText(targetDnsLabel.getText());
        sourceDnsLabel.setForeground(targetDnsLabel.getForeground());
        sourceSignInBtn.setText(targetSignInBtn.getText());
        sourceAuthStatus.setText(targetAuthStatus.getText());
        sourceAuthStatus.setForeground(targetAuthStatus.getForeground());

        targetClient = tmpClient;
        targetClientDns = tmpDns;
        targetDnsLabel.setText(tmpDnsText);
        targetDnsLabel.setForeground(tmpDnsColor);
        targetSignInBtn.setText(tmpBtnText);
        targetAuthStatus.setText(tmpAuthText);
        targetAuthStatus.setForeground(tmpAuthColor);

        resetApplyState();
        updateCompareButton();
    }

    /**
     * Checks whether a vault client is connected to a production environment.
     * First applies a quick DNS exemption for known non-production domains (vaultdev.com, vaultpvm.com),
     * then calls the Vault API to retrieve the domain type.
     *
     * @param client the vault client to check
     * @param dns    the vault DNS (used for the quick DNS exemption check)
     * @return true if the vault is a production environment
     */
    private static boolean checkProductionVault(VaultClient client, String dns) {
        if (client == null) return false;
        if (dns != null) {
            String lower = dns.toLowerCase();
            if (lower.contains("vaultdev.com") || lower.contains("vaultpvm.com")) return false;
        }
        try {
            DomainResponse response = client.newRequest(DomainRequest.class).retrieveDomainInformation();
            if (response != null && !response.isFailure() && response.getDomain() != null) {
                return "PRODUCTION".equalsIgnoreCase(response.getDomain().getDomainType());
            }
        } catch (Exception e) {
            logger.warn("Could not determine production vault status for {}: {}", dns, e.getMessage());
        }
        return false;
    }

    /**
     * Shows/hides the apply strip and refreshes button states and the applied banner.
     */
    private void updateActionBar() {
        if (applyActionStrip == null) return;
        applyActionStrip.setVisible(applyChangesEnabled);

        if (!applyChangesEnabled || applyInProgress) {
            setApplyButtonsEnabled(false, false, false);
            updateAppliedBanner();
            return;
        }

        if (selectedEntry == null) {
            setApplyButtonsEnabled(false, false, false);
            applyToSourceBtn.setToolTipText("Select a file to apply");
            applyToTargetBtn.setToolTipText("Select a file to apply");
            revertBtn.setToolTipText("Select a file to revert");
            updateAppliedBanner();
            return;
        }

        String key = entryKey(selectedEntry);
        boolean isApplied = recentlyApplied.contains(key);
        boolean hasBackup = backups.containsKey(key);
        boolean identical = STATUS_IDENTICAL.equals(selectedEntry.status());
        boolean canApplyToSource = !identical && selectedEntry.targetFile() != null && !isApplied;
        boolean canApplyToTarget = !identical && selectedEntry.sourceFile() != null && !isApplied;

        setApplyButtonsEnabled(canApplyToSource, isApplied, canApplyToTarget);

        String fullSource = getSourceDns();
        String fullTarget = getTargetDns();
        applyToSourceBtn.setToolTipText(canApplyToSource ? "Deploy target content to " + fullSource
                : isApplied ? "Already applied" : "Not applicable for this entry");
        applyToTargetBtn.setToolTipText(canApplyToTarget ? "Deploy source content to " + fullTarget
                : isApplied ? "Already applied" : "Not applicable for this entry");
        revertBtn.setToolTipText(isApplied
                ? (hasBackup ? "Re-deploy original content to undo the applied change"
                             : "Re-deploy original content (no prior backup)")
                : "No applied change to revert");

        updateAppliedBanner();
    }

    /**
     * Sets the enabled state of the apply buttons.
     *
     * @param source true to enable apply to source button
     * @param revert true to enable revert button
     * @param target true to enable apply to target button
     */
    private void setApplyButtonsEnabled(boolean source, boolean revert, boolean target) {
        if (applyToSourceBtn != null) applyToSourceBtn.setEnabled(source);
        if (revertBtn        != null) revertBtn.setEnabled(revert);
        if (applyToTargetBtn != null) applyToTargetBtn.setEnabled(target);
    }

    /**
     * Updates the applied banner above the diff viewer based on the current selection.
     */
    private void updateAppliedBanner() {
        if (appliedBanner == null) return;
        if (selectedEntry != null && recentlyApplied.contains(entryKey(selectedEntry))) {
            Boolean toTarget = applyDirections.get(entryKey(selectedEntry));
            String dns = toTarget == null ? "vault"
                    : toTarget ? getTargetDns() : getSourceDns();
            appliedBannerLabel.setIcon(AllIcons.Actions.Checked);
            appliedBannerLabel.setText(" Applied to " + dns);
            appliedBanner.setVisible(true);
        } else {
            appliedBanner.setVisible(false);
        }
    }

    /**
     * Applies the selected diff entry's changes to the destination vault.
     *
     * @param entry    the diff entry to apply
     * @param toTarget true to apply source → target; false to apply target → source
     */
    private void applyChange(DiffEntry entry, boolean toTarget) {
        File fileToApply = toTarget ? entry.sourceFile() : entry.targetFile();
        if (fileToApply == null || !fileToApply.exists()) {
            Messages.showErrorDialog(toolboxProject.getProject(),
                    "The file to apply could not be found.", "Apply Error");
            return;
        }

        VaultClient destinationClient = toTarget ? targetClient : sourceClient;
        String destinationDns = toTarget ? getTargetDns() : getSourceDns();


        String key = entryKey(entry);
        File originalDestFile = toTarget ? entry.targetFile() : entry.sourceFile();
        if (originalDestFile != null && originalDestFile.exists() && !backups.containsKey(key)) {
            File backup = new File(originalDestFile.getAbsolutePath() + ".bak");
            try {
                FileUtils.copyFile(originalDestFile, backup);
                backups.put(key, backup);
            } catch (IOException ex) {
                logger.warn("Could not create backup for {}: {}", key, ex.getMessage());
            }
        }

        applyInProgress = true;
        setApplyButtonsEnabled(false, false, false);
        setStatus("Checking vault type and applying changes to " + destinationDns + "…");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (checkProductionVault(destinationClient, destinationDns)) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        File bk = backups.remove(key);
                        if (bk != null) bk.delete();
                        Message message = toolboxProject.newMessage();
                        message.append("This action cannot be performed on this vault.");
                        message.showError();
                        applyInProgress = false;
                        setStatus("Apply changes could not be completed for " + destinationDns + ".");
                        updateActionBar();
                    }, ModalityState.any());
                    return;
                }

                VaultResponse response = deployToVault(destinationClient, entry, fileToApply);
                boolean success = response != null && !response.isFailure();

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (success) {
                        updateLocalDestFile(entry, toTarget, fileToApply);
                        recentlyApplied.add(key);
                        applyDirections.put(key, toTarget);
                        resultsTree.repaint();
                        showInlineDiff(entry);
                        setStatus("Changes applied to " + destinationDns + ".");
                    } else {
                        File bk = backups.remove(key);
                        if (bk != null) bk.delete();
                        setStatus("Failed to apply changes to " + destinationDns + ".");
                    }
                    applyInProgress = false;
                    showResultDialog(response, "Apply: " + entry.name() + " → " + destinationDns);
                    selectedEntry = entry;
                    updateActionBar();
                }, ModalityState.any());
            } catch (Exception ex) {
                logger.error("Error applying changes for {}", key, ex);
                ApplicationManager.getApplication().invokeLater(() -> {
                    applyInProgress = false;
                    File bk = backups.remove(key);
                    if (bk != null) bk.delete();
                    setStatus("Error applying changes.");
                    Messages.showErrorDialog(toolboxProject.getProject(),
                            "Exception: " + ex.getMessage(),
                            "Apply: " + entry.name() + " → " + destinationDns);
                    updateActionBar();
                }, ModalityState.any());
            }
        });
    }

    /**
     * Deploys the given file's content to the destination vault.
     *
     * @param client      the vault client to deploy to
     * @param entry       the diff entry (used to determine type)
     * @param fileToApply the file whose content should be deployed
     * @return the vault response
     */
    private VaultResponse deployToVault(VaultClient client, DiffEntry entry, File fileToApply) throws Exception {
        if (entry.comparisonType() == ComparisonType.MDL) {
            String mdlContent = FileUtils.readFileToString(fileToApply, "UTF-8");
            return client.newRequest(MetaDataRequest.class)
                    .setRequestString(mdlContent)
                    .executeMDLScript();
        } else {
            byte[] bytes = FileUtils.readFileToByteArray(fileToApply);
            return client.newRequest(SDKRequest.class)
                    .setBinaryFile(fileToApply.getName(), bytes)
                    .addOrReplaceSingleSourceCodeFile();
        }
    }

    /**
     * Updates the local temp file in the destination directory to reflect the applied content.
     * For "Source Only" entries applying to target, this creates the target temp file.
     *
     * @param entry       the diff entry
     * @param toTarget    true if applying source → target
     * @param fileToApply the source file being applied
     */
    private void updateLocalDestFile(DiffEntry entry, boolean toTarget, File fileToApply) {
        File destFile = toTarget ? entry.targetFile() : entry.sourceFile();

        if (destFile == null) {
            File[] dirs = comparisonDirs.get(entry.comparisonType());
            if (dirs == null) return;
            File fromDir = toTarget ? dirs[0] : dirs[1];
            File toDir   = toTarget ? dirs[1] : dirs[0];
            String rel = fromDir.toURI().relativize(fileToApply.toURI()).getPath();
            destFile = new File(toDir, rel);
        }

        try {
            destFile.getParentFile().mkdirs();
            FileUtils.copyFile(fileToApply, destFile);
        } catch (IOException ex) {
            logger.warn("Could not update local dest file: {}", ex.getMessage());
        }
    }

    /**
     * Reverts a previously applied change.
     * <ul>
     *   <li>If a backup exists ("Modified" entries): re-deploys the backup content.</li>
     *   <li>If no backup ("Source Only" / "Target Only" entries): deletes the newly
     *       created component from the destination vault.</li>
     * </ul>
     *
     * @param entry the diff entry to revert
     */
    private void revertChange(DiffEntry entry) {
        String key = entryKey(entry);
        Boolean wasAppliedToTarget = applyDirections.get(key);
        if (wasAppliedToTarget == null) {
            Messages.showErrorDialog(toolboxProject.getProject(),
                    "Cannot determine apply direction. This change cannot be automatically reverted.",
                    "Revert Error");
            return;
        }

        VaultClient clientToRevert = wasAppliedToTarget ? targetClient : sourceClient;
        String dns = wasAppliedToTarget ? getTargetDns() : getSourceDns();
        File backup = backups.get(key);

        if (backup == null || !backup.exists()) {

            File appliedFile = wasAppliedToTarget ? entry.sourceFile() : entry.targetFile();
            if (appliedFile == null || !appliedFile.exists()) {
                Messages.showErrorDialog(toolboxProject.getProject(),
                        "Cannot revert: the applied file is no longer accessible.", "Revert Error");
                return;
            }

            int result = Messages.showOkCancelDialog(
                    toolboxProject.getProject(),
                    "This will delete the component from " + dns + ".\n\n" +
                    "Are you sure you want to revert?",
                    "Revert Applied Change",
                    "Revert",
                    "Cancel",
                    Messages.getWarningIcon());
            if (result != Messages.OK) return;

            applyInProgress = true;
            setApplyButtonsEnabled(false, false, false);
            setStatus("Reverting (deleting from " + dns + ")…");

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    VaultResponse response = deleteFromVault(clientToRevert, entry, appliedFile);
                    boolean success = response != null && !response.isFailure();

                    ApplicationManager.getApplication().invokeLater(() -> {
                        applyInProgress = false;
                        if (success) {
                            removeLocalDestFile(entry, wasAppliedToTarget);
                            recentlyApplied.remove(key);
                            applyDirections.remove(key);
                            resultsTree.repaint();
                            showInlineDiff(entry);
                            setStatus("Changes reverted in " + dns + ".");
                        } else {
                            setStatus("Failed to revert in " + dns + ".");
                        }
                        showResultDialog(response, "Revert: " + entry.name() + " in " + dns);
                        selectedEntry = entry;
                        updateActionBar();
                    }, ModalityState.any());
                } catch (Exception ex) {
                    logger.error("Error reverting changes for {}", key, ex);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        applyInProgress = false;
                        setStatus("Error reverting changes.");
                        Messages.showErrorDialog(toolboxProject.getProject(),
                                "Exception: " + ex.getMessage(),
                                "Revert: " + entry.name() + " in " + dns);
                        updateActionBar();
                    }, ModalityState.any());
                }
            });
            return;
        }

        int result = Messages.showOkCancelDialog(
                toolboxProject.getProject(),
                "This will re-deploy the original content to " + dns + ".\n\n" +
                "Are you sure you want to revert?",
                "Revert Applied Change",
                "Revert",
                "Cancel",
                Messages.getWarningIcon());
        if (result != Messages.OK) return;

        applyInProgress = true;
        setApplyButtonsEnabled(false, false, false);
        setStatus("Reverting changes in " + dns + "…");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                VaultResponse response = deployToVault(clientToRevert, entry, backup);
                boolean success = response != null && !response.isFailure();

                ApplicationManager.getApplication().invokeLater(() -> {
                    applyInProgress = false;
                    if (success) {
                        File destFile = wasAppliedToTarget ? entry.targetFile() : entry.sourceFile();
                        if (destFile != null) {
                            try {
                                FileUtils.copyFile(backup, destFile);
                            } catch (IOException ex) {
                                logger.warn("Could not restore local file from backup: {}", ex.getMessage());
                            }
                        }
                        backup.delete();
                        backups.remove(key);
                        recentlyApplied.remove(key);
                        applyDirections.remove(key);
                        resultsTree.repaint();
                        showInlineDiff(entry);
                        setStatus("Changes reverted in " + dns + ".");
                    } else {
                        setStatus("Failed to revert changes in " + dns + ".");
                    }
                    showResultDialog(response, "Revert: " + entry.name() + " in " + dns);
                    selectedEntry = entry;
                    updateActionBar();
                }, ModalityState.any());
            } catch (Exception ex) {
                logger.error("Error reverting changes for {}", key, ex);
                ApplicationManager.getApplication().invokeLater(() -> {
                    applyInProgress = false;
                    setStatus("Error reverting changes.");
                    Messages.showErrorDialog(toolboxProject.getProject(),
                            "Exception: " + ex.getMessage(),
                            "Revert: " + entry.name() + " in " + dns);
                    updateActionBar();
                }, ModalityState.any());
            }
        });
    }

    /**
     * Deletes a component from the destination vault. Used to revert a "Source Only" or
     * "Target Only" apply where no backup exists to re-deploy.
     *
     * @param client      the vault client to issue the delete against
     * @param entry       the diff entry (used to derive component type and name)
     * @param appliedFile the local file that was applied (used for SDK request body)
     * @return the vault response
     */
    private VaultResponse deleteFromVault(VaultClient client, DiffEntry entry, File appliedFile) throws Exception {
        if (entry.comparisonType() == ComparisonType.MDL) {
            String compType = entry.componentType();
            String prefixDot = compType + ".";
            String compName = entry.name().startsWith(prefixDot)
                    ? entry.name().substring(prefixDot.length()) : entry.name();
            String dropStatement = "DROP " + compType + " " + compName + ";";
            return client.newRequest(MetaDataRequest.class)
                    .setRequestString(dropStatement)
                    .executeMDLScript();
        } else {
            String fullPath = entry.componentType() + "/" + entry.name();
            String componentName = fullPath.replace('/', '.').replace(".java", "");
            byte[] bytes = FileUtils.readFileToByteArray(appliedFile);
            return client.newRequest(SDKRequest.class)
                    .setBinaryFile(appliedFile.getName(), bytes)
                    .deleteSingleSourceCodeFile(componentName);
        }
    }

    /**
     * Removes the local temp file that was created in the destination directory by
     * {@link #updateLocalDestFile} when a "Source Only" or "Target Only" entry was applied.
     *
     * @param entry           the diff entry
     * @param wasAppliedToTarget true if the apply went source → target
     */
    private void removeLocalDestFile(DiffEntry entry, boolean wasAppliedToTarget) {
        File[] dirs = comparisonDirs.get(entry.comparisonType());
        if (dirs == null) return;
        File fromDir = wasAppliedToTarget ? dirs[0] : dirs[1];
        File toDir   = wasAppliedToTarget ? dirs[1] : dirs[0];
        File srcFile = wasAppliedToTarget ? entry.sourceFile() : entry.targetFile();
        if (srcFile == null) return;
        String rel = fromDir.toURI().relativize(srcFile.toURI()).getPath();
        File destFile = new File(toDir, rel);
        if (destFile.exists()) destFile.delete();
    }

    /**
     * Shows a synchronous result dialog for an apply or revert operation.
     * Mirrors the logic of {@code Deploy.showResults} but calls {@code Messages.*} directly
     * so the dialog appears immediately inside the modal Compare Environments dialog.
     *
     * @param response the vault response; may be {@code null}
     * @param title    the dialog title
     */
    private void showResultDialog(VaultResponse response, String title) {
        int numErrors = 0;
        int numWarnings = 0;
        StringBuilder sb = new StringBuilder();
        try {
            if (response == null) {
                sb.append("No response received from vault.");
                numErrors = 1;
            } else {
                sb.append(response.getResponseStatus());
                if (response.getResponseMessage() != null) {
                    sb.append("\n\n").append(response.getResponseMessage());
                }
                if (response.hasErrors()) {
                    for (VaultResponse.APIResponseError error : response.getErrors()) {
                        sb.append("\n").append(error.getMessage());
                        numErrors++;
                    }
                }
                if (response instanceof MdlExecuteResponse mdlResponse) {
                    MdlExecuteResponse.ScriptExecution exec = mdlResponse.getScriptExecution();
                    if (exec != null) {
                        numWarnings = exec.getWarnings() != null ? exec.getWarnings() : 0;
                        numErrors += (exec.getFailures() != null ? exec.getFailures() : 0)
                                + (exec.getExceptions() != null ? exec.getExceptions() : 0);
                        sb.append("\n\nmessage = ").append(exec.getMessage());
                        sb.append("\nwarnings = ").append(exec.getWarnings());
                        sb.append("\nfailures = ").append(exec.getFailures());
                        sb.append("\nexceptions = ").append(exec.getExceptions());
                        sb.append("\ncomponents_affected = ").append(exec.getComponentsAffected());
                    }
                    List<MdlExecuteResponse.StatementExecution> statements = mdlResponse.getStatementExecution();
                    if (statements != null) {
                        for (MdlExecuteResponse.StatementExecution stmt : statements) {
                            sb.append("\n\ncommand = ").append(stmt.getCommand());
                            sb.append("\ncomponent = ").append(stmt.getComponent());
                            sb.append("\nmessage = ").append(stmt.getMessage());
                            sb.append("\nresponse = ").append(stmt.getResponse());
                            Object rawErrors = stmt.get("errors");
                            if (rawErrors instanceof List<?> errors) {
                                for (Object errObj : errors) {
                                    if (errObj instanceof Map<?, ?> errMap) {
                                        sb.append("\n").append(errMap.get("message"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            numErrors = 1;
            sb.append("\nexception = ").append(ex.getMessage());
        }

        String text = sb.toString();
        if (numErrors > 0) {
            Messages.showErrorDialog(toolboxProject.getProject(), text, title);
        } else if (numWarnings > 0) {
            Messages.showWarningDialog(toolboxProject.getProject(), text, title);
        } else {
            Messages.showInfoMessage(toolboxProject.getProject(), text, title);
        }
    }

    /**
     * Deletes all backup files currently tracked.
     */
    private void deleteAllBackups() {
        for (File backup : backups.values()) {
            if (backup != null && backup.exists()) {
                backup.delete();
            }
        }
    }



    /**
     * Gets the source Vault DNS.
     *
     * @return the source DNS
     */
    private String getSourceDns() { return sourceClientDns; }

    /**
     * Gets the target Vault DNS.
     *
     * @return the target DNS
     */
    private String getTargetDns() { return targetClientDns; }

    /**
     * Updates the status bar text.
     *
     * @param text the text to set
     */
    private void setStatus(String text) { statusLabel.setText(text); }

    /**
     * Disposes panel resources. Cancels any in-flight comparison task (which triggers
     * cleanup of its own temp dir via {@code onCancel}), then deletes the last completed
     * run's temp dir and any backup files.
     */
    @Override
    public void dispose() {
        if (runningIndicator != null) {
            runningIndicator.cancel();
            runningIndicator = null;
        }
        deleteAllBackups();
        deleteTempDir(lastComparisonBaseDir);
    }

    /**
     * Deletes temporary directories used for comparison.
     *
     * @param dir the base directory
     */
    private void deleteTempDir(File dir) {
        if (dir == null) return;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException ex) {
                logger.warn("Could not delete temp comparison dir: {}", dir, ex);
            }
        });
    }

    /**
     * Custom tree cell renderer for the diff result tree.
     * Non-static so it can access the panel's {@code recentlyApplied} set.
     */
    private class DiffTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final Color SOURCE_ONLY = new JBColor(new Color(0, 128, 64),   new Color(80, 200, 120));
        private static final Color TARGET_ONLY = new JBColor(new Color(180, 40, 40),  new Color(220, 100, 100));
        private static final Color MODIFIED    = new JBColor(new Color(160, 90, 0),   new Color(210, 150, 50));
        private static final Color IDENTICAL   = JBColor.namedColor("Label.disabledForeground", JBColor.GRAY);
        private static final Color APPLIED     = new JBColor(new Color(0, 100, 200),  new Color(90, 155, 230));

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (!(value instanceof DefaultMutableTreeNode node)) return this;

            Font plain = tree.getFont().deriveFont(Font.PLAIN);
            Font bold  = tree.getFont().deriveFont(Font.BOLD);

            if (node.getUserObject() instanceof SectionNode section) {
                Icon icon = section.type() == ComparisonType.MDL
                        ? AllIcons.Nodes.Module
                        : AllIcons.Nodes.Package;
                setIcon(icon);
                setText(section.type().name() + "  (" + section.count() + ")");
                setFont(bold);
                if (!sel) setForeground(tree.getForeground());

            } else if (node.getUserObject() instanceof GroupNode group) {
                setIcon(AllIcons.Nodes.Folder);
                setText(group.name() + "  (" + group.count() + ")");
                setFont(bold);
                if (!sel) setForeground(tree.getForeground());

            } else if (node.getUserObject() instanceof DiffEntry entry) {
                setFont(plain);
                boolean applied = recentlyApplied.contains(entryKey(entry));

                if (applied) {
                    setIcon(AllIcons.Actions.Checked);
                    setText(entry.name() + "  (applied)");
                    setToolTipText("Changes applied");
                    if (!sel) setForeground(APPLIED);
                } else if (STATUS_IDENTICAL.equals(entry.status())) {
                    setIcon(AllIcons.FileTypes.Text);
                    setText(entry.name());
                    setToolTipText("Identical");
                    if (!sel) setForeground(IDENTICAL);
                } else {
                    Color statusColor = switch (entry.status()) {
                        case "Source Only" -> SOURCE_ONLY;
                        case "Target Only" -> TARGET_ONLY;
                        default            -> MODIFIED;
                    };
                    Icon statusIcon = switch (entry.status()) {
                        case "Source Only" -> AllIcons.General.Add;
                        case "Target Only" -> AllIcons.General.Remove;
                        default            -> AllIcons.Actions.Edit;
                    };
                    setIcon(statusIcon);
                    setText(entry.name());
                    setToolTipText(entry.status());
                    if (!sel) setForeground(statusColor);
                }
            }
            return this;
        }
    }
}
