package com.veeva.vault.toolbox.intellij.ui.fileviewer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.Disposable;

public class ArchiveViewerPanel extends JBPanel<ArchiveViewerPanel> {

    private final Map<String, File> extractedFilesCache = new HashMap<>();
    private final Map<String, JComponent> viewerCache = new HashMap<>();

    public ArchiveViewerPanel(Project project, File archiveFile, Disposable parentDisposable) {
        setLayout(new BorderLayout());

        try (ZipFile zipFile = new ZipFile(archiveFile)) {
            List<String> fileNames = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String entryName = entry.getName();
                    fileNames.add(entryName);

                    File tempFile = File.createTempFile("vt_archive_", "_" + new File(entryName).getName());
                    tempFile.deleteOnExit();
                    try (InputStream in = zipFile.getInputStream(entry);
                         FileOutputStream out = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }

                    extractedFilesCache.put(entryName, tempFile);
                }
            }

            fileNames.sort(String::compareToIgnoreCase);

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

            ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("ArchiveViewerToolbar", actionGroup, true);

            JBPanel<?> rightHeader = new JBPanel<>(new BorderLayout());
            rightHeader.add(toolbar.getComponent(), BorderLayout.WEST);
            rightHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, com.intellij.ui.JBColor.border()));

            JBPanel<?> rightPanel = new JBPanel<>(new BorderLayout());
            rightPanel.add(rightHeader, BorderLayout.NORTH);
            rightPanel.add(contentPanel, BorderLayout.CENTER);
            toolbar.setTargetComponent(rightPanel);

            splitter.setFirstComponent(listScrollPane);
            splitter.setSecondComponent(rightPanel);
            add(splitter, BorderLayout.CENTER);

            fileList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    String selectedFile = fileList.getSelectedValue();
                    if (selectedFile != null && extractedFilesCache.containsKey(selectedFile)) {

                        JComponent viewer = viewerCache.computeIfAbsent(selectedFile, k -> {
                            return FileViewerDialog.createViewerComponent(project, extractedFilesCache.get(selectedFile), parentDisposable, false);
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

        } catch (Exception e) {
            add(new JBLabel("Error reading archive: " + e.getMessage()), BorderLayout.NORTH);
        }
    }
}