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
	
	@Override
	protected void onPause() {
		super.onPause();
		
		// make sure that location-based tracking gets enabled/disabled
		Basics.getInstance().checkLocationBasedTracking();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		// make sure that location-based tracking gets enabled/disabled
		Basics.getInstance().checkLocationBasedTracking();
	}
	
}
