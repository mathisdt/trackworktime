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
package org.zephyrsoft.trackworktime;

import hirondelle.date4j.DateTime;
import java.util.Calendar;
import org.acra.ACRA;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.location.CoordinateUtil;
import org.zephyrsoft.trackworktime.location.LocationCallback;
import org.zephyrsoft.trackworktime.location.LocationTrackerService;
import org.zephyrsoft.trackworktime.location.WifiTrackerService;
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.Logger;
import org.zephyrsoft.trackworktime.util.PreferencesUtil;
import org.zephyrsoft.trackworktime.util.VibrationManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;

/**
 * Creates the database connection on device boot and starts the location-based tracking service (if location-based
 * tracking is enabled) and/or the wifi-based tracking service. Also schedules periodic intents for {@link Watchdog}
 * which in turn checks if {@link LocationTrackerService} needs to be (re-)started.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class Basics extends BroadcastReceiver {
	
	// check once every minute
	private static final long REPEAT_TIME = 1000 * 60;
	
	// notification IDs
	/** used for the message about ACCESS_COARSE_LOCATION */
	public static final int MISSING_PRIVILEGE_ACCESS_COARSE_LOCATION_ID = 1;
	/** used for the status notification when clocked in */
	public static final int PERSISTENT_STATUS_ID = 2;
	/** used for the message about ACCESS_WIFI_STATE */
	public static final int MISSING_PRIVILEGE_ACCESS_WIFI_STATE_ID = 3;
	
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
		Intent intentToSchedule = new Intent(context, Watchdog.class);
		PendingIntent pendingIntent =
			PendingIntent.getBroadcast(context, 0, intentToSchedule, PendingIntent.FLAG_CANCEL_CURRENT);
		
		Calendar cal = Calendar.getInstance();
		// start one minute after boot completed
		cal.add(Calendar.MINUTE, 1);
		
		// schedule once every minute
		service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), REPEAT_TIME, pendingIntent);
	}
	
	/**
	 * Hook method which gets called approx. once a minute.
	 */
	public void periodicHook() {
		Logger.debug("executing periodic hook");
		// first make sure that the options are consistent
		safeCheckPreferences();
		// then start the action
		safeCheckLocationBasedTracking();
		safeCheckWifiBasedTracking();
		safeCheckPersistentNotification();
	}
	
	/**
	 * Wrapper for {@link PreferencesUtil#checkAllPreferenceSections()} that doesn't throw any exception.
	 */
	private void safeCheckPreferences() {
		try {
			PreferencesUtil.checkAllPreferenceSections();
		} catch (Exception e) {
			ACRA.getErrorReporter().handleException(e);
		}
	}
	
	/**
	 * Wrapper for {@link #checkLocationBasedTracking()} that doesn't throw any exception.
	 */
	public void safeCheckLocationBasedTracking() {
		try {
			checkLocationBasedTracking();
		} catch (Exception e) {
			ACRA.getErrorReporter().handleException(e);
		}
	}
	
	/**
	 * Wrapper for {@link #checkWifiBasedTracking()} that doesn't throw any exception.
	 */
	public void safeCheckWifiBasedTracking() {
		try {
			checkWifiBasedTracking();
		} catch (Exception e) {
			ACRA.getErrorReporter().handleException(e);
		}
	}
	
	/**
	 * Wrapper for {@link #checkPersistentNotification()} that doesn't throw any exception.
	 */
	public void safeCheckPersistentNotification() {
		try {
			checkPersistentNotification();
		} catch (Exception e) {
			ACRA.getErrorReporter().handleException(e);
		}
	}
	
	/**
	 * Check if persistent notification has to be displayed/updated/removed. Only works when "flexi time" enabled!
	 */
	public void checkPersistentNotification() {
		Logger.debug("checking persistent notification");
		if (preferences.getBoolean(Key.ENABLE_FLEXI_TIME.getName(), false) && timerManager.isTracking()) {
			// display/update
			Intent intent = new Intent(context, WorkTimeTrackerActivity.class);
			intent.setAction(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			String timeSoFar =
				timerManager.calculateTimeSum(DateTimeUtil.getCurrentDateTime(), PeriodEnum.DAY).toString();
			DateTime finishingTime = timerManager.getFinishingTime();
			String targetTime = (finishingTime == null ? null : DateTimeUtil.dateTimeToHourMinuteString(finishingTime));
			Logger.debug("persistent notification: worked={0} possiblefinish={1}", timeSoFar, targetTime);
			String targetTimeString = null;
			if (targetTime != null) {
				// target time in future
				targetTimeString = "possible finishing time: " + targetTime;
			} else if (targetTime == null && timerManager.isTodayWorkDay()) {
				// target time in past
				targetTimeString = "regular work time is over";
			} else {
				// nothing to do, this is not a working day
			}
			showNotification(null, "worked " + timeSoFar + " so far", targetTimeString, intent, PERSISTENT_STATUS_ID,
				true);
		} else {
			// try to remove
			NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(PERSISTENT_STATUS_ID);
		}
	}
	
	/**
	 * Check if location-based tracking has to be en- or disabled.
	 */
	public void checkLocationBasedTracking() {
		Logger.debug("checking location-based tracking");
		if (preferences.getBoolean(Key.LOCATION_BASED_TRACKING_ENABLED.getName(), false)) {
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
			Intent startIntent = buildLocationTrackerServiceIntent(latitude, longitude, tolerance, vibrate);
			// we can start the service again even if it is already running because
			// onStartCommand(...) in LocationTrackerService won't do anything if the service
			// is already running with the current parameters - if the location or the
			// tolerance changed, then it will update the values for the service
			Logger.debug("try to start location-based tracking service");
			context.startService(startIntent);
		} else {
			Intent stopIntent = buildLocationTrackerServiceIntent(null, null, null, null);
			context.stopService(stopIntent);
			Logger.debug("location-based tracking service stopped");
		}
	}
	
	/**
	 * Check if wifi-based tracking has to be en- or disabled and perfoem wifi-check
	 */
	public void checkWifiBasedTracking() {
		Logger.debug("checking wifi-based tracking");
		if (preferences.getBoolean(Key.WIFI_BASED_TRACKING_ENABLED.getName(), false)) {
			String ssid = preferences.getString(Key.WIFI_BASED_TRACKING_SSID.getName(), "unknown_ssid");
			Boolean vibrate = preferences.getBoolean(Key.WIFI_BASED_TRACKING_VIBRATE.getName(), Boolean.FALSE);
			Intent startIntent = buildWifiTrackerServiceIntent(ssid, vibrate);
			// changes to settings will be adopted & wifi-check will be performed
			Logger.debug("try to start wifi-based tracking service");
			context.startService(startIntent);
		} else {
			Intent stopIntent = buildWifiTrackerServiceIntent(null, null);
			context.stopService(stopIntent);
			Logger.debug("wifi-based tracking service stopped");
		}
	}
	
	/**
	 * Check the current device location and use that as work place.
	 * 
	 * @param reference an activity to use as reference for starting other activities
	 */
	public void useCurrentLocationAsWorkplace(final Activity reference) {
		requestCurrentLocation(new LocationCallback() {
			@Override
			public void callback(double latitude, double longitude, int tolerance) {
				boolean locationBasedTrackingEnabled =
					preferences.getBoolean(Key.LOCATION_BASED_TRACKING_ENABLED.getName(), false);
				
				Logger
					.debug(
						"received current device location: lat={0} long={1} tol={2} / location-based tracking already enabled = {3}",
						latitude, longitude, tolerance, locationBasedTrackingEnabled);
				
				SharedPreferences.Editor editor = preferences.edit();
				String roundedLatitude = CoordinateUtil.roundCoordinate(latitude);
				editor.putString(Key.LOCATION_BASED_TRACKING_LATITUDE.getName(), roundedLatitude);
				String roundedLongitude = CoordinateUtil.roundCoordinate(longitude);
				editor.putString(Key.LOCATION_BASED_TRACKING_LONGITUDE.getName(), roundedLongitude);
				editor.putString(Key.LOCATION_BASED_TRACKING_TOLERANCE.getName(), String.valueOf(tolerance));
				editor.commit();
				
				Intent i = new Intent(reference, OptionsActivity.class);
				reference.startActivity(i);
				
				Intent messageIntent =
					createMessageIntent(
						"New values:\n\nLatitude = "
							+ roundedLatitude
							+ "\nLongitude = "
							+ roundedLongitude
							+ "\nTolerance = "
							+ tolerance
							+ "\n\nPlease review the settings in the options. "
							+ (locationBasedTrackingEnabled ? "Location-based tracking was switched on already and is still enabled."
								: "You can now enable location-based tracking, just check \""
									+ reference.getText(R.string.enableLocationBasedTracking) + "\"."), null);
				reference.startActivity(messageIntent);
			}
			
			@Override
			public void error(Throwable t) {
				Logger.warn("error receiving the current device location: {0}", t);
				Intent messageIntent =
					createMessageIntent(
						"Could not get the current location. Please ensure that this app can access the coarse location.",
						null);
				reference.startActivity(messageIntent);
			}
		});
	}
	
	/**
	 * Queue a request for the current device location, determined by the network (not by GPS).
	 * 
	 * @param callback The callback which should be called when the position is found.
	 */
	public void requestCurrentLocation(final LocationCallback callback) {
		final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		try {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
				@Override
				public void onLocationChanged(Location location) {
					callback.callback(location.getLatitude(), location.getLongitude(),
						Math.round(location.getAccuracy()));
					// detach listener on first received location
					locationManager.removeUpdates(this);
				}
				
				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
					// nothing to do
				}
				
				@Override
				public void onProviderEnabled(String provider) {
					// nothing to do
					
				}
				
				@Override
				public void onProviderDisabled(String provider) {
					callback.error(new IllegalAccessException("provider disabled"));
				}
			});
		} catch (Throwable t) {
			callback.error(t);
		}
	}
	
	/**
	 * Show a notification.
	 * 
	 * @param scrollingText the text which appears in the status line when the notification is first displayed
	 * @param notificationTitle the title line of the notification
	 * @param notificationSubtitle the smaller line of text below the title
	 * @param intent the intent to be executed when the notification is clicked
	 * @param notificationId a unique number to identify the notification
	 */
	public void showNotification(String scrollingText, String notificationTitle, String notificationSubtitle,
		Intent intent, Integer notificationId, boolean persistent) {
		NotificationManager notificationManager =
			(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.ic_launcher_small, scrollingText, 0);
		if (persistent) {
			notification.flags = Notification.FLAG_ONGOING_EVENT;
		}
		notification.setLatestEventInfo(
			context,
			notificationTitle,
			notificationSubtitle,
			PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT
				| (persistent ? Notification.FLAG_ONGOING_EVENT : 0)));
		notificationManager.notify(notificationId, notification);
	}
	
	/**
	 * Create an intent which shows a message dialog.
	 * 
	 * @param text the message to display
	 * @param id the unique number, should correspond to the source notification (if any)
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
	 * Disable the location-based tracking.
	 */
	public void disableLocationBasedTracking() {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(Key.LOCATION_BASED_TRACKING_ENABLED.getName(), false);
		editor.commit();
	}
	
	/**
	 * Disable the wifi-based tracking.
	 */
	public void disableWifiBasedTracking() {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(Key.WIFI_BASED_TRACKING_ENABLED.getName(), false);
		editor.commit();
	}
	
	private Intent buildLocationTrackerServiceIntent(Double latitude, Double longitude, Double tolerance,
		Boolean vibrate) {
		Intent intent = new Intent(context, LocationTrackerService.class);
		intent.putExtra(LocationTrackerService.INTENT_EXTRA_LATITUDE, latitude);
		intent.putExtra(LocationTrackerService.INTENT_EXTRA_LONGITUDE, longitude);
		intent.putExtra(LocationTrackerService.INTENT_EXTRA_TOLERANCE, tolerance);
		intent.putExtra(LocationTrackerService.INTENT_EXTRA_VIBRATE, vibrate);
		return intent;
	}
	
	private Intent buildWifiTrackerServiceIntent(String ssid, Boolean vibrate) {
		Intent intent = new Intent(context, WifiTrackerService.class);
		intent.putExtra(WifiTrackerService.INTENT_EXTRA_SSID, ssid);
		intent.putExtra(WifiTrackerService.INTENT_EXTRA_VIBRATE, vibrate);
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
	
	/**
	 * Get the context.
	 */
	public Context getContext() {
		return context;
	}
	
}
