/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package org.zephyrsoft.trackworktime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.util.Logger;
import org.zephyrsoft.trackworktime.util.PreferencesUtil;

/**
 * Activity to set the preferences of the application.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class OptionsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.options);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		
		// make sure that location-based tracking gets enabled/disabled
		Basics.getOrCreateInstance(getApplicationContext()).checkLocationBasedTracking();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyName) {
		Key sectionToDisable = PreferencesUtil.check(sharedPreferences, keyName);
		if (sectionToDisable != null && PreferencesUtil.getBooleanPreference(sharedPreferences, sectionToDisable)) {
			Logger.warn("option {0} is invalid => disabling option {1}", keyName, sectionToDisable.getName());
			
			// show message to user
			Intent messageIntent =
				Basics
					.getInstance()
					.createMessageIntent(
						"The option \""
							+ getString(sectionToDisable.getReadableNameResourceId())
							+ "\" was disabled due to invalid settings.\n\nYou can re-enable it after you have checked the values you entered in that section.",
						null);
			startActivity(messageIntent);
			
			// deactivate the section
			PreferencesUtil.disablePreference(sharedPreferences, sectionToDisable);
			// reload data in options view
			setPreferenceScreen(null);
			addPreferencesFromResource(R.xml.options);
		}
	}
}
