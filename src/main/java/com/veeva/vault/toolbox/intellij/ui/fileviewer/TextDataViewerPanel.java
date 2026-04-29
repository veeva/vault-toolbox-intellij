package com.veeva.vault.toolbox.intellij.ui.fileviewer;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class TextDataViewerPanel extends JBPanel<TextDataViewerPanel> {
    private Editor editor;

    public TextDataViewerPanel(Project project, File textFile, Disposable parentDisposable) {
        setLayout(new BorderLayout());
        
        try {
            String content = new String(Files.readAllBytes(textFile.toPath()), StandardCharsets.UTF_8);
            
            VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(textFile);
            
            Document document = EditorFactory.getInstance().createDocument(content);
            document.setReadOnly(true);
            
            if (vFile != null) {
                editor = EditorFactory.getInstance().createEditor(document, project, vFile, true);
            } else {
                editor = EditorFactory.getInstance().createViewer(document, project);
            }
            
            Disposer.register(parentDisposable, () -> {
                if (editor != null && !editor.isDisposed()) {
                    EditorFactory.getInstance().releaseEditor(editor);
                }
            });
            
            add(editor.getComponent(), BorderLayout.CENTER);
        } catch (Exception e) {
            JBTextArea textArea = new JBTextArea();
            textArea.setEditable(false);
            textArea.setText("Error loading file: " + e.getMessage());
            add(new JBScrollPane(textArea), BorderLayout.CENTER);
        }
    }
}