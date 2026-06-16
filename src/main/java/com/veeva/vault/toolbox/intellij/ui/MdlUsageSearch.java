package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds where a Vault API name is referenced across the extracted {@code .mdl} files (the reverse
 * of go-to-definition). Scans on a background thread, then shows the matches in a navigable popup;
 * choosing one opens that file at the reference. Self-contained so it depends only on stable
 * platform API.
 */
public final class MdlUsageSearch {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private MdlUsageSearch() {
    }

    /**
     * Finds references to a specific Vault API name across all extracted MDL files.
     * Starts a background task to perform the search and displays results in a popup.
     *
     * @param project the active IntelliJ project
     * @param source  the UI component serving as the source/anchor for popups
     * @param apiName the Vault API name to search for
     */
    public static void findInMdl(Project project, JComponent source, String apiName) {
        if (apiName == null || apiName.isEmpty()) {
            return;
        }
        ToolboxProject toolboxProject = ToolboxProject.getInstance(project);
        File mdlDirectory = toolboxProject != null ? toolboxProject.getMdlDirectory() : null;
        VirtualFile root = mdlDirectory != null ? VfsUtil.findFileByIoFile(mdlDirectory, true) : null;
        if (root == null || !root.exists()) {
            JBPopupFactory.getInstance()
                    .createMessage("No extracted MDL found. Extract MDL first to search usages.")
                    .showInCenterOf(source);
            return;
        }

        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(apiName) + "\\b");
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Finding usages of " + apiName, true) {
            private final List<Match> matches = new ArrayList<>();

            /**
             * Runs the background scan process.
             *
             * @param indicator the progress indicator
             */
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                scan(root, pattern, indicator);
            }

            /**
             * Invoked on the Event Dispatch Thread (EDT) upon successful completion of the search.
             * Displays the matches in a navigable popup.
             */
            @Override
            public void onSuccess() {
                showResults(project, source, apiName, matches);
            }

            /**
             * Recursively scans files and directories for references matching the specified pattern.
             *
             * @param file      the file or directory to scan
             * @param pattern   the regular expression pattern to match
             * @param indicator the progress indicator to check for cancellation
             */
            private void scan(VirtualFile file, Pattern pattern, ProgressIndicator indicator) {
                indicator.checkCanceled();
                if (file.isDirectory()) {
                    for (VirtualFile child : file.getChildren()) {
                        scan(child, pattern, indicator);
                    }
                    return;
                }
                if (!"mdl".equalsIgnoreCase(file.getExtension())) {
                    return;
                }
                String text = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
                    try {
                        return VfsUtilCore.loadText(file);
                    } catch (Exception e) {
                        return null;
                    }
                });
                if (text == null) {
                    return;
                }
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    int offset = matcher.start();
                    matches.add(new Match(file, offset, lineNumber(text, offset), lineText(text, offset)));
                }
            }
        });
    }

    /**
     * Displays the list of matches in a choice popup, allowing the user to select
     * a match and navigate directly to its position in the file.
     *
     * @param project the active IntelliJ project
     * @param source  the UI component to anchor the popup to
     * @param apiName the API name that was searched
     * @param matches the list of found matches
     */
    private static void showResults(Project project, JComponent source, String apiName, List<Match> matches) {
        if (matches.isEmpty()) {
            JBPopupFactory.getInstance()
                    .createMessage("No usages of '" + apiName + "' found in MDL.")
                    .showInCenterOf(source);
            return;
        }
        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(matches)
                .setTitle("Usages of '" + apiName + "' (" + matches.size() + ")")
                .setItemChosenCallback(match -> ApplicationManager.getApplication().invokeLater(() ->
                        FileEditorManager.getInstance(project)
                                .openTextEditor(new OpenFileDescriptor(project, match.file, match.offset), true)))
                .createPopup()
                .showInCenterOf(source);
    }

    /**
     * Calculates the line number of a character offset within the provided text.
     *
     * @param text   the content of the file
     * @param offset the character offset
     * @return the 1-based line number containing the offset
     */
    private static int lineNumber(String text, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /**
     * Extracts the complete line of text containing the character offset.
     *
     * @param text   the content of the file
     * @param offset the character offset
     * @return the trimmed string of the line containing the offset
     */
    private static String lineText(String text, int offset) {
        int start = text.lastIndexOf('\n', offset) + 1;
        int end = text.indexOf('\n', offset);
        if (end < 0) {
            end = text.length();
        }
        return text.substring(start, end).trim();
    }

    /** A single reference match within an MDL file. */
    private static final class Match {
        private final VirtualFile file;
        private final int offset;
        private final int line;
        private final String lineText;

        /**
         * Constructs a Match object representing a single reference in an MDL file.
         *
         * @param file     the file containing the match
         * @param offset   the character offset of the match
         * @param line     the 1-based line number of the match
         * @param lineText the text of the line containing the match
         */
        Match(VirtualFile file, int offset, int line, String lineText) {
            this.file = file;
            this.offset = offset;
            this.line = line;
            this.lineText = lineText;
        }

        /**
         * Returns a user-friendly string representation of the match, including file name,
         * line number, and matching line text.
         *
         * @return the string representation of this match
         */
        @Override
        public String toString() {
            String name = file.getParent() != null ? file.getParent().getName() + "/" + file.getName() : file.getName();
            return name + ":" + line + "  —  " + lineText;
        }
    }
}
