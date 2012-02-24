package org.zephyrsoft.trackworktime;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * @author Mathis Dirksen-Thedens
 */
public class OptionsActivity extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.options);
	}
	
}
