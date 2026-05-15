package com.veeva.vault.toolbox.core.results;

/**
 * Carries a human-readable progress label emitted during a long-running operation.
 *
 * <p>Instances are passed to a {@code Consumer<ProgressResult>} callback so that callers
 * can relay status updates to the UI (e.g., an IDE progress indicator) without coupling
 * the core logic to any particular UI framework.
 */
public class ProgressResult {
    private String label;

    /**
     * Creates a new progress result with the given label.
     *
     * @param label a short, human-readable description of the current operation step
     */
    public ProgressResult(String label) {
        this.label = label;
    }

    /**
     * Returns the current progress label.
     *
     * @return the progress label, never {@code null}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Updates the progress label.
     *
     * @param label the new progress label
     */
    public void setLabel(String label) {
        this.label = label;
    }
}
