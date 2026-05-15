package com.veeva.vault.toolbox.intellij.tasks;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates informational, warning, and error messages produced during a task execution.
 */
public class TaskResults {
	private final List<String> infos = new ArrayList<>();
	private final List<String> errors = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();

	/**
	 * Records an informational message.
	 *
	 * @param info the message to record
	 */
	public void addInfo(String info) {
		this.infos.add(info);
	}

	/**
	 * Records an error message.
	 *
	 * @param error the message to record
	 */
	public void addError(String error) {
		this.errors.add(error);
	}

	/**
	 * Records a warning message.
	 *
	 * @param warning the message to record
	 */
	public void addWarning(String warning) {
		this.warnings.add(warning);
	}

	/**
	 * @return all error messages collected during task execution
	 */
	public List<String> getErrors() {
		return errors;
	}

	/**
	 * @return all informational messages collected during task execution
	 */
	public List<String> getInfos() {
		return infos;
	}

	/**
	 * @return all warning messages collected during task execution
	 */
	public List<String> getWarnings() {
		return warnings;
	}
}
