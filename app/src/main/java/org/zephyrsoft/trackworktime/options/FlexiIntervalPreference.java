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
import android.os.Build;
import android.util.AttributeSet;

import androidx.annotation.RequiresApi;
import androidx.preference.ListPreference;

import org.zephyrsoft.trackworktime.model.FlexiReset;

import java.util.ArrayList;
import java.util.List;

public class FlexiIntervalPreference extends ListPreference {

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public FlexiIntervalPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		initialize();
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public FlexiIntervalPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initialize();
	}

	public FlexiIntervalPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public FlexiIntervalPreference(Context context) {
		super(context);
		initialize();
	}

	private void initialize() {
		setDefaultSelection();
		setEntries();
	}

	private void setDefaultSelection() {
		setDefaultValue(FlexiReset.NONE.name());
	}

	private void setEntries() {
		List<String> values = new ArrayList<>();
		List<String> names = new ArrayList<>();
		for(FlexiReset flexiReset : FlexiReset.values()) {
			String name = flexiReset.getFriendlyName();
			names.add(name);
			String value = flexiReset.name();
			values.add(value);
		}
		setEntryValues(values.toArray(new String[0]));
		setEntries(names.toArray(new String[0]));
	}

}
