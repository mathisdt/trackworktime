package org.zephyrsoft.trackworktime.options;

import android.content.Context;
import android.os.Build;
import android.preference.ListPreference;
import android.util.AttributeSet;

import androidx.annotation.RequiresApi;

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
		setDefaultValue(FlexiReset.NONE.getIntervalPreferenceValue());
	}

	private void setEntries() {
		List<String> values = new ArrayList<>();
		List<String> names = new ArrayList<>();
		for(FlexiReset flexiReset : FlexiReset.values()) {
			String name = flexiReset.getFriendlyName();
			names.add(name);
			String value = flexiReset.getIntervalPreferenceValue();
			values.add(value);
		}
		setEntryValues(values.toArray(new String[0]));
		setEntries(names.toArray(new String[0]));
	}

}
