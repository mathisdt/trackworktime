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
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.Constants;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.options.Key;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The background service providing the wifi-based tracking without having the activity open.
 */
public class WifiTrackerService extends Service {

    private static WifiTracker wifiTracker = null;
    private int startId;

    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    private Basics basics = null;
    private WifiScanner wifiScanner;

    @Override
    public void onCreate() {
        Logger.info("creating WifiTrackerService");
        basics = Basics.getOrCreateInstance(getApplicationContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(Constants.PERSISTENT_TRACKING_ID, basics.createNotificationTracking());
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String checkIntervalString = basics.getPreferences().getString(Key.WIFI_BASED_TRACKING_CHECK_INTERVAL.getName(), "1");
        int checkInterval = checkIntervalString == null
            ? 1
            : Integer.parseInt(checkIntervalString);
        int time = checkInterval * 60 - 30;
        wifiScanner = new WifiScanner(wifiManager, time, time);
        wifiScanner.register(getApplicationContext());

        wifiTracker = new WifiTracker(
            basics.getTimerManager(),
            basics.getExternalNotificationManager(),
            (AudioManager) getSystemService(Context.AUDIO_SERVICE),
            wifiScanner, getApplicationContext());

        // restart if service crashed previously
        Basics.getOrCreateInstance(getApplicationContext()).safeCheckWifiBasedTracking();
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

        String ssid = (String) intent.getExtras().get(Constants.INTENT_EXTRA_SSID);
        Boolean vibrate = (Boolean) intent.getExtras().get(Constants.INTENT_EXTRA_VIBRATE);
        Integer checkInterval = (Integer) intent.getExtras().get(Constants.INTENT_EXTRA_WIFI_CHECK_INTERVAL);
        Result result = null;
        if (isRunning.compareAndSet(false, true)) {
            this.startId = startId;
            result = wifiTracker.startTrackingByWifi(ssid, vibrate, checkInterval);
            Logger.debug("started WifiTrackerService - ssid={} - vibrate={} - checkInterval={}",
                ssid, vibrate, checkInterval);
        } else if (!Objects.equals(ssid, wifiTracker.getSSID())
            || !Objects.equals(vibrate, wifiTracker.shouldVibrate())
            || !Objects.equals(checkInterval, wifiTracker.getCheckInterval())) {
            // already running, but the data has to be updated
            result = wifiTracker.startTrackingByWifi(ssid, vibrate, checkInterval);
            Logger.debug("re-started WifiTrackerService because of updated settings - ssid={} - vibrate={} - checkInterval={}",
                ssid, vibrate, checkInterval);
        } else {
            Logger.debug("WifiTrackerService is already running and nothing has to be updated - no action");
        }

        if (result == Result.FAILURE_INSUFFICIENT_RIGHTS) {
            // disable the tracking and notify user of it
            basics.disableWifiBasedTracking();
            basics.showNotification(
                getString(R.string.trackingByWifiErrorText),
                getString(R.string.trackingByWifiErrorTitle),
                getString(R.string.trackingByWifiErrorSubtitle),
                basics
                    .createMessagePendingIntent(
                        getString(R.string.trackingByWifiErrorExplanation),
                        Constants.MISSING_PRIVILEGE_ACCESS_WIFI_STATE_ID),
                Constants.MISSING_PRIVILEGE_ACCESS_WIFI_STATE_ID, false, null, null, null, null, null, null);
        } else if (result == Result.SUCCESS) {
            Boolean notificationActive = basics.isNotificationActive(Constants.MISSING_PRIVILEGE_ACCESS_WIFI_STATE_ID);
            if (notificationActive == null || notificationActive) {
                basics.removeNotification(Constants.MISSING_PRIVILEGE_ACCESS_WIFI_STATE_ID);
            }
        }

        // the check for available wifi networks is done here (and thus needs the periodically sent intents):
        checkWifiIfEnabled();

        return Service.START_NOT_STICKY;
    }

    private void checkWifiIfEnabled() {
        if (isRunning.get()) {
            wifiTracker.checkWifi();
        }
    }

    @Override
    public void onDestroy() {
        Logger.info("destroying WifiTrackerService");
        wifiTracker.stopTrackingByWifi();

        wifiScanner.unregister(getApplicationContext());
        wifiScanner.setWifiScanListener(null);

        isRunning.set(false);
        stopSelf();
    }

}
