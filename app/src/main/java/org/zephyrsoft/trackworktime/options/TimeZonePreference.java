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

import org.pmw.tinylog.Logger;
import org.threeten.bp.ZoneId;
import org.zephyrsoft.trackworktime.R;

public class TimeZonePreference extends DialogPreference {

	public TimeZonePreference(Context context, AttributeSet attrs) {
		super(context, attrs, R.attr.preferenceScreenStyle);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}

	@Override
	protected void onSetInitialValue(Object defaultValue) {
		String timeZone;

		if (defaultValue == null) {
			timeZone = getPersistedString(ZoneId.systemDefault().getId());
		} else {
			timeZone = getPersistedString(defaultValue.toString());
		}

		try {
			ZoneId.of(timeZone);
		} catch (Exception e) {
			timeZone = ZoneId.systemDefault().getId();

			Logger.error("Invalid time zone was reset to system default.");
		}

		setSummary(timeZone);
	}

	void updateValue(String value) {
		if (callChangeListener(value)) {
			if (persistString(value)) {
				setSummary(value);
			}
		}
	}
}
