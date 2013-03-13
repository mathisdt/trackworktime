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
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.IBinder;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.Constants;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * The background service providing the location-based tracking without having the activity open.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class LocationTrackerService extends Service {
	
	private static LocationTracker locationTracker = null;
	private int startId;
	
	private static final AtomicBoolean isRunning = new AtomicBoolean(false);
	
	private Basics basics = null;
	
	@Override
	public void onCreate() {
		Logger.info("creating LocationTrackerService");
		basics = Basics.getOrCreateInstance(getApplicationContext());
		locationTracker =
			new LocationTracker((LocationManager) getSystemService(Context.LOCATION_SERVICE), basics.getTimerManager(),
				basics.getVibrationManager(), (AudioManager) getSystemService(Context.AUDIO_SERVICE));
		// restart if service crashed previously
		Basics.getOrCreateInstance(getApplicationContext()).safeCheckLocationBasedTracking();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// do nothing here as we don't bind the service to an activity
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, @SuppressWarnings("hiding") int startId) {
		Double latitude = (Double) intent.getExtras().get(Constants.INTENT_EXTRA_LATITUDE);
		Double longitude = (Double) intent.getExtras().get(Constants.INTENT_EXTRA_LONGITUDE);
		Double toleranceInMeters = (Double) intent.getExtras().get(Constants.INTENT_EXTRA_TOLERANCE);
		Boolean vibrate = (Boolean) intent.getExtras().get(Constants.INTENT_EXTRA_VIBRATE);
		Result result = null;
		if (isRunning.compareAndSet(false, true)) {
			this.startId = startId;
			result = locationTracker.startTrackingByLocation(latitude, longitude, toleranceInMeters, vibrate);
		} else if (!latitude.equals(locationTracker.getLatitude()) || !longitude.equals(locationTracker.getLongitude())
			|| !toleranceInMeters.equals(locationTracker.getTolerance())
			|| !vibrate.equals(locationTracker.shouldVibrate())) {
			// already running, but the data has to be updated
			result = locationTracker.startTrackingByLocation(latitude, longitude, toleranceInMeters, vibrate);
		} else {
			Logger.debug("LocationTrackerService is already running and nothing has to be updated - no action");
		}
		
		if (result != null && result == Result.FAILURE_INSUFFICIENT_RIGHTS) {
			// disable the tracking and notify user of it
			basics.disableLocationBasedTracking();
			basics
				.showNotification(
					"Disabling the location-based tracking because of missing privileges!",
					"Disabled location-based tracking!",
					"(open to see details)",
					basics
						.createMessageIntent(
							"Track Work Time disabled the location-based tracking because of missing privileges. You can re-enable it in the options when the permission ACCESS_COARSE_LOCATION is granted.",
							Constants.MISSING_PRIVILEGE_ACCESS_COARSE_LOCATION_ID),
					Constants.MISSING_PRIVILEGE_ACCESS_COARSE_LOCATION_ID, false);
		}
		
		return Service.START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Logger.info("destroying LocationTrackerService");
		locationTracker.stopTrackingByLocation();
		isRunning.set(false);
		stopSelf();
	}
	
}
