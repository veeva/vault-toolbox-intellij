package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.veeva.vault.vapil.api.model.VaultModel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A reusable data grid component for displaying lists of {@link VaultModel} objects.
 * Supports automatic column generation and an optional "Select All" feature.
 */
public class ToolboxDataGrid extends JPanel {
	private ToolboxTableModel tableModel = new ToolboxTableModel();
	private JCheckBox selectAllCheck;
	private List<String> columnsName = null;
	private List<? extends VaultModel> data;

	/**
	 * Default constructor that initializes the grid with sample data.
	 */
	public ToolboxDataGrid() {
		columnsName = new ArrayList<>();
		columnsName.add("code_type");
		columnsName.add("class_name");

		List<VaultModel> newData = new ArrayList<>();
		VaultModel model = new VaultModel();
		model.set("code_type", "Recordtrigger");
		model.set("class_name", "com.veeva.vault.custom.TestTrigger");
		newData.add(model);
		data = newData;

		init(true);
	}

	/**
	 * Initializes the grid with specific columns and data.
	 *
	 * @param columnsName      List of column keys to extract from the models.
	 * @param data             List of models to display.
	 * @param includeSelectAll true to include a selection checkbox column.
	 */
	public ToolboxDataGrid(List<String> columnsName, List<? extends VaultModel> data, boolean includeSelectAll) {
		this.columnsName = columnsName;
		this.data = data;
		init(includeSelectAll);
	}

	/**
	 * Synchronizes all row selection states with the state of the "Select All" checkbox.
	 */
	protected void toggleSelect() {
		try {
			boolean newValue = selectAllCheck.isSelected();
			for(int row = 0; row < tableModel.getRowCount(); row++) {
				tableModel.setValueAt(newValue, row, 0);
			}
			selectAllCheck.revalidate();
			repaint();
		} catch (Exception ignored) {
		}
	}

	/**
	 * Configures the internal table model and UI layout.
	 *
	 * @param includeSelectAll true to add the selection column.
	 */
    public void init(boolean includeSelectAll) {
		this.setLayout(new BorderLayout());
		tableModel = new ToolboxTableModel() {
			@Override
			public Class<?> getColumnClass(int column) {
				if (column == 0 && includeSelectAll) {
					return Boolean.class;
				}
				return String.class;
			}
		};

		if (includeSelectAll) {
			tableModel.addColumn("include");
		}
		for (String columnName : columnsName) {
			tableModel.addColumn(columnName);
		}
		for (VaultModel vaultModel : data) {
			Object[] row = new Object[includeSelectAll ? columnsName.size() + 1 : columnsName.size()];
			if (includeSelectAll) {
				row[0] = Boolean.FALSE;
			}
			for (int i = 0; i < columnsName.size(); i++) {
				int colummIndex = includeSelectAll ? i + 1 : i;
				String columnName = columnsName.get(i);
				row[colummIndex] = vaultModel.getString(columnName);
			}
			tableModel.addRow(row);
		}

		JBTable table = new JBTable(tableModel);
		if (includeSelectAll) {
			table.getColumnModel().getColumn(0).setMinWidth(75);
			table.getColumnModel().getColumn(0).setMaxWidth(75);
		}
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		TableUtils.autoResizeColumns(table);

		JBScrollPane scrollPane = new JBScrollPane(table);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(800, 250));

		JPanel controlPanel = new JPanel(new BorderLayout());

		if (includeSelectAll) {
			selectAllCheck = new JCheckBox();
			selectAllCheck.setText("Select All");
			selectAllCheck.addActionListener(e -> toggleSelect());
			controlPanel.add(selectAllCheck, BorderLayout.WEST);
		}

		add(scrollPane, BorderLayout.CENTER);
		add(controlPanel, BorderLayout.SOUTH);
        setVisible(true);
    }
}
