package org.zephyrsoft.trackworktime.editevent;

import android.app.TimePickerDialog;
import android.text.format.DateFormat;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.threeten.bp.LocalTime;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

public class TimeTextViewController {

	private final TextView view;

	@Nullable
	private LocalTime time;

	public void setTime(@Nullable LocalTime time) {
		String text = DateTimeUtil.formatLocalizedTime(time);
		view.setText(text);
		this.time = time;
	}

	@Nullable
	public LocalTime getTime() {
		return time;
	}

	public TimeTextViewController(@NonNull TextView view) {
		this.view = view;
		view.setOnClickListener(v -> showPicker());
	}

	private void showPicker() {
		createPicker().show();
	}

	private TimePickerDialog createPicker() {
		var time = getInitialTime();
		var context = view.getContext();
		var is24Format = DateFormat.is24HourFormat(context);
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
		var time = LocalTime.of(hourOfDay, minute);
		setTime(time);
	}

}
