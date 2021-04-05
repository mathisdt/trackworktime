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

import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;

import com.getpebble.android.kit.PebbleKit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages vibration alarms.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class ExternalNotificationManager {

	private final Vibrator vibratorService;
	private final Context context;

	/**
	 * Create the manager.
	 */
	public ExternalNotificationManager(Context context) {
		this.context = context;
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

	public void notifyPebble(String message) {
		if (PebbleKit.isWatchConnected(context)) {
			final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

			final Map<String, String> data = new HashMap<>();
			data.put("title", "Track Work Time");
			data.put("body", message);
			final JSONObject jsonData = new JSONObject(data);
			final String notificationData = new JSONArray().put(jsonData).toString();

			i.putExtra("messageType", "PEBBLE_ALERT");
			i.putExtra("sender", "PebbleKit Android");
			i.putExtra("notificationData", notificationData);
			context.sendBroadcast(i);
		}
	}
}
