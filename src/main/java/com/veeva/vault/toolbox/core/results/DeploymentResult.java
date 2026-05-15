package com.veeva.vault.toolbox.core.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumulates messages produced during a deployment or configuration-report operation
 * and exposes the overall outcome as a status flag.
 *
 * <p>Messages are separated into three severity buckets:
 * <ul>
 *   <li><b>error</b> – fatal issues that prevented a successful deployment</li>
 *   <li><b>warn</b> – non-fatal issues that require attention</li>
 *   <li><b>info</b> – informational notes with no action required</li>
 * </ul>
 *
 * <p>Status flags are mutually exclusive: {@link #isError()} takes priority over
 * {@link #isWarning()}, which takes priority over {@link #isSuccess()}.
 */
public class DeploymentResult {
    private final List<String> errorMessages = new ArrayList<>();
    private final List<String> infoMessages = new ArrayList<>();
    private final List<String> warnMessages = new ArrayList<>();

    /**
     * Returns {@code true} if at least one error message has been recorded.
     *
     * @return {@code true} when errors are present
     */
    public boolean isError() {
        return !errorMessages.isEmpty();
    }

    /**
     * Returns {@code true} if no error or warning messages have been recorded.
     *
     * @return {@code true} when the operation completed without errors or warnings
     */
    public boolean isSuccess() {
        return errorMessages.isEmpty() && warnMessages.isEmpty();
    }

    /**
     * Returns {@code true} if at least one warning message has been recorded and no
     * error messages are present.
     *
     * @return {@code true} when warnings exist but no errors
     */
    public boolean isWarning() {
        return !warnMessages.isEmpty() && errorMessages.isEmpty();
    }

    /**
     * Returns an unmodifiable view of all recorded error messages.
     *
     * @return error messages, never {@code null}
     */
    public List<String> getErrorMessages() {
        return Collections.unmodifiableList(errorMessages);
    }

    /**
     * Returns an unmodifiable view of all recorded informational messages.
     *
     * @return info messages, never {@code null}
     */
    public List<String> getInfoMessages() {
        return Collections.unmodifiableList(infoMessages);
    }

    /**
     * Returns an unmodifiable view of all recorded warning messages.
     *
     * @return warning messages, never {@code null}
     */
    public List<String> getWarnMessages() {
        return Collections.unmodifiableList(warnMessages);
    }

    /**
     * Appends an error message to this result.
     *
     * @param errorMessage the error message to record
     */
    public void addErrorMessage(String errorMessage) {
        errorMessages.add(errorMessage);
    }

    /**
     * Appends an informational message to this result.
     *
     * @param infoMessage the informational message to record
     */
    public void addInfoMessage(String infoMessage) {
        infoMessages.add(infoMessage);
    }

    /**
     * Appends a warning message to this result.
     *
     * @param warnMessage the warning message to record
     */
    public void addWarnMessage(String warnMessage) {
        warnMessages.add(warnMessage);
    }
}
