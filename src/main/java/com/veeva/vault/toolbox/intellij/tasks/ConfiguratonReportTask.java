package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.veeva.vault.toolbox.core.results.DeploymentResult;
import com.veeva.vault.toolbox.core.results.ProgressResult;
import com.veeva.vault.toolbox.core.config.ConfigurationReport;
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

public class ConfiguratonReportTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(ConfiguratonReportTask.class);

    public static final AtomicBoolean isDownloading = new AtomicBoolean(false);

    private DeploymentResult reportResult;
    private File reportFolder;

    public ConfiguratonReportTask(@Nullable Project project) {
        super(project, "Downloading Configuration Report", true);
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            super.run(indicator);

            Consumer<ProgressResult> changeProgress = progressMessage -> {
                indicator.setText(progressMessage.getLabel());
            };

            ZonedDateTime now = ZonedDateTime.now();
            String dateFolder = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(now);
            assert toolboxProject != null;
            String vaultIdentifier = toolboxProject.getVaultId().toString();

            String nestedPath = vaultIdentifier + File.separator + dateFolder;
            reportFolder = new File(toolboxProject.getConfigDirectory(), nestedPath);

            if (!reportFolder.exists()) {
                reportFolder.mkdirs();
            }

            File outputFile = new File(reportFolder, "temp-report.zip");

            ConfigurationReport configurationReport = new ConfigurationReport(toolboxProject.getVaultClient());
            reportResult = configurationReport.downloadConfigurationReport(outputFile, changeProgress, true);

            if (outputFile.exists()) {
                outputFile.delete();
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            if (reportResult == null) {
                reportResult = new DeploymentResult();
            }
            reportResult.addErrorMessage("Exception: " + e.getMessage());
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        try {
            if (toolboxProject != null && reportResult != null) {
                Message message = toolboxProject.newMessage();

                if (reportResult.isError()) {
                    message.setTitle("Configuration Report Error");
                    message.append("Failed to download or extract the configuration report.");

                    for (String error : reportResult.getErrorMessages()) {
                        message.append("\n • " + error);
                    }
                    message.showError();
                } else {
                    message.setTitle("Configuration Report");
                    message.append("Configuration Report successfully downloaded and extracted!");

                    message.showInformation();

                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                        if (reportFolder != null && reportFolder.exists()) {
                            com.intellij.openapi.vfs.VirtualFile vFolder = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                                    .refreshAndFindFileByIoFile(reportFolder);

                            if (vFolder != null) {
                                vFolder.refresh(false, true);

                                com.intellij.openapi.wm.ToolWindow projectWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(toolboxProject.getProject())
                                        .getToolWindow(com.intellij.openapi.wm.ToolWindowId.PROJECT_VIEW);

                                if (projectWindow != null) {
                                    projectWindow.activate(() -> {
                                        com.intellij.ide.projectView.ProjectView.getInstance(toolboxProject.getProject())
                                                .select(null, vFolder, true);
                                    });
                                }
                            }
                        }
                    });
                }
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            isDownloading.set(false);
        }
    }

    @Override
    public void onCancel() {
        super.onCancel();
        isDownloading.set(false);
    }
}