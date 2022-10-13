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
package org.zephyrsoft.trackworktime;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.timer.TimeCalculator;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.ExternalNotificationManager;
import org.zephyrsoft.trackworktime.util.PermissionsUtil;
import org.zephyrsoft.trackworktime.util.PreferencesUtil;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Creates the database connection on device boot and starts the location-based tracking service (if location-based
 * tracking is enabled) and/or the wifi-based tracking service. Also schedules periodic intents for {@link Watchdog}
 * which in turn checks if {@link LocationTrackerService} needs to be (re-)started.
 */
public class Basics {

    private Context context = null;
    private SharedPreferences preferences = null;
    private Uri documentTree = null;
    private DAO dao = null;
    private TimerManager timerManager = null;
    private TimeCalculator timeCalculator = null;
    private ExternalNotificationManager externalNotificationManager = null;
    private NotificationChannel notificationChannel = null;
    private NotificationChannel serviceNotificationChannel = null;
    private ThirdPartyReceiver thirdPartyReceiver;

    public Basics(Context context) {
        Logger.info("instantiating Basics");
        this.context = context;
        init();
        schedulePeriodicIntents();
    }

    private void init() {
        if (context == null) {
            throw new IllegalStateException("no context set");
        }
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (dao != null) {
            // let's be nice in case this was already initialized
            dao.close();
        }
        dao = new DAO(context, this);

        // run database migrations if needed
        dao.executePendingMigrations();

        timerManager = new TimerManager(dao, preferences, context);
        timeCalculator = new TimeCalculator(dao, timerManager);
        externalNotificationManager = new ExternalNotificationManager(context, preferences);

        initTinyLog();

        registerThirdPartyReceiver();
    }

    public void initTinyLog() {
        String threadToObserve = Thread.currentThread().getName();
        Configurator.defaultConfig()
            .writer(new RollingFileWriter(getCurrentLogFile().getPath(),
                    2, false, new CountLabeler(), new DailyPolicy()),
                Level.DEBUG, "{date:yyyy-MM-dd HH:mm:ss} {{level}|min-size=5} {class_name}.{method} - {message}")
            .addWriter(new LogcatWriter("trackworktime"), Level.DEBUG, "{message}")
            .writingThread(threadToObserve, 1)
            .activate();
        Logger.info("logger initialized - writing thread observes \"{}\"", threadToObserve);
    }

    public static Basics get(Context context) {
        if (context != null && context.getApplicationContext() instanceof WorkTimeTrackerApplication) {
            return get((WorkTimeTrackerApplication) context.getApplicationContext());
        } else {
            throw new IllegalStateException("no context given or of wrong type: " + context);
        }
    }

    public static Basics get(Activity activity) {
        if (activity != null) {
            return get(activity.getApplication());
        } else {
            throw new IllegalStateException("no activity given");
        }
    }

    public static Basics get(Application application) {
        if (application instanceof WorkTimeTrackerApplication) {
            return ((WorkTimeTrackerApplication) application).getBasics();
        } else if (application == null) {
            throw new IllegalStateException("no application given");
        } else {
            throw new IllegalStateException("application was of type " + application.getClass());
        }
    }

