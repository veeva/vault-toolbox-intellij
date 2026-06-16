package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.veeva.vault.toolbox.core.config.ConfigurationReport;
import com.veeva.vault.toolbox.core.results.DeploymentResult;
import com.veeva.vault.toolbox.core.results.ProgressResult;
import com.veeva.vault.toolbox.intellij.ui.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Downloads and extracts the Vault configuration report into a date-stamped folder
 * under the project's configuration directory. A shared {@link #isDownloading} flag
 * is exposed so callers can avoid launching multiple concurrent downloads.
 */
public class ConfiguratonReportTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(ConfiguratonReportTask.class);
    private static final DateTimeFormatter DATE_FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Tracks whether a configuration-report download is currently in flight so the UI
     * can prevent overlapping requests.
     */
    public static final AtomicBoolean isDownloading = new AtomicBoolean(false);

    private DeploymentResult reportResult;
    private File reportFolder;
    private ConfigurationReport.Options options;

    /**
     * @param project the IntelliJ project, may be {@code null}
     */
    public ConfiguratonReportTask(@Nullable Project project, ConfigurationReport.Options options) {
        super(project, "Downloading Configuration Report", true);
        this.options = options;
    }

    /**
     * Downloads the configuration report from Vault and saves it to a zip file.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            Consumer<ProgressResult> changeProgress = progressMessage ->
                    indicator.setText(progressMessage.getLabel());

            assert toolboxProject != null;
            String dateFolder = DATE_FOLDER_FORMAT.format(ZonedDateTime.now());
            String vaultIdentifier = toolboxProject.getVaultId().toString();
            String nestedPath = vaultIdentifier + File.separator + dateFolder;
            reportFolder = new File(toolboxProject.getConfigDirectory(), nestedPath);

            if (!reportFolder.exists()) {
                reportFolder.mkdirs();
            }

            File outputFile = new File(reportFolder, "temp-report.zip");

            ConfigurationReport configurationReport = new ConfigurationReport(toolboxProject.getVaultClient());
            reportResult = configurationReport.downloadConfigurationReport(outputFile, changeProgress, true, indicator::isCanceled, options);

            if (outputFile.exists()) {
                outputFile.delete();
            }
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            if (reportResult == null) {
                reportResult = new DeploymentResult();
            }
            reportResult.addErrorMessage("Exception: " + e.getMessage());
        }

        indicator.checkCanceled();
    }

    /**
     * Extracts the report, notifies the user, and reveals the folder in the project view.
     */
    @Override
    public void onSuccess() {
        super.onSuccess();
        try {
            if (toolboxProject == null || reportResult == null) {
                return;
            }
            Message message = toolboxProject.newMessage();
            if (reportResult.isError()) {
                message.setTitle("Configuration Report Error");
                message.append("Failed to download or extract the configuration report.");
                for (String error : reportResult.getErrorMessages()) {
                    message.append("\n • " + error);
                }
                message.showError();
                return;
            }
            message.setTitle("Configuration Report");
            message.append("Configuration Report successfully downloaded and extracted!");
            message.showInformation();

            ApplicationManager.getApplication().invokeLater(this::revealReportFolder);
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            isDownloading.set(false);
        }
    }

    /**
     * Refreshes the VFS for the report folder and selects it in the Project view so
     * the user can immediately browse the freshly downloaded files.
     */
    private void revealReportFolder() {
        if (reportFolder == null || !reportFolder.exists()) {
            return;
        }
        VirtualFile vFolder = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(reportFolder);
        if (vFolder == null) {
            return;
        }
        vFolder.refresh(false, true);

        ToolWindow projectWindow = ToolWindowManager.getInstance(toolboxProject.getProject())
                .getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (projectWindow != null) {
            projectWindow.activate(() ->
                    ProjectView.getInstance(toolboxProject.getProject()).select(null, vFolder, true));
        }
    }

    /**
     * Resets the downloading flag if the task is cancelled.
     */
    @Override
    public void onCancel() {
        super.onCancel();
        isDownloading.set(false);
    }
}
