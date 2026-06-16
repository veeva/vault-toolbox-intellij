package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.request.QueryRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Cancellable background task that runs a single read-only VQL query (or fetches a
 * specific result page) against the connected Vault and delivers the
 * {@link QueryResponse} back to the caller on the EDT.
 *
 * <p>VQL is read-only by design; this task never mutates Vault data. Either
 * {@code vql} (for a fresh query) or {@code pageUrl} (for pagination) is supplied;
 * exactly one is expected to be non-null.
 */
public class RunVqlQueryTask extends ToolboxTask {

    private final String vql;
    private final String pageUrl;
    private final BiConsumer<QueryResponse, Long> onResult;
    private final Consumer<String> onError;
    private final Runnable onFinally;

    private QueryResponse response;
    private long elapsedMillis;
    private boolean sessionExpired = false;
    private volatile ProgressIndicator indicator;

    /**
     * Creates a VQL query task.
     *
     * @param project   the IntelliJ project
     * @param vql       the VQL statement to execute, or {@code null} when loading a page
     * @param pageUrl   the relative page URL to fetch, or {@code null} for a fresh query
     * @param onResult  invoked on the EDT with a successful response and the query elapsed time (ms)
     * @param onError   invoked on the EDT with a user-facing error message
     * @param onFinally invoked on the EDT after the task finishes (success, error, or cancel)
     */
    public RunVqlQueryTask(@Nullable Project project,
                           @Nullable String vql,
                           @Nullable String pageUrl,
                           @NotNull BiConsumer<QueryResponse, Long> onResult,
                           @NotNull Consumer<String> onError,
                           @NotNull Runnable onFinally) {
        super(project, "Running VQL Query", true);
        this.vql = vql;
        this.pageUrl = pageUrl;
        this.onResult = onResult;
        this.onError = onError;
        this.onFinally = onFinally;
    }

    /** Requests cancellation of the in-flight query. */
    public void requestCancel() {
        ProgressIndicator current = indicator;
        if (current != null) {
            current.cancel();
        }
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        this.indicator = indicator;
        indicator.setIndeterminate(true);

        VaultClient client = toolboxProject.getVaultClient();
        if (client == null) {
            return;
        }

        QueryRequest request = client.newRequest(QueryRequest.class);
        long start = System.currentTimeMillis();
        response = (pageUrl != null) ? request.queryByPage(pageUrl) : request.query(vql);
        elapsedMillis = System.currentTimeMillis() - start;

        if (toolboxProject.handleSessionExpiration(response)) {
            sessionExpired = true;
        }
    }

    @Override
    public void onSuccess() {
        if (sessionExpired) {
            return;
        }
        if (response == null) {
            onError.accept("No response received from Vault.");
            return;
        }
        if (response.isFailure()) {
            onError.accept(extractErrorMessage(response));
            return;
        }
        onResult.accept(response, elapsedMillis);
    }

    @Override
    public void onThrowable(@NotNull Throwable error) {
        if (error instanceof Exception && toolboxProject.handleSessionExpiration((Exception) error)) {
            return;
        }
        onError.accept(error.getMessage() != null ? error.getMessage() : error.toString());
    }

    @Override
    public void onFinished() {
        super.onFinished();
        onFinally.run();
    }

    /**
     * Extracts a user-facing message from a failed response, mirroring the convention
     * used elsewhere (response message first, then the first error entry).
     */
    private static String extractErrorMessage(QueryResponse response) {
        String message = response.getResponseMessage();
        if ((message == null || message.isEmpty())
                && response.getErrors() != null && !response.getErrors().isEmpty()) {
            message = response.getErrors().get(0).getMessage();
        }
        return (message == null || message.isEmpty()) ? "Query failed." : message;
    }
}
