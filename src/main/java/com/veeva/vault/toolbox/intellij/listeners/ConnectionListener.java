package com.veeva.vault.toolbox.intellij.listeners;

/**
 * Receives notifications about Vault connection state changes.
 */
public interface ConnectionListener {

	/**
	 * Invoked when a Vault connection has been successfully established.
	 */
	void connected();

	/**
	 * Invoked when the active Vault connection has been terminated.
	 */
	void disconnected();
}
