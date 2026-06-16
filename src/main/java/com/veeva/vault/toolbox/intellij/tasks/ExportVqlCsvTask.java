package com.veeva.vault.toolbox.intellij.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.opencsv.CSVWriter;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.model.response.QueryResponse;
import com.veeva.vault.vapil.api.request.QueryRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Background task that runs a read-only VQL query and streams all result pages to a CSV
 * file, up to {@code maxRows}. Re-runs the query from the first page (VQL is idempotent),
 * following {@code nextPage} links until exhausted, the row cap is reached, or the user
 * cancels.
 */
public class ExportVqlCsvTask extends ToolboxTask {

    private final String vql;
    private final List<String> columns;
    private final File file;
    private final int maxRows;
    private final Consumer<Integer> onDone;
    private final Consumer<String> onError;
    private final Runnable onFinally;

    private int written = 0;
    private boolean truncated = false;
    private String errorMessage;

    /**
     * @param project the IntelliJ project
     * @param vql     the VQL statement to export
     * @param columns the ordered output columns (header and per-row value keys)
     * @param file    the destination CSV file
     * @param maxRows the maximum number of data rows to write
     * @param onDone    invoked on the EDT with the number of rows written (and whether truncated)
     * @param onError   invoked on the EDT with a user-facing error message
     * @param onFinally invoked on the EDT after the task finishes (success, error, or cancel)
     */
    public ExportVqlCsvTask(@Nullable Project project,
                            @NotNull String vql,
                            @NotNull List<String> columns,
                            @NotNull File file,
                            int maxRows,
                            @NotNull Consumer<Integer> onDone,
                            @NotNull Consumer<String> onError,
                            @NotNull Runnable onFinally) {
        super(project, "Exporting VQL Results", true);
        this.vql = vql;
        this.columns = columns;
        this.file = file;
        this.maxRows = maxRows;
        this.onDone = onDone;
        this.onError = onError;
        this.onFinally = onFinally;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);

        VaultClient client = toolboxProject.getVaultClient();
        if (client == null) {
            errorMessage = "Not connected to a Vault.";
            return;
        }

        QueryRequest request = client.newRequest(QueryRequest.class);
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(columns.toArray(new String[0]));

            QueryResponse response = request.query(vql);
            while (response != null) {
                if (toolboxProject.handleSessionExpiration(response)) {
                    errorMessage = "Session expired during export.";
                    return;
                }
                if (response.isFailure()) {
                    errorMessage = extractErrorMessage(response);
                    return;
                }

                if (response.getData() != null) {
                    for (QueryResponse.QueryResult row : response.getData()) {
                        if (written >= maxRows) {
                            truncated = true;
                            break;
                        }
                        indicator.checkCanceled();
                        String[] cells = new String[columns.size()];
                        for (int i = 0; i < columns.size(); i++) {
                            cells[i] = cellString(row.get(columns.get(i)));
                        }
                        writer.writeNext(cells);
                        written++;
                        indicator.setText(written + " rows exported");
                    }
                }

                QueryResponse.ResponseDetails details = response.getResponseDetails();
                String nextPage = details != null ? details.getNextPage() : null;
                if (truncated || nextPage == null) {
                    break;
                }
                response = request.queryByPage(nextPage);
            }
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
        }
    }

    @Override
    public void onSuccess() {
        if (errorMessage != null) {
            onError.accept(errorMessage);
        } else {
            onDone.accept(written);
        }
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
     * Renders a raw VQL result value as a flat CSV/grid cell string. VQL fields are not
     * all scalars: multi-value fields arrive as lists and relationship subqueries as
     * nested structures, so a plain {@code (String)} cast (e.g. {@code VaultModel.getString})
     * would throw. Lists are joined; everything else uses {@code String.valueOf}.
     */
    public static String cellString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(cellString(item));
                first = false;
            }
            return sb.toString();
        }
        return String.valueOf(value);
    }

    private static String extractErrorMessage(QueryResponse response) {
        String message = response.getResponseMessage();
        if ((message == null || message.isEmpty())
                && response.getErrors() != null && !response.getErrors().isEmpty()) {
            message = response.getErrors().get(0).getMessage();
        }
        return (message == null || message.isEmpty()) ? "Export failed." : message;
    }
}
