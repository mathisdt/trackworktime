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

import android.content.SharedPreferences;

/**
 * A check tests if a logical constraint holds. This constraint can involve multiple preferences.
 * 
 * @author Mathis Dirksen-Thedens
 */
public abstract class Check {

	private final String description;

	/**
	 * Constructor
	 */
	public Check(String description) {
		this.description = description;
	}

	/**
	 * Gets the description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Does this check use the value of the specified preference?
	 */
	public abstract boolean usesPreference(Key key);

	/**
	 * Checks the constraint coded in this check.
	 * 
	 * @return {@code true} if the constraint holds
	 */
	public abstract boolean check(SharedPreferences prefs);

}
