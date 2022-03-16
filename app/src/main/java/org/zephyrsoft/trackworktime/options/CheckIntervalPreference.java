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

public class CheckIntervalPreference extends DialogPreference {
	private Integer number = 1;

	public CheckIntervalPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}

	@Override
	protected void onSetInitialValue(Object defaultValue) {
		int value;

		if (defaultValue == null) {
			value = Integer.parseInt(getPersistedString("1"));
		} else {
			value = Integer.parseInt(getPersistedString(defaultValue.toString()));
		}

		if (isValid(value)) {
			number = value;
			updateSummary();
		}
	}

	private void updateSummary() {
		setSummary(String.format(getContext().getString(R.string.current_value_minutes), number.toString()));
	}

	static boolean isValid(String str) {
		try {
			int i = Integer.parseInt(str);
			return isValid(i);
		} catch (NumberFormatException nfe) {
			return false;
		}
	}

	static boolean isValid(int i) {
		return i >= 1 && i <= 60;
	}

	void updateValue(String value) {
		if (isValid(value) && callChangeListener(value)) {
			number = Integer.valueOf(value);

			if (persistString(number.toString())) {
				updateSummary();
			}
		}
	}

	Integer getNumber() {
		return number;
	}
}
