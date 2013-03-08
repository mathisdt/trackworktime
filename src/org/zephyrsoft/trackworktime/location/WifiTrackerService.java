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
package org.zephyrsoft.trackworktime.location;

import java.util.concurrent.atomic.AtomicBoolean;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * The background service providing the wifi-based tracking without having the activity open.
 * 
 * @author Christoph Loewe
 */
public class WifiTrackerService extends Service {
	
	/** the key for the {@link String} which determines the ssid to look for */
	public static String INTENT_EXTRA_SSID = "SSID";
	
	/** the key for the {@link Boolean} which determines if vibration should be used */
	public static String INTENT_EXTRA_VIBRATE = "VIBRATE";
	
	private static WifiTracker wifiTracker = null;
	private int startId;
	
	private static final AtomicBoolean isRunning = new AtomicBoolean(false);
	
	private Basics basics = null;
	
	@Override
	public void onCreate() {
		Logger.info("creating WifiTrackerService");
		basics = Basics.getOrCreateInstance(getApplicationContext());
		wifiTracker =
			new WifiTracker((WifiManager) getSystemService(Context.WIFI_SERVICE), basics.getTimerManager(),
				basics.getVibrationManager(), (AudioManager) getSystemService(Context.AUDIO_SERVICE));
		// restart if service crashed previously
		Basics.getOrCreateInstance(getApplicationContext()).safeCheckWifiBasedTracking();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// do nothing here as we don't bind the service to an activity
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, @SuppressWarnings("hiding") int startId) {
		String ssid = (String) intent.getExtras().get(INTENT_EXTRA_SSID);
		Boolean vibrate = (Boolean) intent.getExtras().get(INTENT_EXTRA_VIBRATE);
		Result result = null;
		if (isRunning.compareAndSet(false, true)) {
			this.startId = startId;
			result = wifiTracker.startTrackingByWifi(ssid, vibrate);
		} else if (!ssid.equals(wifiTracker.getSSID()) || !vibrate.equals(wifiTracker.shouldVibrate())) {
			// already running, but the data has to be updated
			result = wifiTracker.startTrackingByWifi(ssid, vibrate);
		} else {
			Logger.debug("WifiTrackerService is already running and nothing has to be updated - no action");
		}
		
		if (result != null && result == Result.FAILURE_INSUFFICIENT_RIGHTS) {
			// disable the tracking and notify user of it
			basics.disableWifiBasedTracking();
			basics
				.showNotification(
					"Disabling the wifi-based tracking because of missing privileges!",
					"Disabled wifi-based tracking!",
					"(open to see details)",
					basics
						.createMessageIntent(
							"Track Work Time disabled the wifi-based tracking because of missing privileges. You can re-enable it in the options when the permission ACCESS_COARSE_LOCATION is granted.",
							Basics.MISSING_PRIVILEGE_ACCESS_WIFI_STATE_ID),
					Basics.MISSING_PRIVILEGE_ACCESS_WIFI_STATE_ID, false);
		}
		
		if (isRunning.get()) {
			wifiTracker.checkWifi();
		}
		
		return Service.START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Logger.info("destroying WifiTrackerService");
		wifiTracker.stopTrackingByWifi();
		isRunning.set(false);
		stopSelf();
	}
	
}
