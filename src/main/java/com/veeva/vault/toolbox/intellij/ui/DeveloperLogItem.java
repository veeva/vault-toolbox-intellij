package com.veeva.vault.toolbox.intellij.ui;

/**
 * A wrapper class for developer log items that tracks their presence in the remote Vault
 * and on the local filesystem.
 *
 * @param <T> The type of the underlying log session or data object.
 */
public class DeveloperLogItem<T> {
    private T item;
    private boolean inVault;
    private boolean isLocal;

    /**
     * Initializes a new developer log item.
     *
     * @param item    The underlying log data object.
     * @param inVault true if the log exists in the remote Vault.
     * @param isLocal true if the log has been downloaded to the local machine.
     */
    public DeveloperLogItem(T item, boolean inVault, boolean isLocal) {
        this.item = item;
        this.inVault = inVault;
        this.isLocal = isLocal;
    }

    /**
     * @return The underlying log data object.
     */
    public T getItem() {
        return item;
    }

    /**
     * @return true if the log exists in the remote Vault.
     */
    public boolean isInVault() {
        return inVault;
    }

    /**
     * @param inVault Whether the log exists in the remote Vault.
     */
    public void setInVault(boolean inVault) {
        this.inVault = inVault;
    }

    /**
     * @return true if the log is stored on the local machine.
     */
    public boolean isLocal() {
        return isLocal;
    }

    /**
     * @param local Whether the log is stored on the local machine.
     */
    public void setLocal(boolean local) {
        this.isLocal = local;
    }
}
