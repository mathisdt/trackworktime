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

import android.content.Context;
import android.os.Vibrator;

/**
 * Manages vibration alarms.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class VibrationManager {

	private Vibrator vibratorService;

	/**
	 * Create the manager.
	 */
	public VibrationManager(Context context) {
		vibratorService = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}

	/**
	 * Vibrate a specified pattern (once, do not repeat it).
	 * 
	 * @see Vibrator#vibrate(long[], int)
	 */
	public void vibrate(long[] pattern) {
		vibratorService.vibrate(pattern, -1);
	}
}
