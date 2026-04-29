package com.veeva.vault.toolbox.intellij.tasks;

import java.util.ArrayList;
import java.util.List;

public class TaskResults {
	private final List<String> infos = new ArrayList<>();
	private final List<String> errors = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();

	public void addInfo(String info) {
		this.infos.add(info);
	}
	public void addError(String error) {
		this.errors.add(error);
	}
	public void addWarning(String warning) {
		this.warnings.add(warning);
	}

	public List<String> getErrors() {
		return errors;
	}

	public List<String> getInfos() {
		return infos;
	}

	public List<String> getWarnings() {
		return warnings;
	}

}
