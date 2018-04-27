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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import org.acra.ACRA;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.labelers.CountLabeler;
import org.pmw.tinylog.policies.DailyPolicy;
import org.pmw.tinylog.writers.LogcatWriter;
import org.pmw.tinylog.writers.RollingFileWriter;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.location.CoordinateUtil;
import org.zephyrsoft.trackworktime.location.LocationCallback;
import org.zephyrsoft.trackworktime.location.LocationTrackerService;
import org.zephyrsoft.trackworktime.location.WifiTrackerService;
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.timer.TimeCalculator;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.ExternalNotificationManager;
import org.zephyrsoft.trackworktime.util.PreferencesUtil;

import java.io.File;
import java.util.Calendar;

import hirondelle.date4j.DateTime;
import hirondelle.date4j.DateTime.DayOverflow;

/**
 * Creates the database connection on device boot and starts the location-based tracking service (if location-based
 * tracking is enabled) and/or the wifi-based tracking service. Also schedules periodic intents for {@link Watchdog}
 * which in turn checks if {@link LocationTrackerService} needs to be (re-)started.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class Basics extends BroadcastReceiver {

    private Context context = null;
	private SharedPreferences preferences = null;
	private DAO dao = null;
	private TimerManager timerManager = null;
	private TimeCalculator timeCalculator = null;
	private ExternalNotificationManager externalNotificationManager = null;
	private NotificationChannel notificationChannel = null;

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
		timerManager = new TimerManager(dao, preferences, context);
		timeCalculator = new TimeCalculator(dao, timerManager);
		externalNotificationManager = new ExternalNotificationManager(context);

		// init TinyLog
		String threadToObserve = Thread.currentThread().getName();
		Configurator.defaultConfig()
			.writer(new RollingFileWriter(getDataDirectory().getAbsolutePath() + File.separatorChar + Constants.CURRENT_LOG_FILE_NAME,
					2, false, new CountLabeler(), new DailyPolicy()),
					Level.DEBUG, "{date:yyyy-MM-dd HH:mm:ss} {{level}|min-size=5} {class_name}.{method} - {message}")
			.addWriter(new LogcatWriter("trackworktime"), Level.DEBUG, "{message}")
			.writingThread(threadToObserve, 1)
			.activate();
		Logger.info("logger initialized - writing thread observes \"{}\"", threadToObserve);
	}

	public File getCurrentLogFile() {
		return new File(getDataDirectory(), Constants.CURRENT_LOG_FILE_NAME);
	}

	public File getDataDirectory() {
		File backupDir = new File(".");
		final File externalStorageDirectory = Environment.getExternalStorageDirectory();
		if (externalStorageDirectory != null) {
			backupDir = new File(externalStorageDirectory, Constants.DATA_DIR);
		} else {
			Logger.warn("external storage directory not available");
		}
		return backupDir;
	}

	public NotificationChannel getNotificationChannel() {
		return notificationChannel;
	}

	public void setNotificationChannel(NotificationChannel notificationChannel) {
		this.notificationChannel = notificationChannel;
	}

	private void schedulePeriodicIntents() {
		AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intentToSchedule = new Intent(context, Watchdog.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intentToSchedule,
			PendingIntent.FLAG_CANCEL_CURRENT);

		Calendar cal = Calendar.getInstance();
		// start one minute after boot completed
		cal.add(Calendar.MINUTE, 1);

		// schedule once every minute
		service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), Constants.REPEAT_TIME,
			pendingIntent);
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
			ACRA.getErrorReporter().handleException(e);
		}
	}

	/**
	 * Check if persistent notification has to be displayed/updated/removed. Only works when "flexi time" enabled!
	 */
	private void checkPersistentNotification() {
		Logger.debug("checking persistent notification");
		if (preferences.getBoolean(Key.NOTIFICATION_ENABLED.getName(), false)
			&& timerManager.isTracking()) {
			// display/update

			Intent clickIntent = new Intent(context, WorkTimeTrackerActivity.class);
			clickIntent.setAction(Intent.ACTION_MAIN);
			clickIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			Intent buttonOneIntent = new Intent("org.zephyrsoft.trackworktime.ClockIn");
			Intent buttonTwoIntent = new Intent("org.zephyrsoft.trackworktime.ClockOut");

			String timeSoFar = timerManager.calculateTimeSum(DateTimeUtil.getCurrentDateTime(), PeriodEnum.DAY)
				.toString();
			String targetTimeString = "";
			if (preferences.getBoolean(Key.ENABLE_FLEXI_TIME.getName(), false)) {
				final Integer minutesRemaining = timerManager.getMinutesRemaining(preferences.getBoolean(
					Key.NOTIFICATION_USES_FLEXI_TIME_AS_TARGET.getName(), false));
				if (minutesRemaining != null) {
					if (minutesRemaining >= 0) {
						// target time in future
						DateTime finishingTime = DateTimeUtil.getCurrentDateTime()
							.plus(0, 0, 0, 0, minutesRemaining, 0, 0, DayOverflow.Spillover);
						String targetTime = (finishingTime == null ? null : DateTimeUtil
							.dateTimeToHourMinuteString(finishingTime));
						targetTimeString = "possible finishing time: " + targetTime;
					} else {
						// target time in past
						TimeSum timeSum = new TimeSum();
						timeSum.add(0, -minutesRemaining);
						targetTimeString = "regular work time is over since " + timeSum.toString();
					}
				} // else not a working day
			} else {
				// no second line displayed because no flexi time can be calculated
			}
			Logger.debug("persistent notification: worked={} possiblefinish={}", timeSoFar, targetTimeString);
			showNotification(null, "worked " + timeSoFar + " so far", targetTimeString,
				PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT),
				Constants.PERSISTENT_STATUS_ID, true,
				PendingIntent.getBroadcast(context, 0, buttonOneIntent, PendingIntent.FLAG_CANCEL_CURRENT),
				R.drawable.ic_menu_forward, context.getString(R.string.clockInChangeShort),
				PendingIntent.getBroadcast(context, 0, buttonTwoIntent, PendingIntent.FLAG_CANCEL_CURRENT),
				R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.clockOutShort));
			Logger.debug("added persistent notification");
		} else {
			// try to remove
			NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(Constants.PERSISTENT_STATUS_ID);
			Logger.debug("removed persistent notification");
		}
	}

	/**
	 * Check if location-based tracking has to be en- or disabled.
	 */
	private void checkLocationBasedTracking() {
		Logger.debug("checking location-based tracking");
		if (preferences.getBoolean(Key.LOCATION_BASED_TRACKING_ENABLED.getName(), false)) {
			String latitudeString = preferences.getString(Key.LOCATION_BASED_TRACKING_LATITUDE.getName(), "0");
			String longitudeString = preferences.getString(Key.LOCATION_BASED_TRACKING_LONGITUDE.getName(), "0");
			String toleranceString = preferences.getString(Key.LOCATION_BASED_TRACKING_TOLERANCE.getName(), "0");
			double latitude = 0.0;
			boolean valuesParsable = true;

			try {
				latitude = Double.parseDouble(latitudeString);
			} catch (NumberFormatException nfe) {
				Logger.warn("could not parse latitude: {}", latitudeString);
				valuesParsable = false;
			}
			double longitude = 0.0;
			try {
				longitude = Double.parseDouble(longitudeString);
			} catch (NumberFormatException nfe) {
				Logger.warn("could not parse longitude: {}", longitudeString);
				valuesParsable = false;
			}
			double tolerance = 0.0;
			try {
				tolerance = Double.parseDouble(toleranceString);
			} catch (NumberFormatException nfe) {
				Logger.warn("could not parse tolerance: {}", toleranceString);
				valuesParsable = false;
			}
			Boolean vibrate = preferences.getBoolean(Key.LOCATION_BASED_TRACKING_VIBRATE.getName(), Boolean.FALSE);

			if (valuesParsable) {
				startLocationTrackerService(latitude, longitude, tolerance, vibrate);
			} else {
				// just in case
				stopWifiTrackerService();
			}
		} else {
			stopLocationTrackerService();
		}
	}

	/**
	 * start the location-based tracking service by serviceIntent
	 */
	private void startLocationTrackerService(double latitude, double longitude, double tolerance, Boolean vibrate) {
		Intent startIntent = buildLocationTrackerServiceIntent(latitude, longitude, tolerance, vibrate);
		// we can start the service again even if it is already running because
		// onStartCommand(...) in LocationTrackerService won't do anything if the service
		// is already running with the current parameters - if the location or the
		// tolerance changed, then it will update the values for the service
		Logger.debug("try to start location-based tracking service");
		context.startService(startIntent);
		Logger.debug("location-based tracking service started");
	}

	/**
	 * stop the location-based tracking service by serviceIntent
	 */
	private void stopLocationTrackerService() {
		Intent stopIntent = buildLocationTrackerServiceIntent(null, null, null, null);
		context.stopService(stopIntent);
		Logger.debug("location-based tracking service stopped");
	}

	/**
	 * Check if wifi-based tracking has to be en- or disabled and perform wifi-check
	 */
	private void checkWifiBasedTracking() {
		Logger.debug("checking wifi-based tracking");
		if (preferences.getBoolean(Key.WIFI_BASED_TRACKING_ENABLED.getName(), false)) {
			String ssid = preferences.getString(Key.WIFI_BASED_TRACKING_SSID.getName(), null);
			Boolean vibrate = preferences.getBoolean(Key.WIFI_BASED_TRACKING_VIBRATE.getName(), Boolean.FALSE);
			if (ssid != null && ssid.length() > 0) {
				startWifiTrackerService(ssid, vibrate);
			} else {
				Logger.warn("NOT starting wifi-based tracking service, the configured SSID is empty");
				// just in case
				stopWifiTrackerService();
			}
		} else {
			stopWifiTrackerService();
		}
	}

	/**
	 * start the wifi-based tracking service by serviceIntent
	 */
	private void startWifiTrackerService(String ssid, Boolean vibrate) {
		Intent startIntent = buildWifiTrackerServiceIntent(ssid, vibrate);
		Logger.debug("try to start wifi-based tracking service");
		// changes to settings will be adopted & wifi-check will be performed
		context.startService(startIntent);
		Logger.debug("wifi-based tracking service started");
	}

	/**
	 * stop the wifi-based tracking service by serviceIntent
	 */
	private void stopWifiTrackerService() {
		Intent stopIntent = buildWifiTrackerServiceIntent(null, null);
		context.stopService(stopIntent);
		Logger.debug("wifi-based tracking service stopped");
	}

	/**
	 * Check the current device location and use that as work place.
	 * 
	 * @param reference
	 *            an activity to use as reference for starting other activities
	 */
	public void useCurrentLocationAsWorkplace(final Activity reference) {
		requestCurrentLocation(new LocationCallback() {
			@Override
			public void callback(double latitude, double longitude, int tolerance) {
				boolean locationBasedTrackingEnabled = preferences.getBoolean(Key.LOCATION_BASED_TRACKING_ENABLED
					.getName(), false);

				Logger
					.debug(
						"received current device location: lat={} long={} tol={} / location-based tracking already enabled = {}",
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

				Intent messageIntent = createMessageIntent(
					"New values:\n\nLatitude = "
						+ roundedLatitude
						+ "\nLongitude = "
						+ roundedLongitude
						+ "\nTolerance = "
						+ tolerance
						+ "\n\nPlease review the settings in the options. "
						+ (locationBasedTrackingEnabled
							? "Location-based tracking was switched on already and is still enabled."
							: "You can now enable location-based tracking, just check \""
								+ reference.getText(R.string.enableLocationBasedTracking) + "\"."), null);
				reference.startActivity(messageIntent);
			}

			@Override
			public void error(Throwable t) {
				Logger.warn("error receiving the current device location: {}", t);
				Intent messageIntent = createMessageIntent(
					"Could not get the current location. Please ensure that this app can access the coarse location.",
					null);
				reference.startActivity(messageIntent);
			}
		});
	}

	/**
	 * Queue a request for the current device location, determined by the network (not by GPS).
	 * 
	 * @param callback
	 *            The callback which should be called when the position is found.
	 */
	public void requestCurrentLocation(final LocationCallback callback) {
		final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		try {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
				@Override
				public void onLocationChanged(Location location) {
					callback.callback(location.getLatitude(), location.getLongitude(), Math.round(location
							.getAccuracy()));
					try {
						// detach listener on first received location
						locationManager.removeUpdates(this);
					} catch (SecurityException se) {
						Logger.error("could not remove updates because of missing rights");
					}
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
		} catch (SecurityException se) {
			callback.error(se);
		} catch (Throwable t) {
			callback.error(t);
		}
	}

	/**
	 * Show a notification.
	 * 
	 * @param scrollingText
	 *            the text which appears in the status line when the notification is first displayed
	 * @param notificationTitle
	 *            the title line of the notification
	 * @param notificationSubtitle
	 *            the smaller line of text below the title
	 * @param clickIntent
	 *            the intent to be executed when the notification is clicked
	 * @param notificationId
	 *            a unique number to identify the notification
	 */
	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void showNotification(String scrollingText, String notificationTitle, String notificationSubtitle,
		PendingIntent clickIntent, Integer notificationId, boolean persistent, PendingIntent buttonOneIntent,
		Integer buttonOneIcon, String buttonOneText, PendingIntent buttonTwoIntent, Integer buttonTwoIcon,
		String buttonTwoText) {
		NotificationManager notificationManager = (NotificationManager) context
			.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			Notification.Builder notificationBuilder = new Notification.Builder(context)
				.setContentTitle(notificationTitle)
				.setContentText(notificationSubtitle)
				.setContentIntent(clickIntent)
				.setSmallIcon(R.drawable.ic_launcher)
				.setTicker(scrollingText)
				.setOngoing(persistent);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				notificationBuilder.setChannelId(getNotificationChannel().getId());
			}
			if (buttonOneIntent != null && buttonOneIcon != null) {
				notificationBuilder.addAction(buttonOneIcon, buttonOneText, buttonOneIntent);
			}
			if (buttonTwoIntent != null && buttonTwoIcon != null) {
				notificationBuilder.addAction(buttonTwoIcon, buttonTwoText, buttonTwoIntent);
			}
			notification = notificationBuilder.build();
			Logger.debug("prepared JellyBean+ notification {} / {} with button1={} and button2={}",
				notificationTitle,
				notificationSubtitle, buttonOneText, buttonTwoText);
		} else {
			notification = new Notification(R.drawable.ic_launcher, scrollingText, 0);
			if (persistent) {
				notification.flags = Notification.FLAG_ONGOING_EVENT;
			}
//			notification.setLatestEventInfo(context, notificationTitle, notificationSubtitle, clickIntent);
			Logger.debug("prepared pre-JellyBean notification {} / {} with button1={} and button2={}",
				notificationTitle, notificationSubtitle, buttonOneText, buttonTwoText);
		}
		notificationManager.notify(notificationId, notification);
	}

	/**
	 * Create an intent which shows a message dialog.
	 * 
	 * @param text
	 *            the message to display
	 * @param id
	 *            the unique number, should correspond to the source notification (if any)
	 */
	public Intent createMessageIntent(String text, Integer id) {
		Intent messageIntent = new Intent(context, MessageActivity.class);
		messageIntent.putExtra(Constants.MESSAGE_EXTRA_KEY, text);
		if (id != null) {
			messageIntent.putExtra(Constants.ID_EXTRA_KEY, id.intValue());
		}
		return messageIntent;
	}

	public PendingIntent createMessagePendingIntent(String text, Integer id) {
		return PendingIntent.getActivity(context, 0, createMessageIntent(text, id), PendingIntent.FLAG_UPDATE_CURRENT);
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
		intent.putExtra(Constants.INTENT_EXTRA_LATITUDE, latitude);
		intent.putExtra(Constants.INTENT_EXTRA_LONGITUDE, longitude);
		intent.putExtra(Constants.INTENT_EXTRA_TOLERANCE, tolerance);
		intent.putExtra(Constants.INTENT_EXTRA_VIBRATE, vibrate);
		return intent;
	}

	private Intent buildWifiTrackerServiceIntent(String ssid, Boolean vibrate) {
		Intent intent = new Intent(context, WifiTrackerService.class);
		intent.putExtra(Constants.INTENT_EXTRA_SSID, ssid);
		intent.putExtra(Constants.INTENT_EXTRA_VIBRATE, vibrate);
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
	 * The app's time calculator.
	 */
	public TimeCalculator getTimeCalculator() {
		return timeCalculator;
	}

	/**
	 * The wrapper for Android's {@link Vibrator} and Pebble's Notification API.
	 */
	public ExternalNotificationManager getExternalNotificationManager() {
		return externalNotificationManager;
	}

	/**
	 * Get the context.
	 */
	public Context getContext() {
		return context;
	}

	public boolean isDevelopmentVersion() {
		String versionName = getVersionName();
		return versionName != null && versionName.contains("-SNAPSHOT");
	}

	public String getVersionName() {
		try {
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException nnfe) {
			Logger.error("could not get version name from manifest: {}", nnfe.getMessage());
			return "?";
		}
	}
}
