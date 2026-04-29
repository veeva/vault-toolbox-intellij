package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DateTimePickerControl extends JBPanel<DateTimePickerControl> {

	private final JTextField dateField;
	private final JBLabel timeLabel;
	private final ComboBox<String> timeComboBox;
	private JBPopup currentPopup;
	private LocalDate selectedDate;

	private final LocalDateTime minDateTime;
	private final LocalDateTime maxDateTime;

	public DateTimePickerControl(LocalDateTime minDateTime, LocalDateTime maxDateTime) {
		super(new GridBagLayout());
		this.minDateTime = minDateTime;
		this.maxDateTime = maxDateTime;

		// Initialize selectedDate to today, or clamp it to the min/max bounds if today is invalid
		LocalDate today = LocalDate.now();
		if (minDateTime != null && today.isBefore(minDateTime.toLocalDate())) {
			this.selectedDate = minDateTime.toLocalDate();
		} else if (maxDateTime != null && today.isAfter(maxDateTime.toLocalDate())) {
			this.selectedDate = maxDateTime.toLocalDate();
		} else {
			this.selectedDate = today;
		}

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = JBUI.insets(0, 0, 0, 2);

		dateField = new JTextField(10);
		dateField.setEditable(false);
		updateDateField();

		JButton calendarButton = new JButton("📅");
		calendarButton.setToolTipText("Select Date");
		calendarButton.addActionListener(e -> showCalendarPopup(calendarButton));

		timeLabel = new JBLabel("Time: ");
		timeComboBox = new ComboBox<>();
		updateTimeDropdown();

		gbc.gridx = 0;
		add(dateField, gbc);
		gbc.gridx = 1;
		add(calendarButton, gbc);
		gbc.gridx = 2;
		gbc.insets = JBUI.insets(0, 5, 0, 2);
		add(timeLabel, gbc);
		gbc.gridx = 3;
		gbc.insets = JBUI.insets(0, 0, 0, 0);
		add(timeComboBox, gbc);
	}

	private void updateDateField() {
		dateField.setText(selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
	}

	private void updateTimeDropdown() {
		String previouslySelected = (String) timeComboBox.getSelectedItem();
		timeComboBox.removeAllItems();

		for (int hour = 0; hour < 24; hour++) {
			for (int minute : new int[]{0, 30}) {
				LocalTime time = LocalTime.of(hour, minute);
				LocalDateTime currentDateTime = LocalDateTime.of(selectedDate, time);

				boolean isTooEarly = minDateTime != null && currentDateTime.isBefore(minDateTime);
				boolean isTooLate = maxDateTime != null && currentDateTime.isAfter(maxDateTime);

				if (!isTooEarly && !isTooLate) {
					timeComboBox.addItem(String.format("%02d:%02d", hour, minute));
				}
			}
		}

		if (previouslySelected != null) {
			for (int i = 0; i < timeComboBox.getItemCount(); i++) {
				if (timeComboBox.getItemAt(i).equals(previouslySelected)) {
					timeComboBox.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	/**
	 * Forcibly sets the UI to a specific date and time chunk.
	 */
	public void setDateTime(LocalDateTime dateTime) {
		if (dateTime == null) return;
		this.selectedDate = dateTime.toLocalDate();
		updateDateField();
		updateTimeDropdown();

		int minute = dateTime.getMinute() >= 30 ? 30 : 0;
		timeComboBox.setSelectedItem(String.format("%02d:%02d", dateTime.getHour(), minute));
	}

	/** Toggles the visibility of the time selection components */
	public void setTimeVisible(boolean visible) {
		timeLabel.setVisible(visible);
		timeComboBox.setVisible(visible);
	}

	public void addChangeListener(Runnable listener) {
		dateField.getDocument().addDocumentListener(new DocumentAdapter() {
			@Override
			protected void textChanged(DocumentEvent e) { listener.run(); }
		});
		timeComboBox.addActionListener(e -> listener.run());
	}

	public String getSelectedDateString() {
		return dateField.getText();
	}

	public String getSelectedTimeString() {
		return (String) timeComboBox.getSelectedItem();
	}

	/** Returns just the selected date, ignoring the time dropdown entirely */
	public LocalDate getSelectedDate() {
		return this.selectedDate;
	}

	public LocalDateTime getSelectedDateTime() {
		if (timeComboBox.getSelectedItem() == null) {
			return LocalDateTime.of(selectedDate, LocalTime.MIDNIGHT);
		}
		LocalTime time = LocalTime.parse((String) timeComboBox.getSelectedItem());
		return LocalDateTime.of(selectedDate, time);
	}

	private void showCalendarPopup(Component owner) {
		if (currentPopup != null && !currentPopup.isDisposed()) currentPopup.cancel();

		CalendarView calendarView = new CalendarView(selectedDate, minDateTime, maxDateTime, newDate -> {
			this.selectedDate = newDate;
			updateDateField();
			updateTimeDropdown();
			if (currentPopup != null) currentPopup.cancel();
		});

		currentPopup = JBPopupFactory.getInstance()
				.createComponentPopupBuilder(calendarView, null)
				.setRequestFocus(true)
				.setCancelOnClickOutside(true)
				.createPopup();

		currentPopup.showUnderneathOf(owner);
	}

	private static class CalendarView extends JBPanel<CalendarView> {
		private YearMonth currentDisplayedMonth;
		private final JPanel gridPanel;
		private final JLabel monthLabel;

		public CalendarView(LocalDate initialDate, LocalDateTime minDateTime, LocalDateTime maxDateTime, java.util.function.Consumer<LocalDate> onDateSelected) {
			super(new BorderLayout());
			this.currentDisplayedMonth = YearMonth.from(initialDate);
			this.setBorder(JBUI.Borders.empty(10));

			JPanel headerPanel = new JPanel(new BorderLayout());
			JButton prevButton = new JButton("<");
			JButton nextButton = new JButton(">");
			monthLabel = new JBLabel("", SwingConstants.CENTER);
			monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD));

			prevButton.addActionListener(e -> {
				currentDisplayedMonth = currentDisplayedMonth.minusMonths(1);
				updateCalendar(minDateTime, maxDateTime, onDateSelected);
			});
			nextButton.addActionListener(e -> {
				currentDisplayedMonth = currentDisplayedMonth.plusMonths(1);
				updateCalendar(minDateTime, maxDateTime, onDateSelected);
			});

			headerPanel.add(prevButton, BorderLayout.WEST);
			headerPanel.add(monthLabel, BorderLayout.CENTER);
			headerPanel.add(nextButton, BorderLayout.EAST);

			gridPanel = new JPanel(new GridLayout(0, 7, JBUI.scale(2), JBUI.scale(2)));

			add(headerPanel, BorderLayout.NORTH);
			add(gridPanel, BorderLayout.CENTER);

			updateCalendar(minDateTime, maxDateTime, onDateSelected);
		}

		private void updateCalendar(LocalDateTime minDateTime, LocalDateTime maxDateTime, java.util.function.Consumer<LocalDate> onDateSelected) {
			gridPanel.removeAll();
			monthLabel.setText(currentDisplayedMonth.getMonth().name() + " " + currentDisplayedMonth.getYear());

			String[] days = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
			for (String day : days) {
				JBLabel label = new JBLabel(day, SwingConstants.CENTER);
				label.setForeground(JBColor.GRAY);
				gridPanel.add(label);
			}

			LocalDate firstOfMonth = currentDisplayedMonth.atDay(1);
			int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
			for (int i = 0; i < dayOfWeek; i++) gridPanel.add(new JLabel(""));

			int daysInMonth = currentDisplayedMonth.lengthOfMonth();
			for (int day = 1; day <= daysInMonth; day++) {
				LocalDate dateForButton = currentDisplayedMonth.atDay(day);
				JButton dayBtn = new JButton(String.valueOf(day));
				dayBtn.setMargin(JBUI.insets(2));

				if (dateForButton.equals(LocalDate.now())) dayBtn.setForeground(JBColor.BLUE);

				boolean isTooEarly = minDateTime != null && dateForButton.isBefore(minDateTime.toLocalDate());
				boolean isTooLate = maxDateTime != null && dateForButton.isAfter(maxDateTime.toLocalDate());

				if (isTooEarly || isTooLate) {
					dayBtn.setEnabled(false);
				} else {
					dayBtn.addActionListener(e -> onDateSelected.accept(dateForButton));
				}
				gridPanel.add(dayBtn);
			}
			revalidate();
			repaint();
		}
	}
}