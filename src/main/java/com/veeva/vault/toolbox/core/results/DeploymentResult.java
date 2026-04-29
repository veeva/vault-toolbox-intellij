package com.veeva.vault.toolbox.core.results;

import java.util.ArrayList;
import java.util.List;

public class DeploymentResult {
    private List<String> errorMessages = new ArrayList<>();
    private List<String> infoMessages = new ArrayList<>();
    private List<String> warnMessages = new ArrayList<>();

    public boolean isError() {
        return !errorMessages.isEmpty();
    }

    public boolean isSuccess() {
        return errorMessages.isEmpty() && warnMessages.isEmpty();
    }

    public boolean isWarning() {
        return !warnMessages.isEmpty() && errorMessages.isEmpty();
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public List<String> getInfoMessages() {
        return infoMessages;
    }

    public List<String> getWarnMessages() {
        return warnMessages;
    }

    public void addErrorMessage(String errorMessage) {
        errorMessages.add(errorMessage);
    }

    public void addInfoMessage(String infoMessage) {
        infoMessages.add(infoMessage);
    }

    public void addWarnMessage(String warnMessage) {
        warnMessages.add(warnMessage);
    }
}