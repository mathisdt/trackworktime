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
package org.zephyrsoft.trackworktime.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.options.Checks;
import org.zephyrsoft.trackworktime.options.Key;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for handling {@link SharedPreferences}.
 */
public class PreferencesUtil {

	private static final String PREF_FIELD_SEPARATOR = ";";
	private static final String PREF_TYPE_STRING = "string";
	private static final String PREF_TYPE_STRINGSET = "stringset";
	private static final String PREF_TYPE_BOOLEAN = "boolean";
	private static final String PREF_TYPE_INTEGER = "integer";
	private static final String PREF_TYPE_FLOAT = "float";
	private static final String PREF_TYPE_LONG = "long";
	private static final String NEWLINE = "\n";

	/**
	 * Execute all checks for the options and disable any sections that contain illegal options or option combinations.
	 *
	 * @return the number of sections that were disabled because of failed checks
	 */
	public static int checkAllPreferenceSections(Context context) {
		SharedPreferences preferences = Basics.get(context).getPreferences();
		int disabledSections = 0;
		for (String key : preferences.getAll().keySet()) {
			Key sectionToDisable = PreferencesUtil.check(preferences, key);
			if (PreferencesUtil.getBooleanPreference(preferences, sectionToDisable)) {
				Logger.warn("option {} is invalid => disabling option {}", key, sectionToDisable.getName());
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
	@SuppressLint("ApplySharedPref")
	public static void disablePreference(SharedPreferences sharedPreferences, Key toDisable) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(toDisable.getName(), false);
		editor.commit();
	}

	/**
	 * Get the value of the indicated preference. Defaults to false if the preference does not exist.
	 */
	public static boolean getBooleanPreference(SharedPreferences sharedPreferences, Key key) {
		return key != null && sharedPreferences != null && sharedPreferences.getBoolean(key.getName(), false);
	}

	/**
	 * Check a preference value in combination with the other values.
	 *
	 * @param sharedPreferences
	 *            where to get all preferences from
	 * @param keyName
	 *            which one to check
	 * @return the {@link Key} belonging to the illegal section (this boolean preference should be disabled
	 *         subsequently) or {@code null} if all is OK";"
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
	 * @param sharedPreferences
	 *            where to get all preferences from
	 * @param key
	 *            which one to check
	 * @return the {@link Key} belonging to the illegal section (this boolean preference should be disabled
	 *         subsequently) or {@code null} if all is OK
	 */
	public static Key check(SharedPreferences sharedPreferences, Key key) {
		// only two levels planned - either a pref is a parent or a child, it cannot be both!
		boolean isParent = (key.getParent() == null);

		Key keyToDisableIfInvalid;
		boolean isValid = true;
		if (isParent) {
			keyToDisableIfInvalid = key;
			Set<Key> keysToCheck = Key.getChildKeys(key);
			for (Key keyToCheck : keysToCheck) {
				isValid = keyToCheck.getDataType().validateFromSharedPreferences(sharedPreferences,
					keyToCheck.getName())
					&& Checks.executeFor(keyToCheck, sharedPreferences);
				if (!isValid) {
					break;
				}
			}
		} else {
			keyToDisableIfInvalid = key.getParent();
			isValid = key.getDataType().validateFromSharedPreferences(sharedPreferences, key.getName())
				&& Checks.executeFor(key, sharedPreferences);
		}

		return isValid ? null : keyToDisableIfInvalid;
	}

	/**
	 * writes the current preferences including their values to the given writer
	 * @see #readPreferences(SharedPreferences, BufferedReader)
	 */
	@SuppressWarnings({"unchecked"})
	public static void writePreferences(SharedPreferences preferences, BufferedWriter output) throws IOException {
		for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
			if (entry.getValue() instanceof String) {
				output.append(entry.getKey()).append(PREF_FIELD_SEPARATOR)
					.append(PREF_TYPE_STRING).append(PREF_FIELD_SEPARATOR)
					.append((String)entry.getValue()).append(NEWLINE);
			} else if (entry.getValue() instanceof Set) {
				Set<String> value = (Set<String>) entry.getValue();
				String separator = findSeparator(value);
				output.append(entry.getKey()).append(PREF_FIELD_SEPARATOR)
					.append(PREF_TYPE_STRINGSET).append(PREF_FIELD_SEPARATOR)
					.append(separator).append(PREF_FIELD_SEPARATOR);
				boolean isFirst = true;
				for (String v : value) {
					if (isFirst) {
						isFirst = false;
					} else {
						output.append(separator);
					}
					output.append(v);
				}
				output.append(NEWLINE);
			} else if (entry.getValue() instanceof Boolean) {
				output.append(entry.getKey()).append(PREF_FIELD_SEPARATOR)
					.append(PREF_TYPE_BOOLEAN).append(PREF_FIELD_SEPARATOR)
					.append(((Boolean)entry.getValue()).toString()).append(NEWLINE);
			} else if (entry.getValue() instanceof Integer) {
				output.append(entry.getKey()).append(PREF_FIELD_SEPARATOR)
					.append(PREF_TYPE_INTEGER).append(PREF_FIELD_SEPARATOR)
					.append(((Integer)entry.getValue()).toString()).append(NEWLINE);
			} else if (entry.getValue() instanceof Float) {
				output.append(entry.getKey()).append(PREF_FIELD_SEPARATOR)
					.append(PREF_TYPE_FLOAT).append(PREF_FIELD_SEPARATOR)
					.append(((Float)entry.getValue()).toString()).append(NEWLINE);
			} else if (entry.getValue() instanceof Long) {
				output.append(entry.getKey()).append(PREF_FIELD_SEPARATOR)
					.append(PREF_TYPE_LONG).append(PREF_FIELD_SEPARATOR)
					.append(((Long)entry.getValue()).toString()).append(NEWLINE);
			} else {
				throw new IllegalStateException("unknown preference type: " + entry.getValue());
			}
		}
	}

	private static String findSeparator(Set<String> set) {
		for (String possibleSeparator : new String[] {",", "|", "!", "$", "%", "#", "~", "+", ":",
			"!§&%$", "&§%$}!,.-#+"}) {
			if (!anyContains(set, possibleSeparator)) {
				return possibleSeparator;
			}
		}
		throw new IllegalStateException("could not find a separator");
	}

	private static boolean anyContains(Set<String> set, String testedSeparator) {
		for (String str : set) {
			if (str.contains(testedSeparator)) {
				return true;
			}
		}
		return false;
	}

    /**
     * reads the keys and values from the given reader and saves them as preferences
     * @see #writePreferences(SharedPreferences, BufferedWriter)
     */
	@SuppressLint("ApplySharedPref")
	public static void readPreferences(SharedPreferences preferences, BufferedReader input) throws IOException {
	    String line;
		SharedPreferences.Editor editor = preferences.edit();
		// remove all entries
		editor.clear();
		// now add the entries from the reader - there is no try/catch
		// around the put method calls intentionally so either all or none are changed
		while ((line = input.readLine()) != null) {
			String[] parts = line.split(PREF_FIELD_SEPARATOR, 3);
			switch (parts[1]) {
				case PREF_TYPE_STRING:
					editor.putString(parts[0], parts[2]);
					break;
				case PREF_TYPE_STRINGSET:
					String[] separatorAndValues = parts[2].split(PREF_FIELD_SEPARATOR, 2);
					String[] values = separatorAndValues[1].split(separatorAndValues[0]);
					Set<String> valueSet = new HashSet<>(Arrays.asList(values));
					editor.putStringSet(parts[0], valueSet);
					break;
				case PREF_TYPE_BOOLEAN:
					editor.putBoolean(parts[0], Boolean.parseBoolean(parts[2]));
					break;
				case PREF_TYPE_INTEGER:
					editor.putInt(parts[0], Integer.parseInt(parts[2]));
					break;
				case PREF_TYPE_FLOAT:
					editor.putFloat(parts[0], Float.parseFloat(parts[2]));
					break;
				case PREF_TYPE_LONG:
					editor.putLong(parts[0], Long.parseLong(parts[2]));
					break;
				default:
					throw new IllegalStateException("unknown preference type " + parts[1]
						+ " for preference " + parts[0]);
			}
        }
		// if no exception until here, make changes permanent
		editor.commit();
	}
}
