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
package org.zephyrsoft.trackworktime;

import java.util.Calendar;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.location.LocationTrackerService;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.Logger;
import org.zephyrsoft.trackworktime.util.VibrationManager;

/**
 * Creates the database connection on device boot and starts the location-based tracking service (if location-based
 * tracking is enabled). Also schedules periodic intents for {@link ServiceWatchdogReceiver} which in turn checks if
 * {@link LocationTrackerService} needs to be (re-)started.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class Basics extends BroadcastReceiver {
	
	// check once every minute
	private static final long REPEAT_TIME = 1000 * 60;
	
	private Context context = null;
	private SharedPreferences preferences = null;
	private DAO dao = null;
	private TimerManager timerManager = null;
	private VibrationManager vibrationManager = null;
	
	private static Basics instance = null;
	
	/**
	 * Creates an instance of this class.
	 */
	public Basics() {
		if (instance == null) {
			instance = this;
		}
	}
	
	/**
	 * Fetches the singleton.
	 */
	public static Basics getInstance() {
		return instance;
	}
	
	/**
	 * Creates the singleton if not already created.
	 */
	public static Basics getOrCreateInstance(Context androidContext) {
		if (instance == null) {
			instance = new Basics();
			instance.receivedIntent(androidContext);
		}
		return instance;
	}
	
	@Override
	public void onReceive(Context androidContext, Intent intent) {
		// always only use the one instance
		instance.receivedIntent(androidContext);
		
	}
	
	/**
	 * Forwarding from {@link #onReceive(Context, Intent)}, but this method is only called on the singleton instance -
	 * no matter how many instances Android might choose to create of this class.
	 */
	public void receivedIntent(Context androidContext) {
		context = androidContext;
		init();
		schedulePeriodicIntents();
	}
	
	private void init() {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		dao = new DAO(context);
		timerManager = new TimerManager(dao, preferences);
		vibrationManager = new VibrationManager(context);
	}
	
	private void schedulePeriodicIntents() {
		AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intentToSchedule = new Intent(context, ServiceWatchdogReceiver.class);
		PendingIntent pendingIntent =
			PendingIntent.getBroadcast(context, 0, intentToSchedule, PendingIntent.FLAG_CANCEL_CURRENT);
		
		Calendar cal = Calendar.getInstance();
		// start one minute after boot completed
		cal.add(Calendar.MINUTE, 1);
		
		// schedule once every minute
		service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), REPEAT_TIME, pendingIntent);
	}
	
	/**
	 * Check if location-based tracking has to be en- or disabled.
	 */
	public void checkLocationBasedTracking() {
		Logger.debug("checking location-based tracking");
		if (preferences.getBoolean(Key.LOCATION_BASED_TRACKING_ENABLED.getName(), false)) {
			Logger.debug("checking location-based tracking ENABLED");
			String latitudeString = preferences.getString(Key.LOCATION_BASED_TRACKING_LATITUDE.getName(), "0");
			String longitudeString = preferences.getString(Key.LOCATION_BASED_TRACKING_LONGITUDE.getName(), "0");
			String toleranceString = preferences.getString(Key.LOCATION_BASED_TRACKING_TOLERANCE.getName(), "0");
			double latitude = 0.0;
			try {
				latitude = Double.parseDouble(latitudeString);
			} catch (NumberFormatException nfe) {
				Logger.warn("could not parse latitude: {0}", latitudeString);
			}
			double longitude = 0.0;
			try {
				longitude = Double.parseDouble(longitudeString);
			} catch (NumberFormatException nfe) {
				Logger.warn("could not parse longitude: {0}", longitudeString);
			}
			double tolerance = 0.0;
			try {
				tolerance = Double.parseDouble(toleranceString);
			} catch (NumberFormatException nfe) {
				Logger.warn("could not parse tolerance: {0}", toleranceString);
			}
			Boolean vibrate = preferences.getBoolean(Key.LOCATION_BASED_TRACKING_VIBRATE.getName(), Boolean.FALSE);
			Intent startIntent = buildServiceIntent(latitude, longitude, tolerance, vibrate);
			// we can start the service again even if it is already running because
			// onStartCommand(...) in LocationTrackerService won't do anything if the service
			// is already running with the current parameters - if the location or the
			// tolerance changed, then it will update the values for the service
			context.startService(startIntent);
			Logger.debug("location-based tracking service started");
		} else {
			Intent stopIntent = buildServiceIntent(null, null, null, null);
			context.stopService(stopIntent);
			Logger.debug("location-based tracking service stopped");
		}
	}
	
	/**
	 * Show a notification.
	 */
	public void showNotification(String scrollingText, String notificationTitle, String detailText) {
		NotificationManager notificationManager =
			(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.ic_launcher, scrollingText, System.currentTimeMillis());
		Intent messageIntent =
			createMessageIntent(detailText, MessageActivity.MISSING_PRIVILEGE_ACCESS_COARSE_LOCATION_ID);
		notification.setLatestEventInfo(context, notificationTitle, "(open to see details)",
			PendingIntent.getActivity(context, 0, messageIntent, PendingIntent.FLAG_CANCEL_CURRENT));
		notificationManager.notify(MessageActivity.MISSING_PRIVILEGE_ACCESS_COARSE_LOCATION_ID, notification);
	}
	
	/**
	 * Create an intent which shows a message dialog.
	 */
	public Intent createMessageIntent(String text, Integer id) {
		Intent messageIntent = new Intent(context, MessageActivity.class);
		messageIntent.putExtra(MessageActivity.MESSAGE_EXTRA_KEY, text);
		if (id != null) {
			messageIntent.putExtra(MessageActivity.ID_EXTRA_KEY, id.intValue());
		}
		return messageIntent;
	}
	
	/**
	 * Disable the tracking.
	 */
	public void disableLocationBasedTracking() {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(Key.LOCATION_BASED_TRACKING_ENABLED.getName(), false);
		editor.commit();
	}
	
	private Intent buildServiceIntent(Double latitude, Double longitude, Double tolerance, Boolean vibrate) {
		Intent intent = new Intent(context, LocationTrackerService.class);
		intent.putExtra(LocationTrackerService.INTENT_EXTRA_LATITUDE, latitude);
		intent.putExtra(LocationTrackerService.INTENT_EXTRA_LONGITUDE, longitude);
		intent.putExtra(LocationTrackerService.INTENT_EXTRA_TOLERANCE, tolerance);
		intent.putExtra(LocationTrackerService.INTENT_EXTRA_VIBRATE, vibrate);
		return intent;
	}
	
	/**
	 * The app's preferences.
	 */
	public SharedPreferences getPreferences() {
		return preferences;
	}
	
	/**
	 * The app's DAO.
	 */
	public DAO getDao() {
		return dao;
	}
	
	/**
	 * The app's timer manager.
	 */
	public TimerManager getTimerManager() {
		return timerManager;
	}
	
	/**
	 * The wrapper for Android's {@link Vibrator}.
	 */
	public VibrationManager getVibrationManager() {
		return vibrationManager;
	}
	
}
