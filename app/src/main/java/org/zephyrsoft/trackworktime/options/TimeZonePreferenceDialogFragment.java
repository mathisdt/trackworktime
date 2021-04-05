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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.android.timezonepicker.TimeZoneInfo;
import com.android.timezonepicker.TimeZonePickerView;

import org.pmw.tinylog.Logger;

public class TimeZonePreferenceDialogFragment extends PreferenceDialogFragmentCompat
		implements DialogPreference.TargetFragment, TimeZonePickerView.OnTimeZoneSetListener {

	public static final String BUNDLE_START_TIME_MILLIS = "bundle_event_start_time";
	public static final String BUNDLE_TIME_ZONE = "bundle_event_time_zone";

	private static final String KEY_HAS_RESULTS = "has_results";
	private static final String KEY_LAST_FILTER_STRING = "last_filter_string";
	private static final String KEY_LAST_FILTER_TYPE = "last_filter_type";
	private static final String KEY_LAST_FILTER_TIME = "last_filter_time";

	private TimeZonePickerView timeZonePickerView = null;

	@Override
	protected View onCreateDialogView(Context context) {
		Bundle b = getArguments();

		long timeMillis = 0;
		String timeZone = null;

		if (b != null) {
			timeMillis = b.getLong(BUNDLE_START_TIME_MILLIS);
			timeZone = b.getString(BUNDLE_TIME_ZONE);
		}

		timeZonePickerView = new TimeZonePickerView(context, null, timeZone, timeMillis, this, true);
		return timeZonePickerView;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		//dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		//dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		return dialog;
	}

	@Override
	public void onDialogClosed(boolean positiveResult) {
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean(KEY_HAS_RESULTS, timeZonePickerView != null && timeZonePickerView.hasResults());
		if (timeZonePickerView != null) {
			outState.putInt(KEY_LAST_FILTER_TYPE, timeZonePickerView.getLastFilterType());
			outState.putString(KEY_LAST_FILTER_STRING, timeZonePickerView.getLastFilterString());
			outState.putInt(KEY_LAST_FILTER_TIME, timeZonePickerView.getLastFilterTime());
		}
	}

	@Override
	public Preference findPreference(CharSequence key) {
		return getPreference();
	}

	@Override
	public void onTimeZoneSet(TimeZoneInfo tzi) {
		Logger.debug("onTimeZoneSet");

		TimeZonePreference pref = (TimeZonePreference) getPreference();
		pref.updateValue(tzi.mTzId);
		dismiss();
	}
}
