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
import android.content.SharedPreferences;
import android.os.Vibrator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.options.Key;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages vibration alarms.
 */
public class ExternalNotificationManager {

	private final Context context;
	private final SharedPreferences preferences;
	private final Vibrator vibratorService;

	/**
	 * Create the manager.
	 */
	public ExternalNotificationManager(Context context, SharedPreferences preferences) {
		this.context = context;
		this.preferences = preferences;
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
		try {
			if (preferences.getBoolean(Key.NOTIFICATION_ON_PEBBLE.getName(), false)) {
				final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

				final Map<String, String> data = new HashMap<>();
				data.put("title", context.getString(R.string.app_name));
				data.put("body", message);
				final JSONObject jsonData = new JSONObject(data);
				final String notificationData = new JSONArray().put(jsonData).toString();

				i.putExtra("messageType", "PEBBLE_ALERT");
				i.putExtra("sender", "PebbleKit Android");
				i.putExtra("notificationData", notificationData);
				context.sendBroadcast(i);
			}
		} catch (Exception e) {
			Logger.warn(e, "problem while notifying via Pebble");
		}
	}
}
