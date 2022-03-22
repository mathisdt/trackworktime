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
package org.zephyrsoft.trackworktime.editevent;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import org.threeten.bp.LocalTime;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

public class TimeTextViewController {

	private final TextView view;
	private Consumer<LocalTime> listener;

	@Nullable
	private LocalTime time;

	public void setTime(@Nullable LocalTime time) {
		String text = DateTimeUtil.formatLocalizedTime(time, Basics.getOrCreateInstance(view.getContext()).getLocale());
		view.setText(text);
		this.time = time;

		if (listener != null) {
			listener.accept(time);
		}
	}

	@Nullable
	public LocalTime getTime() {
		return time;
	}

	public void setListener(Consumer<LocalTime> listener) {
		this.listener = listener;
	}

	public TimeTextViewController(@NonNull TextView view) {
		this.view = view;
		view.setOnClickListener(v -> showPicker());
	}

	private void showPicker() {
		createPicker().show();
	}

	private TimePickerDialog createPicker() {
		LocalTime time = getInitialTime();
		Context context = view.getContext();
		boolean is24Format = DateFormat.is24HourFormat(context);
		return new TimePickerDialog(
				context,
				this::onNewTimeSelected,
				time.getHour(),
				time.getMinute(),
				is24Format
		);
	}

	private LocalTime getInitialTime() {
		if(time == null) {
			return LocalTime.now();
		} else {
			return time;
		}
	}

	private void onNewTimeSelected(TimePicker view, int hourOfDay, int minute) {
		LocalTime time = LocalTime.of(hourOfDay, minute);
		setTime(time);
	}

}
