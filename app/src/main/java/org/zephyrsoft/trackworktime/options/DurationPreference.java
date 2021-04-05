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
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

public class DurationPreference extends DialogPreference {
	private String duration = "0:00";

	public DurationPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}

	@Override
	protected void onSetInitialValue(Object defaultValue) {
		String value;

		if (defaultValue == null) {
			value = getPersistedString("00:00");
		} else {
			value = getPersistedString(defaultValue.toString());
		}

		if (DateTimeUtil.isDurationValid(value)) {
			duration = value;
			updateSummary();
		}
	}

	private void updateSummary() {
		setSummary(String.format(getContext().getString(R.string.current_value), duration));
	}

	void updateValue(String value) {
		if (DateTimeUtil.isDurationValid(value) && callChangeListener(value)) {
			duration = value;

			if (persistString(value)) {
				updateSummary();
			}
		}
	}

	String getDuration() {
		return duration;
	}
}
