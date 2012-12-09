/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.IBinder;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.MessageActivity;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * The background service providing the location-based tracking without having the activity open.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class LocationTrackerService extends Service {
	
	/** the key for the {@link Double} which determines the latitude */
	public static String INTENT_EXTRA_LATITUDE = "LATITUDE";
	
	/** the key for the {@link Double} which determines the longitude */
	public static String INTENT_EXTRA_LONGITUDE = "LONGITUDE";
	
	/** the key for the {@link Double} which determines the tolerance */
	public static String INTENT_EXTRA_TOLERANCE = "TOLERANCE";
	
	/** the key for the {@link Boolean} which determines if vibration should be used */
	public static String INTENT_EXTRA_VIBRATE = "VIBRATE";
	
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
				basics.getVibrationManager());
		// restart if service crashed previously
		Basics.getOrCreateInstance(getApplicationContext()).checkLocationBasedTracking();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// do nothing here as we don't bind the service to an activity
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, @SuppressWarnings("hiding") int startId) {
		Double latitude = (Double) intent.getExtras().get(INTENT_EXTRA_LATITUDE);
		Double longitude = (Double) intent.getExtras().get(INTENT_EXTRA_LONGITUDE);
		Double toleranceInMeters = (Double) intent.getExtras().get(INTENT_EXTRA_TOLERANCE);
		Boolean vibrate = (Boolean) intent.getExtras().get(INTENT_EXTRA_VIBRATE);
		Result result = null;
		if (isRunning.compareAndSet(false, true)) {
			this.startId = startId;
			result = locationTracker.startTrackingByLocation(latitude, longitude, toleranceInMeters, vibrate);
		} else {
			// already running, but perhaps the target location has to be updated?
			if (!latitude.equals(locationTracker.getLatitude()) || !longitude.equals(locationTracker.getLongitude())
				|| !toleranceInMeters.equals(locationTracker.getTolerance())) {
				result = locationTracker.startTrackingByLocation(latitude, longitude, toleranceInMeters, vibrate);
			}
		}
		
		if (result != null && result == Result.FAILURE_INSUFFICIENT_RIGHTS) {
			// disable the tracking and notify user of it
			basics.disableLocationBasedTracking();
			NotificationManager notificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Notification notification =
				new Notification(R.drawable.ic_launcher,
					"Disabling the location-based tracking because of missing privileges!", System.currentTimeMillis());
			Intent messageIntent = new Intent(this, MessageActivity.class);
			messageIntent
				.putExtra(
					MessageActivity.MESSAGE_EXTRA_KEY,
					"Track Work Time disabled the location-based tracking because of missing privileges. You can re-enable it in the options when the permission ACCESS_COARSE_LOCATION is granted.");
			messageIntent.putExtra(MessageActivity.ID_EXTRA_KEY,
				MessageActivity.MISSING_PRIVILEGE_ACCESS_COARSE_LOCATION_ID);
			notification.setLatestEventInfo(getApplicationContext(), "Disabled location-based tracking!",
				"(open to see details)", PendingIntent.getActivity(getApplicationContext(), 0, messageIntent, flags));
			notificationManager.notify(MessageActivity.MISSING_PRIVILEGE_ACCESS_COARSE_LOCATION_ID, notification);
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
