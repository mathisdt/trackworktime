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
import org.zephyrsoft.trackworktime.timer.TimerManager;

/**
 * Available methods of automatically tracking work time.
 */
public enum TrackingMethod {

	LOCATION(R.string.keyClockedInByLocation, TimerManager.EventOrigin.LOCATION),
	WIFI(R.string.keyClockedInByWifi, TimerManager.EventOrigin.WIFI);

	private final int preferenceKeyId;
	private final TimerManager.EventOrigin source;

	TrackingMethod(int preferenceKeyId, TimerManager.EventOrigin source) {
		this.preferenceKeyId = preferenceKeyId;
		this.source = source;
	}

	public int getPreferenceKeyId() {
		return preferenceKeyId;
	}

	public TimerManager.EventOrigin getSource() {
		return source;
	}
}
