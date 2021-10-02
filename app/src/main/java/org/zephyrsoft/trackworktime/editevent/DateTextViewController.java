package org.zephyrsoft.trackworktime.editevent;

import android.app.DatePickerDialog;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.threeten.bp.LocalDate;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

class DateTextViewController {

	private final TextView view;

	@Nullable
	private LocalDate date;

	DateTextViewController(@NonNull TextView view) {
		this.view = view;
		view.setOnClickListener(v -> showDatePicker());
	}

	private void showDatePicker() {
		LocalDate date = getInitialPickerDate();
		DatePickerDialog dialog = new DatePickerDialog(
				view.getContext(),
				this::onNewDateSelected,
				date.getYear(),
				date.getMonthValue() - 1,
				date.getDayOfMonth()
		);
		dialog.show();
	}

	private LocalDate getInitialPickerDate() {
		if (date == null) {
			return LocalDate.now();
	 	} else {
			return date;
		}
	}

	private void onNewDateSelected(DatePicker picker, int year, int month, int day) {
		LocalDate newDate = LocalDate.of(year, month + 1, day);
		setDate(newDate);
	}

	public void setDate(LocalDate date) {
		String text = DateTimeUtil.formatLocalizedDateShort(date);
		view.setText(text);
		this.date = date;
	}

	@Nullable
	public LocalDate getDate() {
		return date;
	}

}
