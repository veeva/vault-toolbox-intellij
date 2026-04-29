package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.veeva.vault.vapil.api.model.VaultModel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ToolboxDataGrid extends JPanel {
	ToolboxTableModel tableModel = new ToolboxTableModel();
	JCheckBox selectAllCheck;
	List<String> columnsName = null;
	List<? extends VaultModel> data;

	public ToolboxDataGrid() {
		columnsName = new ArrayList<String>();
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

	public ToolboxDataGrid(List<String> columnsName, List<? extends VaultModel> data, boolean includeSelectAll) {
		this.columnsName = columnsName;
		this.data = data;
		init(includeSelectAll);
	}

	protected void toggleSelect() {
		try {
			Boolean newValue = selectAllCheck.isSelected();
			for(int row = 0;row < tableModel.getRowCount();row++) {
				tableModel.setValueAt(newValue,row,0);
			}
			selectAllCheck.revalidate();
			repaint();
		}
		catch (Exception e) {
		}
	}

    public void init(boolean includeSelectAll) {
		this.setLayout(new BorderLayout());
		tableModel = new ToolboxTableModel() {
			public Class<?> getColumnClass(int column) {
				switch (column) {
					case 0:
						return Boolean.class;
					default:
						return String.class;
				}
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
			tableModel.addColumn("include");
			table.getColumnModel().getColumn(0).setMinWidth(75);
			table.getColumnModel().getColumn(0).setMaxWidth(75);
		}


		JPanel dataPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JBScrollPane scrollPane = new JBScrollPane(table);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(800,250));
		//dataPanel.add(scrollPane);

		JPanel controlPanel = new JPanel(new BorderLayout());

		if (includeSelectAll) {
			selectAllCheck = new JCheckBox();
			selectAllCheck.setText("Select All");
			selectAllCheck.addActionListener(e -> {
				toggleSelect();
			});
			controlPanel.add(selectAllCheck, BorderLayout.WEST);
		}



		add(dataPanel, BorderLayout.CENTER);
		add(controlPanel, BorderLayout.SOUTH);
        setVisible(true);
    }
}