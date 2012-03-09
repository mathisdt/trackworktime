package org.zephyrsoft.trackworktime;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Activity to set the preferences of the application.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class OptionsActivity extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.options);
	}
	
}
