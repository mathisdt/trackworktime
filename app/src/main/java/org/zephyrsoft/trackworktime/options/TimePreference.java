/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime.options;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import org.threeten.bp.LocalTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

public class TimePreference extends DialogPreference {
	private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ofPattern("HH:mm");

	private LocalTime time = LocalTime.MIN;

	public TimePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}

	@Override
	protected void onSetInitialValue(Object defaultValue) {
		String timeString = getPersistedString(null);

		if (timeString == null && defaultValue != null) {
			timeString = defaultValue.toString();
		}

		if (timeString != null) {
			time = LocalTime.parse(DateTimeUtil.refineTime(timeString));
			updateSummary();
		}
	}

	private void updateSummary() {
		setSummary(String.format(getContext().getString(R.string.current_value), time.format(LOCAL_TIME)));
	}

	void updateValue(int hour, int minute) {
		LocalTime newTime = LocalTime.of(hour,minute);
		String value = newTime.toString();

		if (callChangeListener(value)) {
			time = newTime;

			if (persistString(value)) {
				updateSummary();
			}
		}
	}

	int getHour() {
		return time.getHour();
	}

	int getMinute() {
		return time.getMinute();
	}
}
