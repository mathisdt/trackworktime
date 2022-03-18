/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License 3.0 as published by
 * the Free Software Foundation.
 *
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License 3.0 for more details.
 *
 * You should have received a copy of the GNU General Public License 3.0
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime.ui;

import static org.zephyrsoft.trackworktime.util.DateTimeUtil.dateToEpoch;

import android.app.DatePickerDialog;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;


public class DateTextViewController {

	private final TextView view;

	@Nullable
	private LocalDate date;

	@Nullable
	private ZonedDateTime min;

	@Nullable
	private ZonedDateTime max;

	@Nullable
	private final Consumer<LocalDate> externalListener;

	public DateTextViewController(@NonNull TextView view, @Nullable Consumer<LocalDate> externalListener) {
		this.view = view;
		this.externalListener = externalListener;
		view.setOnClickListener(v -> showDatePicker());
	}

	public DateTextViewController(@NonNull TextView view) {
		this(view, null);
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
		setDateLimits(dialog);
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
		if (externalListener != null) {
			externalListener.accept(newDate);
		}
	}

	private void setDateLimits(DatePickerDialog dialog) {
		DatePicker picker = dialog.getDatePicker();
		if (min != null) {
			picker.setMinDate(dateToEpoch(min));
		}
		if (max != null) {
			picker.setMaxDate(dateToEpoch(max));
		}
	}

	public void setDate(LocalDate date) {
		String text = DateTimeUtil.formatLocalizedDayAndDate(date, Basics.getOrCreateInstance(view.getContext()).getLocale());
		view.setText(text);
		this.date = date;
	}

	@Nullable
	public LocalDate getDate() {
		return date;
	}

	public void setDateLimits(
			@Nullable ZonedDateTime minInclusive,
			@Nullable ZonedDateTime maxInclusive
	) {
		min = minInclusive;
		max = maxInclusive;
	}

}
