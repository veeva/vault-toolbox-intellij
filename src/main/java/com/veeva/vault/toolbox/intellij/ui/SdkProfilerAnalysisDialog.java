package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.vapil.api.model.common.SdkProfilingSession;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class SdkProfilerAnalysisDialog extends DialogWrapper {

	private final Map<SdkProfilingSession, File> sessionFiles;
	private final Map<SdkProfilingSession, List<Map<String, String>>> parsedSessionData = new HashMap<>();

	private LocalDateTime minDateTime = null;
	private LocalDateTime maxDateTime = null;

	private DateTimePickerControl startDatePicker;
	private DateTimePickerControl endDatePicker;
	private ComboBox<String> usernameDropdown;
	private JButton executionIdButton;

	private final Set<String> allExecutionIds = new TreeSet<>();
	private final Set<String> selectedExecutionIds = new HashSet<>();

	private static class MetricDef {
		String title;
		String csvKey;
		String yAxisLabel;
		DefaultCategoryDataset dataset;
		JFreeChart chart;
		boolean hasData;

		MetricDef(String title, String csvKey, String yAxisLabel) {
			this.title = title;
			this.csvKey = csvKey;
			this.yAxisLabel = yAxisLabel;
			this.dataset = new DefaultCategoryDataset();
		}
	}

	private final List<MetricDef> metrics = Arrays.asList(
			new MetricDef("SDK Count", "sdk_count", "Count"),
			new MetricDef("SDK CPU Time", "sdk_cpu_time", "Time (ms)"),
			new MetricDef("SDK Elapsed Time", "sdk_elapsed_time", "Time (ms)"),
			new MetricDef("SDK Gross Memory", "sdk_gross_memory", "Memory (bytes)"),
			new MetricDef("Action Trigger Count", "action_trigger_count", "Count"),
			new MetricDef("Action Trigger Elapsed Time", "action_trigger_elapsed_time", "Time (ms)")
	);

	public SdkProfilerAnalysisDialog(@Nullable Project project, Map<SdkProfilingSession, File> sessionFiles) {
		super(project, true);
		this.sessionFiles = sessionFiles;
		setTitle("Analyze SDK Profiler Logs");
		setModal(false);

		loadCsvData();
		selectedExecutionIds.addAll(allExecutionIds);
		calculateDateBounds();
		init();
	}

	private void loadCsvData() {
		for (Map.Entry<SdkProfilingSession, File> entry : sessionFiles.entrySet()) {
			List<Map<String, String>> rows = new ArrayList<>();
			File file = entry.getValue();

			try (BufferedReader br = new BufferedReader(new FileReader(file))) {
				String headerLine = br.readLine();
				if (headerLine != null) {
					String[] headers = headerLine.split(",");
					for (int i = 0; i < headers.length; i++) {
						headers[i] = headers[i].replace("\"", "").trim();
					}

					String line;
					while ((line = br.readLine()) != null) {
						String[] values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
						Map<String, String> row = new HashMap<>();
						for (int i = 0; i < headers.length; i++) {
							String val = i < values.length ? values[i].replace("\"", "").trim() : "";
							row.put(headers[i], val);
						}
						rows.add(row);

						String execId = row.get("execution_id");
						if (execId != null && !execId.isEmpty()) {
							allExecutionIds.add(execId);
						}
					}
				}
			} catch (IOException ignored) {}
			parsedSessionData.put(entry.getKey(), rows);
		}
	}

	private LocalDateTime parseTimestamp(String tsStr) {
		if (tsStr == null || tsStr.isEmpty()) return null;
		try {
			return LocalDateTime.parse(tsStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS"));
		} catch (Exception e1) {
			try {
				return ZonedDateTime.parse(tsStr).toLocalDateTime();
			} catch (Exception e2) {
				return null;
			}
		}
	}

	private void calculateDateBounds() {
		LocalDateTime min = LocalDateTime.MAX;
		LocalDateTime max = LocalDateTime.MIN;

		for (List<Map<String, String>> rows : parsedSessionData.values()) {
			for (Map<String, String> row : rows) {
				LocalDateTime ts = parseTimestamp(row.get("timestamp"));
				if (ts != null) {
					if (ts.isBefore(min)) min = ts;
					if (ts.isAfter(max)) max = ts;
				}
			}
		}

		if (min != LocalDateTime.MAX) {
			int minMinute = min.getMinute() < 30 ? 0 : 30;
			this.minDateTime = min.withMinute(minMinute).withSecond(0).withNano(0);
		}
		if (max != LocalDateTime.MIN) {
			// Round UP to the next 30-minute window
			int minute = max.getMinute();
			if (minute < 30) {
				this.maxDateTime = max.withMinute(30).withSecond(0).withNano(0);
			} else {
				this.maxDateTime = max.plusHours(1).withMinute(0).withSecond(0).withNano(0);
			}
		}
	}

	@Override
	protected @Nullable JComponent createCenterPanel() {
		JPanel mainPanel = new JPanel(new BorderLayout(JBUI.scale(10), JBUI.scale(10)));
		mainPanel.setPreferredSize(new Dimension(950, 700));

		// Initialize the filter components
		JPanel filterPanel = createFilterPanel();
		mainPanel.add(filterPanel, BorderLayout.NORTH);

		mainPanel.add(createTabbedChartPanel(), BorderLayout.CENTER);

		updateChartData();
		return mainPanel;
	}

	private JPanel createFilterPanel() {
		JPanel filterPanel = new JPanel(new GridBagLayout());
		filterPanel.setBorder(JBUI.Borders.compound(
				BorderFactory.createTitledBorder(BorderFactory.createLineBorder(JBColor.border()), "Filters"),
				JBUI.Borders.empty(10)
		));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = JBUI.insets(5, 5, 5, 10);

		// Row 0: Start and End Time
		gbc.gridy = 0; gbc.gridx = 0;
		gbc.weightx = 0.0;
		filterPanel.add(new JBLabel("Start Time:"), gbc);

		gbc.gridx = 1;
		startDatePicker = new DateTimePickerControl(minDateTime, maxDateTime);
		if (minDateTime != null) startDatePicker.setDateTime(minDateTime);
		filterPanel.add(startDatePicker, gbc);

		gbc.gridx = 2;
		filterPanel.add(new JBLabel("End Time:"), gbc);

		gbc.gridx = 3;
		gbc.gridwidth = 2; // Span across to keep alignment
		endDatePicker = new DateTimePickerControl(minDateTime, maxDateTime);
		if (maxDateTime != null) endDatePicker.setDateTime(maxDateTime);
		filterPanel.add(endDatePicker, gbc);

		// Row 1: Username and Execution IDs
		gbc.gridy = 1; gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		filterPanel.add(new JBLabel("Username:"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 0.3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		usernameDropdown = new ComboBox<>();
		usernameDropdown.setEditable(true);
		populateUsernames();

		Component editorComponent = usernameDropdown.getEditor().getEditorComponent();
		if (editorComponent instanceof JTextField) {
			((JTextField) editorComponent).getDocument().addDocumentListener(new DocumentAdapter() {
				@Override
				protected void textChanged(DocumentEvent e) { updateChartData(); }
			});
		}
		usernameDropdown.addItemListener(e -> {
			if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
				updateChartData();
			}
		});
		filterPanel.add(usernameDropdown, gbc);

		gbc.gridx = 2;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		filterPanel.add(new JBLabel("Execution IDs:"), gbc);

		gbc.gridx = 3;
		executionIdButton = new JButton("Execution Ids (" + selectedExecutionIds.size() + ")");
		executionIdButton.addActionListener(e -> showExecutionIdPopup(executionIdButton));
		filterPanel.add(executionIdButton, gbc);

		startDatePicker.addChangeListener(this::updateChartData);
		endDatePicker.addChangeListener(this::updateChartData);

		return filterPanel;
		}

	private void showExecutionIdPopup(Component owner) {
		CheckBoxList<String> checkBoxList = new CheckBoxList<>();
		for (String id : allExecutionIds) {
			checkBoxList.addItem(id, id, selectedExecutionIds.contains(id));
		}
		checkBoxList.setCheckBoxListListener((index, isChecked) -> {
			String id = checkBoxList.getItemAt(index);
			if (isChecked) {
				selectedExecutionIds.add(id);
			} else {
				selectedExecutionIds.remove(id);
			}
			updateExecutionIdButtonLabel();
			updateChartData();
		});

		JBScrollPane scrollPane = new JBScrollPane(checkBoxList);
		scrollPane.setPreferredSize(new Dimension(300, 400));

		JBPopup popup = JBPopupFactory.getInstance()
				.createComponentPopupBuilder(scrollPane, null)
				.setRequestFocus(true)
				.setCancelOnClickOutside(true)
				.createPopup();

		popup.showUnderneathOf(owner);
	}

	private void updateExecutionIdButtonLabel() {
		if (executionIdButton != null) {
			executionIdButton.setText("Execution Ids (" + selectedExecutionIds.size() + ")");
		}
	}

	private JComponent createTabbedChartPanel() {
		JBTabbedPane tabbedPane = new JBTabbedPane();

		for (MetricDef metric : metrics) {
			metric.chart = ChartFactory.createBarChart(
					null, "", metric.yAxisLabel, metric.dataset,
					PlotOrientation.VERTICAL, true, true, false);

			themeChart(metric.chart);

			ChartPanel panel = new ChartPanel(metric.chart);
			panel.setBorder(JBUI.Borders.empty(10));
			tabbedPane.addTab(metric.title, panel);
		}

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBorder(JBUI.Borders.customLine(JBColor.border(), 1));
		wrapper.add(tabbedPane, BorderLayout.CENTER);

		return wrapper;
	}

	private void themeChart(JFreeChart chart) {
		chart.setBackgroundPaint(JBColor.background());
		chart.getLegend().setBackgroundPaint(JBColor.background());
		chart.getLegend().setItemPaint(JBColor.foreground());

		CategoryPlot plot = chart.getCategoryPlot();
		plot.setBackgroundPaint(JBColor.background());
		plot.setDomainGridlinePaint(JBColor.border());
		plot.setRangeGridlinePaint(JBColor.border());

		plot.getDomainAxis().setTickLabelPaint(JBColor.foreground());
		plot.getDomainAxis().setLabelPaint(JBColor.foreground());
		plot.getRangeAxis().setTickLabelPaint(JBColor.foreground());
		plot.getRangeAxis().setLabelPaint(JBColor.foreground());

		plot.setNoDataMessage("No Data");
		plot.setNoDataMessagePaint(JBColor.foreground());
		plot.setNoDataMessageFont(new Font("SansSerif", Font.BOLD, 16));

		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setBarPainter(new StandardBarPainter());
		renderer.setShadowVisible(false);
		renderer.setMaximumBarWidth(0.20);

		renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		renderer.setDefaultItemLabelsVisible(true);
		renderer.setDefaultItemLabelPaint(JBColor.foreground());
		renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.PLAIN, 12));
	}

	private void populateUsernames() {
		usernameDropdown.addItem("All Users");
		Set<String> uniqueUsers = new TreeSet<>();

		for (List<Map<String, String>> rows : parsedSessionData.values()) {
			for (Map<String, String> row : rows) {
				String user = row.get("user_name");
				if (user != null && !user.isEmpty()) {
					uniqueUsers.add(user.trim());
				}
			}
		}
		for (String user : uniqueUsers) {
			usernameDropdown.addItem(user);
		}
	}

	private void updateChartData() {
		for (MetricDef metric : metrics) {
			metric.dataset.clear();
			metric.hasData = false;
		}

		LocalDateTime startFilter = startDatePicker != null ? startDatePicker.getSelectedDateTime() : null;
		LocalDateTime endFilter = endDatePicker != null ? endDatePicker.getSelectedDateTime() : null;

		String targetUser = "";
		Component editor = usernameDropdown.getEditor().getEditorComponent();
		if (editor instanceof JTextField) {
			targetUser = ((JTextField) editor).getText().trim();
		} else if (usernameDropdown.getSelectedItem() != null) {
			targetUser = usernameDropdown.getSelectedItem().toString().trim();
		}

		boolean filterByUser = !targetUser.isEmpty() && !targetUser.equalsIgnoreCase("All Users");

		for (Map.Entry<SdkProfilingSession, List<Map<String, String>>> entry : parsedSessionData.entrySet()) {
			SdkProfilingSession session = entry.getKey();
			List<Map<String, String>> rows = entry.getValue();

			String seriesName = session.getName() != null ? session.getName() : "Unknown";
			long[] metricSums = new long[metrics.size()];
			boolean sessionHasAnyMatch = false;

			for (Map<String, String> row : rows) {
				LocalDateTime rowTs = parseTimestamp(row.get("timestamp"));
				if (rowTs != null) {
					if (startFilter != null && rowTs.isBefore(startFilter)) continue;
					if (endFilter != null && rowTs.isAfter(endFilter)) continue;
				}

				String rowUser = row.get("user_name");
				if (rowUser != null) rowUser = rowUser.trim();

				if (filterByUser && (rowUser == null || !targetUser.equalsIgnoreCase(rowUser))) {
					continue;
				}

				if (!allExecutionIds.isEmpty()) {
					String rowExecId = row.get("execution_id");
					if (rowExecId != null && !selectedExecutionIds.contains(rowExecId)) {
						continue;
					}
				}

				sessionHasAnyMatch = true;

				for (int i = 0; i < metrics.size(); i++) {
					String val = row.get(metrics.get(i).csvKey);
					if (val != null && !val.trim().isEmpty()) {
						metrics.get(i).hasData = true;
						metricSums[i] += parseLong(val);
					}
				}
			}

			if (sessionHasAnyMatch) {
				for (int i = 0; i < metrics.size(); i++) {
					// Use seriesName for both Row Key (series) and Column Key (category) 
					// to get multiple distinct bars in the bar chart for the same metric tab.
					metrics.get(i).dataset.addValue(metricSums[i], seriesName, seriesName);
				}
			}
		}

		for (MetricDef metric : metrics) {
			CategoryPlot plot = metric.chart.getCategoryPlot();
			if (!metric.hasData) {
				metric.dataset.clear();
				plot.getRangeAxis().setTickLabelsVisible(false);
				plot.getRangeAxis().setTickMarksVisible(false);
			} else {
				plot.getRangeAxis().setTickLabelsVisible(true);
				plot.getRangeAxis().setTickMarksVisible(true);
			}
		}
	}

	private long parseLong(String value) {
		if (value == null || value.isEmpty()) return 0;
		try {
			return (long) Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Override
	protected Action[] createActions() {
		return new Action[]{getCancelAction()};
	}
}