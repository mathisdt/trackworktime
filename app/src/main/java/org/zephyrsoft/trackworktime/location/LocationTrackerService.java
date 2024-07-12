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
package org.zephyrsoft.trackworktime.location;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.Constants;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.util.PreferencesUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The background service providing the location-based tracking without having the activity open.
 */
public class LocationTrackerService extends Service {

    private static LocationTracker locationTracker = null;
    private int startId;

    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    private Basics basics = null;

    @Override
    public void onCreate() {
        Logger.info("creating LocationTrackerService");
        basics = Basics.get(getApplicationContext());
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(Constants.PERSISTENT_TRACKING_ID, basics.createNotificationTracking(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(Constants.PERSISTENT_TRACKING_ID, basics.createNotificationTracking());
            }
        } catch (SecurityException se) {
            // there are permissions missing, disable automatic tracking and tell user
            Logger.warn(se, "could not start location tracking, disabling all automatic tracking");
            PreferencesUtil.disableAutomaticTracking(getApplicationContext());
            Toast.makeText(this, this.getString(R.string.locationPermissionsRemoved), Toast.LENGTH_LONG).show();
        }
        // note: API levels 21 - 25 don't display a notification while "tracking by location" is active
        locationTracker = new LocationTracker((LocationManager) getSystemService(Context.LOCATION_SERVICE), basics
            .getTimerManager(), basics.getExternalNotificationManager(), (AudioManager) getSystemService(Context.AUDIO_SERVICE),
            getApplicationContext());
        // restart if service crashed previously
        basics.safeCheckLocationBasedTracking();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // do nothing here as we don't bind the service to an activity
        return null;
    }

    // START_STICKY : Using this return value, if the OS kills our Service it will recreate it but the Intent that was
    // sent to the Service isn’t redelivered. In this way the Service is always running
    // START_NOT_STICKY: If the SO kills the Service it won’t recreate it until the client calls explicitly onStart
    // command
    // START_REDELIVER_INTENT: It is similar to the START_STICKY and in this case the Intent will be redelivered to the
    // service.
    @Override
    public int onStartCommand(Intent intent, int flags, @SuppressWarnings("hiding") int startId) {
        if (intent == null || intent.getExtras() == null) {
            // something went wrong, quit here
            return Service.START_NOT_STICKY;
        }

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

        if (result == Result.FAILURE_INSUFFICIENT_RIGHTS) {
            // disable the tracking and notify user of it
            basics.disableLocationBasedTracking();
            basics
                .showNotification(
                    getString(R.string.trackingByLocationErrorText),
                    getString(R.string.trackingByLocationErrorTitle),
                    getString(R.string.trackingByLocationErrorSubtitle),
                    basics
                        .createMessagePendingIntent(
                            getString(R.string.trackingByLocationErrorExplanation),
                            Constants.MISSING_PRIVILEGE_ACCESS_LOCATION_ID),
                    Constants.MISSING_PRIVILEGE_ACCESS_LOCATION_ID, false, null, null, null, null, null, null);
        } else if (result == Result.SUCCESS) {
            Boolean notificationActive = basics.isNotificationActive(Constants.MISSING_PRIVILEGE_ACCESS_LOCATION_ID);
            if (notificationActive == null || notificationActive) {
                basics.removeNotification(Constants.MISSING_PRIVILEGE_ACCESS_LOCATION_ID);
            }
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

    @Override
    public void onLowMemory() {
        Logger.info("low memory - LocationTrackerService");
        super.onLowMemory();
    }
}
