package com.veeva.vault.toolbox.intellij.ui;

/**
 * A wrapper class for deployment items that tracks their presence in Vault and on the local filesystem.
 *
 * @param <T> The type of the underlying item (e.g., File or QueryResult).
 */
public class DeploymentItem<T> {
    private final T item;
    private boolean inVault;
    private boolean isLocal;

    /**
     * Initializes a new deployment item.
     *
     * @param item    The underlying data object.
     * @param inVault Whether the item exists in the remote Vault.
     * @param isLocal Whether the item exists on the local machine.
     */
    public DeploymentItem(T item, boolean inVault, boolean isLocal) {
        this.item = item;
        this.inVault = inVault;
        this.isLocal = isLocal;
    }

    /**
     * @return The underlying data object.
     */
    public T getItem() {
        return item;
    }

    /**
     * @return true if the item exists in the remote Vault.
     */
    public boolean isInVault() {
        return inVault;
    }

    /**
     * @param inVault Whether the item exists in the remote Vault.
     */
    public void setInVault(boolean inVault) {
        this.inVault = inVault;
    }

    /**
     * @return true if the item exists on the local machine.
     */
    public boolean isLocal() {
        return isLocal;
    }

    /**
     * @param local Whether the item exists on the local machine.
     */
    public void setLocal(boolean local) {
        this.isLocal = local;
    }
}
