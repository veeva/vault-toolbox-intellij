package com.veeva.vault.toolbox.intellij.ui.fileviewer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.Disposable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileViewerDialog extends DialogWrapper {
    private final File target;
    private final Project project;

    public FileViewerDialog(@Nullable Project project, File target) {
        super(project, true);
        this.project = project;
        this.target = target;
        init();
        setTitle("File Viewer: " + target.getName());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(900, 600));

        if (target.isDirectory()) {
            File[] files = target.listFiles();
            if (files != null) {
                Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

                List<String> fileNames = new ArrayList<>();
                for (File f : files) {
                    if (f.isFile()) {
                        fileNames.add(f.getName());
                    }
                }

                JBList<String> fileList = new JBList<>(fileNames);
                fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                fileList.setCellRenderer(new DefaultListCellRenderer() {
                    @Override
                    public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        javax.swing.JLabel label = (javax.swing.JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        label.setBorder(com.intellij.util.ui.JBUI.Borders.empty(4, 8));
                        return label;
                    }
                });

                JBScrollPane listScrollPane = new JBScrollPane(fileList);
                listScrollPane.setMinimumSize(new Dimension(150, 0));

                JBPanel<?> contentPanel = new JBPanel<>(new BorderLayout());
                contentPanel.add(new JBLabel("Select a file to view", SwingConstants.CENTER), BorderLayout.CENTER);

                JBSplitter splitter = new JBSplitter(false, 0.3f);

                DefaultActionGroup actionGroup = new DefaultActionGroup();
                actionGroup.add(new AnAction("Collapse File List", "Show/Hide the file list", AllIcons.General.ArrowLeft) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        if (splitter.getFirstComponent() != null) {
                            splitter.setFirstComponent(null);
                        } else {
                            splitter.setFirstComponent(listScrollPane);
                        }
                    }

                    @Override
                    public void update(@NotNull AnActionEvent e) {
                        boolean isVisible = splitter.getFirstComponent() != null;
                        e.getPresentation().setIcon(isVisible ? AllIcons.General.ArrowLeft : AllIcons.General.ArrowRight);
                        e.getPresentation().setText(isVisible ? "Collapse File List" : "Expand File List");
                    }
                });

                ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("FileViewerToolbar", actionGroup, true);

                JBPanel<?> rightHeader = new JBPanel<>(new BorderLayout());
                rightHeader.add(toolbar.getComponent(), BorderLayout.WEST);
                rightHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, com.intellij.ui.JBColor.border()));

                JBPanel<?> rightPanel = new JBPanel<>(new BorderLayout());
                rightPanel.add(rightHeader, BorderLayout.NORTH);
                rightPanel.add(contentPanel, BorderLayout.CENTER);
                toolbar.setTargetComponent(rightPanel);

                splitter.setFirstComponent(listScrollPane);
                splitter.setSecondComponent(rightPanel);

                Map<String, JComponent> viewerCache = new HashMap<>();

                fileList.addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {
                        String selectedName = fileList.getSelectedValue();
                        if (selectedName != null) {
                            JComponent viewer = viewerCache.computeIfAbsent(selectedName, k -> {
                                File f = new File(target, selectedName);
                                // --- FIX: Pass 'true' because this file was clicked inside a list! ---
                                return createViewerComponent(project, f, getDisposable(), true);
                            });

                            contentPanel.removeAll();
                            contentPanel.add(viewer, BorderLayout.CENTER);
                            contentPanel.revalidate();
                            contentPanel.repaint();
                        }
                    }
                });

                if (!fileNames.isEmpty()) {
                    fileList.setSelectedIndex(0);
                }

                mainPanel.add(splitter, BorderLayout.CENTER);
            } else {
                mainPanel.add(new JBLabel("Directory is empty or not readable", SwingConstants.CENTER), BorderLayout.CENTER);
            }
        } else if (target.isFile()) {
            // --- FIX: Pass 'false' because this is the root window target ---
            mainPanel.add(createViewerComponent(project, target, getDisposable(), false), BorderLayout.CENTER);
        } else {
            mainPanel.add(new JBLabel("Target does not exist or is not readable: " + target.getAbsolutePath()), BorderLayout.NORTH);
        }

        return mainPanel;
    }

    // --- FIX: Add 'isNested' flag to prevent recursive panel nesting ---
    public static JComponent createViewerComponent(Project project, File file, Disposable parentDisposable, boolean isNested) {
        String name = file.getName().toLowerCase();

        if (name.endsWith(".zip") || name.endsWith(".vpk")) {
            if (isNested) {
                // Return a simple prompt to open a NEW window instead of nesting!
                JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
                JBPanel<?> inner = new JBPanel<>(new BorderLayout(0, 10));

                inner.add(new JBLabel("Archive File: " + file.getName(), AllIcons.FileTypes.Archive, SwingConstants.CENTER), BorderLayout.NORTH);

                JButton openBtn = new JButton("Open Archive in New Window");
                openBtn.addActionListener(e -> {
                    new FileViewerDialog(project, file).show();
                });

                inner.add(openBtn, BorderLayout.CENTER);
                panel.add(inner);
                return panel;
            } else {
                return new ArchiveViewerPanel(project, file, parentDisposable);
            }
        } else if (name.endsWith(".csv")) {
            return new CsvDataViewerPanel(file);
        } else {
            return new TextDataViewerPanel(project, file, parentDisposable);
        }
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }
}