package com.veeva.vault.toolbox.intellij.ui;

public class DeploymentItem<T> {
    private final T item;
    private boolean inVault;
    private boolean isLocal;

    public DeploymentItem(T item, boolean inVault, boolean isLocal) {
        this.item = item;
        this.inVault = inVault;
        this.isLocal = isLocal;
    }

    public T getItem() { return item; }
    public boolean isInVault() { return inVault; }
    public void setInVault(boolean inVault) { this.inVault = inVault; }
    public boolean isLocal() { return isLocal; }
    public void setLocal(boolean local) { this.isLocal = local; }
}
