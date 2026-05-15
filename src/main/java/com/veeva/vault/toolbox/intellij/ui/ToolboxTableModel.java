package com.veeva.vault.toolbox.intellij.ui;

import javax.swing.table.DefaultTableModel;
import java.util.Vector;

/**
 * A specialized table model for the Vault Toolbox that supports row reordering.
 */
public class ToolboxTableModel extends DefaultTableModel {

	/**
	 * Reorders a row from one index to another.
	 *
	 * @param from The current index of the row.
	 * @param to   The destination index.
	 */
	public void reorder(int from, int to) {
		Vector<?> o = getDataVector().remove(from);
		getDataVector().add(to, o);
		fireTableDataChanged();
	}
}
