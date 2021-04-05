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