    private void registerThirdPartyReceiver() {
        if (thirdPartyReceiver != null) {
            Logger.warn("{} already registered, skipping.", ThirdPartyReceiver.class.getSimpleName());
            return;
        }

        thirdPartyReceiver = new ThirdPartyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.CLOCK_IN_ACTION);
        intentFilter.addAction(Constants.CLOCK_OUT_ACTION);
        intentFilter.addAction(Constants.STATUS_REQUEST_ACTION);
        context.registerReceiver(thirdPartyReceiver, intentFilter);
        Logger.debug("Registered {}", ThirdPartyReceiver.class.getSimpleName());
    }

    public void unregisterThirdPartyReceiver() {
        if (thirdPartyReceiver == null) {
            Logger.warn("{} not registered, skipping.", ThirdPartyReceiver.class.getSimpleName());
            return;
        }

        context.unregisterReceiver(thirdPartyReceiver);
        thirdPartyReceiver = null;
        Logger.debug("Unregistered {}", ThirdPartyReceiver.class.getSimpleName());
    }

    public File getCurrentLogFile() {
        return new File(context.getFilesDir(), Constants.CURRENT_LOG_FILE_NAME);
    }

    public NotificationChannel getNotificationChannel() {
        return notificationChannel;
    }

    public void setNotificationChannel(NotificationChannel notificationChannel) {
        this.notificationChannel = notificationChannel;
    }

    public NotificationChannel getServiceNotificationChannel() {
        return serviceNotificationChannel;
    }

    public void setServiceNotificationChannel(NotificationChannel serviceNotificationChannel) {
        this.serviceNotificationChannel = serviceNotificationChannel;
    }

    public void schedulePeriodicIntents() {
        AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intentToSchedule = new Intent(context, Watchdog.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intentToSchedule,
            PendingIntent.FLAG_CANCEL_CURRENT +
                (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                    ? PendingIntent.FLAG_IMMUTABLE
                    : 0));

        Calendar cal = Calendar.getInstance();
        // start one minute after boot completed
        cal.add(Calendar.MINUTE, 1);

        // schedule once every minute
        service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), Constants.REPEAT_TIME,
            pendingIntent);
        Logger.info("scheduled periodic intents for watchdog");
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
        safeCheckExternalControls();
        WorkTimeTrackerActivity.refreshViewIfShown();
    }

    /**
     * Wrapper for {@link PreferencesUtil#checkAllPreferenceSections(Context)} that doesn't throw any exception.
     */
    private void safeCheckPreferences() {
        try {
            PreferencesUtil.checkAllPreferenceSections(context);
        } catch (Exception e) {
            Logger.warn(e, "exception handled by ACRA");
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
            Logger.warn(e, "exception handled by ACRA");
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
            Logger.warn(e, "exception handled by ACRA");
            ACRA.getErrorReporter().handleException(e);
        }
    }

    /**
     * Updates any external views, such as notifications, app widgets, etc.
     */
    public void safeCheckExternalControls() {
        safeCheckWidget();
        safeCheckPersistentNotification();
        timerManager.notifyListeners();
    }

    /**
     * Wrapper for {@link #checkWidget()} that doesn't throw any exception.
     */
    public void safeCheckWidget() {
        try {
            checkWidget();
        } catch (Exception e) {
            Logger.warn(e, "exception handled by ACRA");
            ACRA.getErrorReporter().handleException(e);
        }
    }

    /**
     * Dispatches refresh event to {@link Widget}
     */
    public void checkWidget() {
        Widget.dispatchUpdateIntent(context);
    }

    /**
     * Wrapper for {@link #checkPersistentNotification()} that doesn't throw any exception.
     */
    public void safeCheckPersistentNotification() {
        try {
            checkPersistentNotification();
        } catch (Exception e) {
            Logger.warn(e, "exception handled by ACRA");
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
            clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            clickIntent.setAction(Intent.ACTION_MAIN);
            clickIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            Intent buttonOneIntent = new Intent(Constants.CLOCK_IN_ACTION);
            Intent buttonTwoIntent = new Intent(Constants.CLOCK_OUT_ACTION);

            String title;
            String text = null;
            if (preferences.getBoolean(Key.NEVER_UPDATE_PERSISTENT_NOTIFICATION.getName(), false)) {
                OffsetDateTime lastClockIn = timerManager.getLastClockIn();
                title = context.getString(R.string.notificationTitle2, DateTimeUtil.formatLocalizedTime(lastClockIn, getLocale()));
            } else {
                // calculated in home time zone
                int workedTime = (int) timerManager.calculateTimeSum(LocalDate.now(), PeriodEnum.DAY);
                String timeSoFar = DateTimeUtil.formatDuration(workedTime);
                title = context.getString(R.string.notificationTitle1, timeSoFar);
                if (preferences.getBoolean(Key.ENABLE_FLEXI_TIME.getName(), false)) {
                    Integer minutesRemaining = timerManager.getMinutesRemaining();

                    if (minutesRemaining != null) {
                        if (minutesRemaining >= 0) {
                            // target time in future
                            LocalDateTime finishingTime = LocalDateTime.now().plusMinutes(minutesRemaining);

                            if (finishingTime.toLocalDate().isEqual(LocalDate.now())) {
                                String targetTime = DateTimeUtil.formatLocalizedTime(finishingTime, getLocale());
                                text = context.getString(R.string.notificationText1, targetTime);
                            } else {
                                text = context.getString(R.string.notificationText2);
                            }
                        } else {
                            // target time in past
                            text = context.getString(R.string.notificationText3, TimerManager.formatTime(-minutesRemaining));
                        }
                    } // else not a working day
                } else {
                    // no second line displayed because no flexi time can be calculated
                }
            }
            Logger.debug("prepared persistent notification: title={} text={}", title, text);
            Boolean notificationActive = isNotificationActive(Constants.PERSISTENT_STATUS_ID);
            if (preferences.getBoolean(Key.NEVER_UPDATE_PERSISTENT_NOTIFICATION.getName(), false)
                && notificationActive != null
                && notificationActive) {
                Logger.debug("not updated persistent notification, configuration forbids it");
            } else {
                showNotification(null, title, text,
                    PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT +
                        (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                            ? PendingIntent.FLAG_IMMUTABLE
                            : 0)),
                    Constants.PERSISTENT_STATUS_ID, true,
                    PendingIntent.getBroadcast(context, 0, buttonOneIntent, PendingIntent.FLAG_CANCEL_CURRENT +
                        (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                            ? PendingIntent.FLAG_IMMUTABLE
                            : 0)),
                    R.drawable.ic_menu_forward, context.getString(R.string.clockInChangeShort),
                    PendingIntent.getBroadcast(context, 0, buttonTwoIntent, PendingIntent.FLAG_CANCEL_CURRENT +
                        (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                            ? PendingIntent.FLAG_IMMUTABLE
                            : 0)),
                    R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.clockOutShort));
                Logger.debug("added persistent notification");
            }
        } else {
            // try to remove
            safeRemovePersistentNotification();
        }
    }

    private void safeRemovePersistentNotification() {
        try {
            NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(Constants.PERSISTENT_STATUS_ID);
            Logger.debug("removed persistent notification");
        } catch (Exception e) {
            Logger.warn(e, "could not remove persistent notification");
        }
    }

    /**
     * correct type of persistent notification (updatable or non-updatable)
     * because the type was just changed in the options
     */
    public void fixPersistentNotification() {
        Logger.debug("fixing persistent notification");
        if (preferences.getBoolean(Key.NOTIFICATION_ENABLED.getName(), false)
            && timerManager.isTracking()) {
            safeRemovePersistentNotification();
            checkPersistentNotification();
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
                stopLocationTrackerService();
            }
        } else {
            stopLocationTrackerService();
        }
    }

    /**
     * start the location-based tracking service by serviceIntent
     */
    private void startLocationTrackerService(double latitude, double longitude, double tolerance, Boolean vibrate) {
        try {
            Intent startIntent = buildLocationTrackerServiceIntent(latitude, longitude, tolerance, vibrate);
            // we can start the service again even if it is already running because
            // onStartCommand(...) in LocationTrackerService won't do anything if the service
            // is already running with the current parameters - if the location or the
            // tolerance changed, then it will update the values for the service
            Logger.debug("try to start location-based tracking service");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent);
            } else {
                context.startService(startIntent);
            }
            Logger.debug("location-based tracking service started");
        } catch (Exception e) {
            Logger.warn(e, "could not start location-based tracking service");
        }
    }

    /**
     * stop the location-based tracking service by serviceIntent
     */
    private void stopLocationTrackerService() {
        try {
            Intent stopIntent = buildLocationTrackerServiceIntent(null, null, null, null);
            context.stopService(stopIntent);
            Logger.debug("location-based tracking service stopped");
        } catch (Exception e) {
            Logger.warn(e, "could not stop location-based tracking service");
        }
    }

    /**
     * Check if wifi-based tracking has to be en- or disabled and perform wifi-check
     */
    private void checkWifiBasedTracking() {
        Logger.debug("checking wifi-based tracking");
        if (preferences.getBoolean(Key.WIFI_BASED_TRACKING_ENABLED.getName(), false)) {
            String ssid = preferences.getString(Key.WIFI_BASED_TRACKING_SSID.getName(), null);
            Boolean vibrate = preferences.getBoolean(Key.WIFI_BASED_TRACKING_VIBRATE.getName(), Boolean.FALSE);
            String checkIntervalString = preferences.getString(Key.WIFI_BASED_TRACKING_CHECK_INTERVAL.getName(), "1");
            Integer checkInterval = checkIntervalString == null
                ? 1
                : Integer.parseInt(checkIntervalString);
            if (ssid != null && ssid.length() > 0) {
                startWifiTrackerService(ssid, vibrate, checkInterval);
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
    private void startWifiTrackerService(String ssid, Boolean vibrate, Integer checkInterval) {
        try {
            Intent startIntent = buildWifiTrackerServiceIntent(ssid, vibrate, checkInterval);
            Logger.debug("try to start wifi-based tracking service");
            // changes to settings will be adopted & wifi-check will be performed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent);
            } else {
                context.startService(startIntent);
            }
            Logger.debug("wifi-based tracking service started");
        } catch (Exception e) {
            Logger.warn(e, "could not start wifi-based tracking service");
        }
    }

    /**
     * stop the wifi-based tracking service by serviceIntent
     */
    private void stopWifiTrackerService() {
        try {
            Intent stopIntent = buildWifiTrackerServiceIntent(null, null, null);
            context.stopService(stopIntent);
            Logger.debug("wifi-based tracking service stopped");
        } catch (Exception e) {
            Logger.warn(e, "could not stop wifi-based tracking service");
        }
    }

    /**
     * Check the current device location and use that as work place.
     * <p>
     * Only to be called when all necessary permissions are granted!
     */
    @SuppressLint("MissingPermission")
    public void useCurrentLocationAsWorkplace(Activity activity) {
        LocationCallback callback = new LocationCallback() {
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

                String toleranceStr = String.valueOf(tolerance);
                editor.putString(Key.LOCATION_BASED_TRACKING_TOLERANCE.getName(), toleranceStr);
                editor.commit();

                Intent i = new Intent(activity, OptionsActivity.class);
                activity.startActivity(i);

                Intent messageIntent = createMessageIntent(
                    activity.getString(R.string.currentLocationAsWorkplaceSuccess,
                        roundedLatitude, roundedLongitude, toleranceStr)
                        + (locationBasedTrackingEnabled
                        ? activity.getString(R.string.currentLocationAsWorkplaceSuccessExt1)
                        : activity.getString(R.string.currentLocationAsWorkplaceSuccessExt2,
                        activity.getText(R.string.enableLocationBasedTracking))), null);
                activity.startActivity(messageIntent);
            }

            @Override
            public void error(Throwable t) {
                Logger.warn(t, "error receiving the current device location");
                Intent messageIntent = createMessageIntent(
                    activity.getString(R.string.currentLocationAsWorkplaceError),
                    null);
                activity.startActivity(messageIntent);
            }
        };

        try {
            final LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            List<String> enabledProviders = locationManager.getProviders(true);
            String provider = LocationManager.PASSIVE_PROVIDER;
            if (enabledProviders.contains(LocationManager.NETWORK_PROVIDER)) {
                // preferred: coarse location, determined by cell towers
                provider = LocationManager.NETWORK_PROVIDER;
            } else if (enabledProviders.contains(LocationManager.FUSED_PROVIDER)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // second best: the location provider preferred by Android for low power consumption
                provider = LocationManager.FUSED_PROVIDER;
            } else if (enabledProviders.contains(LocationManager.GPS_PROVIDER)) {
                // if the above providers can't be used: GPS
                // (this seems appropriate here because it's only a one-time request)
                provider = LocationManager.GPS_PROVIDER;
            }
            Logger.info("using location provider \"{}\" out of {}", provider, enabledProviders);
            locationManager.requestLocationUpdates(provider, 0, 0, new LocationListener() {
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
        } catch (Throwable t) {
            callback.error(t);
        }
    }

    /**
     * Show a notification.
     *
     * @param scrollingText        the text which appears in the status line when the notification is first displayed
     * @param notificationTitle    the title line of the notification
     * @param notificationSubtitle the smaller line of text below the title
     * @param clickIntent          the intent to be executed when the notification is clicked
     * @param notificationId       a unique number to identify the notification
     */
    public void showNotification(String scrollingText, String notificationTitle, String notificationSubtitle,
                                 PendingIntent clickIntent, @NonNull Integer notificationId, boolean persistent,
                                 PendingIntent buttonOneIntent, Integer buttonOneIcon, String buttonOneText,
                                 PendingIntent buttonTwoIntent, Integer buttonTwoIcon, String buttonTwoText) {
        try {
            if (PermissionsUtil.isNotificationPermissionMissing(context)) {
                PreferencesUtil.disablePreference(preferences, Key.NOTIFICATION_ENABLED);
                Toast.makeText(context, context.getString(R.string.notification_permission_removed), Toast.LENGTH_LONG).show();
                return;
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            Notification notification = createNotification(scrollingText, notificationTitle, notificationSubtitle, clickIntent, persistent, buttonOneIntent, buttonOneIcon, buttonOneText, buttonTwoIntent, buttonTwoIcon, buttonTwoText);
            notificationManager.notify(notificationId, notification);
            Logger.debug("displayed/updated notification {} / {} with button1={} and button2={}",
                notificationTitle,
                notificationSubtitle, buttonOneText, buttonTwoText);
        } catch (Exception e) {
            Logger.warn(e, "could not display/update notification");
        }
    }

    public Notification createNotification(String scrollingText, String notificationTitle, String notificationSubtitle, PendingIntent clickIntent, boolean persistent, PendingIntent buttonOneIntent, Integer buttonOneIcon, String buttonOneText, PendingIntent buttonTwoIntent, Integer buttonTwoIcon, String buttonTwoText) {
        @SuppressWarnings("deprecation")
        NotificationCompat.Builder notificationBuilder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new NotificationCompat.Builder(context, notificationChannel.getId())
            : new NotificationCompat.Builder(context));
        notificationBuilder
            .setContentTitle(notificationTitle)
            .setContentText(notificationSubtitle)
            .setContentIntent(clickIntent)
            .setSmallIcon(R.drawable.ic_notification)
            .setTicker(scrollingText)
            .setCategory(persistent
                ? (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Notification.CATEGORY_REMINDER : Notification.CATEGORY_PROGRESS)
                : Notification.CATEGORY_EVENT)
            .setOnlyAlertOnce(true)
            .setOngoing(persistent)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .setSortKey("A is first");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(getNotificationChannel().getId());
        }
        if (buttonOneIntent != null && buttonOneIcon != null) {
            notificationBuilder.addAction(buttonOneIcon, buttonOneText, buttonOneIntent);
        }
        if (buttonTwoIntent != null && buttonTwoIcon != null) {
            notificationBuilder.addAction(buttonTwoIcon, buttonTwoText, buttonTwoIntent);
        }
        return notificationBuilder.build();
    }

    public Boolean isNotificationActive(int id) {
        try {
            NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (StatusBarNotification n : notificationManager.getActiveNotifications()) {
                    if (n.getId() == id) {
                        return Boolean.TRUE;
                    }
                }
                return Boolean.FALSE;
            } else {
                return null;
            }
        } catch (Exception e) {
            Logger.warn(e, "could not check for notification");
            return null;
        }
    }

    public void removeNotification(int id) {
        try {
            NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(id);
        } catch (Exception e) {
            Logger.warn(e, "could not remove notification {}", id);
        }
    }

    public Notification createNotificationTracking() {
        Intent clickIntent = new Intent(context, WorkTimeTrackerActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        clickIntent.setAction(Intent.ACTION_MAIN);
        clickIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT +
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE
                : 0));

        Notification.Builder notificationBuilder = new Notification.Builder(context)
            .setContentTitle(context.getString(R.string.serviceNotificationTitle))
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .setSortKey("B is second");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(getServiceNotificationChannel().getId());
        }
        Logger.debug("created service notification");
        return notificationBuilder.build();
    }

    /**
     * Create an intent which shows a message dialog.
     *
     * @param text the message to display
     * @param id   the unique number, should correspond to the source notification (if any)
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
        return PendingIntent.getActivity(context, 0, createMessageIntent(text, id), PendingIntent.FLAG_UPDATE_CURRENT +
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE
                : 0));
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
     * Enable the location-based tracking if it's disabled.
     */
    public void enableLocationBasedTracking() {
        if (!preferences.getBoolean(Key.LOCATION_BASED_TRACKING_ENABLED.getName(), true)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Key.LOCATION_BASED_TRACKING_ENABLED.getName(), true);
            editor.commit();
        }
    }

    /**
     * Disable the wifi-based tracking.
     */
    public void disableWifiBasedTracking() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Key.WIFI_BASED_TRACKING_ENABLED.getName(), false);
        editor.commit();
    }

    /**
     * Set home time zone for cache database and week display.
     */
    public void setHomeTimeZone(ZoneId zoneId) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Key.HOME_TIME_ZONE.getName(), zoneId.getId());
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

    private Intent buildWifiTrackerServiceIntent(String ssid, Boolean vibrate, Integer checkInterval) {
        Intent intent = new Intent(context, WifiTrackerService.class);
        intent.putExtra(Constants.INTENT_EXTRA_SSID, ssid);
        intent.putExtra(Constants.INTENT_EXTRA_VIBRATE, vibrate);
        intent.putExtra(Constants.INTENT_EXTRA_WIFI_CHECK_INTERVAL, checkInterval);
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
            Logger.error(nnfe, "could not get version name from manifest");
            return "?";
        }
    }

    public boolean hasToRemoveAppFromBatteryOptimization() {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && !pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    @SuppressLint("BatteryLife")
    public void removeAppFromBatteryOptimization() {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && !pm.isIgnoringBatteryOptimizations(context.getPackageName())) {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            context.startActivity(intent);
        }
    }

    public Uri getDocumentTree() {
        if (documentTree == null) {
            String documenTreeString = preferences.getString(getContext().getString(R.string.keyGrantedDocumentTree), null);
            documentTree = documenTreeString == null
                ? null
                : Uri.parse(documenTreeString);
        }
        return documentTree;
    }

    public Locale getLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return context.getResources().getConfiguration().locale;
        }
    }
}
