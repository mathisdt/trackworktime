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
package org.zephyrsoft.trackworktime.location;

import org.zephyrsoft.trackworktime.R;

/**
 * Available methods of automatically tracking work time.
 * 
 * @author Mathis Dirksen-Thedens
 */
public enum TrackingMethod {

	LOCATION(R.string.keyClockedInByLocation),
	WIFI(R.string.keyClockedInByWifi);

	private final int preferenceKeyId;

	TrackingMethod(int preferenceKeyId) {
		this.preferenceKeyId = preferenceKeyId;
	}

	public int getPreferenceKeyId() {
		return preferenceKeyId;
	}

}
