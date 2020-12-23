package org.zephyrsoft.trackworktime.options;

import android.content.Context;
import android.view.View;
import android.widget.TimePicker;

import androidx.preference.PreferenceDialogFragmentCompat;

public class TimePreferenceDialogFragment extends PreferenceDialogFragmentCompat {

	private TimePicker timePicker;

	@Override
	protected View onCreateDialogView(Context context) {
		timePicker = new TimePicker(context);
		timePicker.setIs24HourView(true);
		return timePicker;
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		TimePreference pref = (TimePreference) getPreference();

		timePicker.setCurrentHour(pref.getHour());
		timePicker.setCurrentMinute(pref.getMinute());
	}

	@Override
	public void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			TimePreference pref = (TimePreference) getPreference();

			pref.updateValue(timePicker.getCurrentHour(), timePicker.getCurrentMinute());
		}
	}
}
