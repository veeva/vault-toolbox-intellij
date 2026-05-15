package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.veeva.vault.toolbox.core.logs.api.ApiUsageLog;
import com.veeva.vault.toolbox.core.utils.FileIO;
import com.veeva.vault.toolbox.intellij.ui.Message;
import com.veeva.vault.vapil.api.model.response.VaultResponse;
import com.veeva.vault.vapil.api.request.AuthenticationRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads API usage logs for a given date range and organizes the downloaded files
 * into per-date subfolders. An optional callback is invoked after a successful run.
 */
public class DownloadApiLogTask extends ToolboxTask {
    private static final Logger logger = LoggerFactory.getLogger(DownloadApiLogTask.class);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Runnable onComplete;
    private boolean isSuccess = false;
    private File outputDirectory;

    /**
     * @param project   the IntelliJ project, may be {@code null}
     * @param startDate inclusive start of the date range to download
     * @param endDate   inclusive end of the date range to download
     */
    public DownloadApiLogTask(@Nullable Project project, LocalDate startDate, LocalDate endDate) {
        this(project, startDate, endDate, null);
    }

    /**
     * @param project    the IntelliJ project, may be {@code null}
     * @param startDate  inclusive start of the date range to download
     * @param endDate    inclusive end of the date range to download
     * @param onComplete optional callback invoked after a successful download
     */
    public DownloadApiLogTask(@Nullable Project project,
                              LocalDate startDate,
                              LocalDate endDate,
                              Runnable onComplete) {
        super(project, "Downloading API Usage Logs");
        this.startDate = startDate;
        this.endDate = endDate;
        this.onComplete = onComplete;
    }

    /**
     * Authenticates with Vault, downloads API logs for the specified range, and organizes them.
     *
     * @param indicator the progress indicator for the background task
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            VaultResponse authCheck = toolboxProject.getVaultClient()
                    .newRequest(AuthenticationRequest.class)
                    .sessionKeepAlive();

            if (toolboxProject.handleSessionExpiration(authCheck)) {
                return;
            }

            outputDirectory = new File(toolboxProject.getLogsDirectory().getPath(), "/api/" + toolboxProject.getVaultId());
            FileIO.makeDirectories(outputDirectory);

            ApiUsageLog apiUsageLog = new ApiUsageLog();
            apiUsageLog.download(toolboxProject.getVaultClient(), startDate, endDate, outputDirectory, true);

            organizeFilesByDate(outputDirectory);
            isSuccess = true;
        }
        catch (Exception e) {
            if (toolboxProject.handleSessionExpiration(e)) {
                return;
            }
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Moves each downloaded log file into a subdirectory named after the date encoded
     * in its filename.
     *
     * @param outputDirectory the directory containing the freshly downloaded log files
     * @throws Exception if moving a file fails
     */
    private void organizeFilesByDate(File outputDirectory) throws Exception {
        File[] downloadedFiles = outputDirectory.listFiles();
        if (downloadedFiles == null) {
            return;
        }
        File lastDateFolder = null;
        for (File file : downloadedFiles) {
            if (!file.isFile()) {
                continue;
            }
            Matcher matcher = DATE_PATTERN.matcher(file.getName());
            if (!matcher.find()) {
                continue;
            }
            File dateFolder = new File(outputDirectory, matcher.group());
            if (!dateFolder.exists()) {
                FileIO.makeDirectories(dateFolder);
            }
            Files.move(file.toPath(), new File(dateFolder, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (lastDateFolder == null || dateFolder.getName().compareTo(lastDateFolder.getName()) > 0) {
                lastDateFolder = dateFolder;
            }
        }
        if (lastDateFolder != null) {
            this.outputDirectory = lastDateFolder;
        }
    }

    /**
     * Notifies the user of completion, executes the completion callback, and reveals the output directory in the project view.
     */
    @Override
    public void onSuccess() {
        super.onSuccess();
        if (!isSuccess) {
            return;
        }
        try {
            if (toolboxProject != null) {
                Message message = toolboxProject.newMessage();
                message.setTitle("Download");
                message.append("Download Completed");
                message.showInformation();
            }
            if (onComplete != null) {
                onComplete.run();
            }
            VirtualFile vOutputDir = VfsUtil.findFileByIoFile(outputDirectory, true);
            selectInProjectView(vOutputDir);
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
