package com.veeva.vault.toolbox.intellij.ui;

import javax.swing.table.DefaultTableModel;
import java.util.Vector;

public class ToolboxTableModel extends DefaultTableModel {
	public void reorder(int from, int to) {
		Vector<?> o = getDataVector().remove(from);
		getDataVector().add(to, o);
		fireTableDataChanged();
	}
}
