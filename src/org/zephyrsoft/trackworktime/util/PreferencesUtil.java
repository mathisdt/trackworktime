/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package org.zephyrsoft.trackworktime.util;

import java.util.Set;
import android.content.SharedPreferences;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.options.Checks;
import org.zephyrsoft.trackworktime.options.Key;

/**
 * Helper class for handling {@link SharedPreferences}.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class PreferencesUtil {
	
	/**
	 * Execute all checks for the options and disable any sections that contain illegal options or option combinations.
	 * 
	 * @return the number of sections that were disabled because of failed checks
	 */
	public static int checkAllPreferenceSections() {
		SharedPreferences preferences = Basics.getInstance().getPreferences();
		int disabledSections = 0;
		for (String key : preferences.getAll().keySet()) {
			Key sectionToDisable = PreferencesUtil.check(preferences, key);
			if (sectionToDisable != null && PreferencesUtil.getBooleanPreference(preferences, sectionToDisable)) {
				Logger.warn("option {0} is invalid => disabling option {1}", key, sectionToDisable.getName());
				disabledSections++;
				
				// deactivate the section
				PreferencesUtil.disablePreference(preferences, sectionToDisable);
			}
		}
		
		return disabledSections;
	}
	
	/**
	 * Set a boolean preference to false. Used primarily to disable a section.
	 */
	public static void disablePreference(SharedPreferences sharedPreferences, Key toDisable) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(toDisable.getName(), false);
		editor.commit();
	}
	
	/**
	 * Get the value of the indicated preference. Defaults to false if the preference does not exist.
	 */
	public static boolean getBooleanPreference(SharedPreferences sharedPreferences, Key key) {
		return key == null || sharedPreferences == null ? false : sharedPreferences.getBoolean(key.getName(), false);
	}
	
	/**
	 * Check a preference value in combination with the other values.
	 * 
	 * @param sharedPreferences where to get all preferences from
	 * @param keyName which one to check
	 * @return the {@link Key} belonging to the illegal section (this boolean preference should be disabled
	 *         subsequently) or {@code null} if all is OK
	 */
	public static Key check(SharedPreferences sharedPreferences, String keyName) {
		Key key = Key.getKeyWithName(keyName);
		if (key != null) {
			return check(sharedPreferences, key);
		} else {
			return null;
		}
	}
	
	/**
	 * Check a preference value in combination with the other values.
	 * 
	 * @param sharedPreferences where to get all preferences from
	 * @param key which one to check
	 * @return the {@link Key} belonging to the illegal section (this boolean preference should be disabled
	 *         subsequently) or {@code null} if all is OK
	 */
	public static Key check(SharedPreferences sharedPreferences, Key key) {
		// only two levels planned - either a pref is a parent or a child, it cannot be both!
		boolean isParent = (key.getParent() == null);
		
		Key keyToDisableIfInvalid = null;
		boolean isValid = true;
		if (isParent) {
			keyToDisableIfInvalid = key;
			Set<Key> keysToCheck = Key.getChildKeys(key);
			for (Key keyToCheck : keysToCheck) {
				isValid =
					keyToCheck.getDataType().validateFromSharedPreferences(sharedPreferences, keyToCheck.getName())
						&& Checks.executeFor(keyToCheck, sharedPreferences);
				if (!isValid) {
					break;
				}
			}
		} else {
			keyToDisableIfInvalid = key.getParent();
			isValid =
				key.getDataType().validateFromSharedPreferences(sharedPreferences, key.getName())
					&& Checks.executeFor(key, sharedPreferences);
		}
		
		return isValid ? null : keyToDisableIfInvalid;
	}
	
}
