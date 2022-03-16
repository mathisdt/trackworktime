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

import android.media.AudioManager;
import android.net.wifi.ScanResult;

import androidx.annotation.NonNull;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.Constants;
import org.zephyrsoft.trackworktime.WorkTimeTrackerActivity;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.ExternalNotificationManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enables the tracking of work time by presence at a specific wifi ssid. This is an addition to the manual tracking,
 * not a replacement: you can still clock in and out manually.
 */
public class WifiTracker implements WifiScanner.WifiScanListener {

	private final WifiScanner wifiScanner;
	private final TimerManager timerManager;
	private final ExternalNotificationManager externalNotificationManager;
	private final AudioManager audioManager;

	private final AtomicBoolean isTrackingByWifi = new AtomicBoolean(false);

	private String ssid = "";
	private Boolean vibrate = false;
	/** in minutes */
	private Integer checkInterval = 1;

	/** previous state of the wifi ssid occurence (from last check) */
	private Boolean ssidWasPreviouslyInRange;

	/**
	 * Creates a new wifi-based tracker. By only creating it, the tracking does not start yet - you have to call
	 * {@link #startTrackingByWifi(String, Boolean, Integer)} explicitly.
	 */
	public WifiTracker(TimerManager timerManager,
		ExternalNotificationManager externalNotificationManager,
		AudioManager audioManager, WifiScanner wifiScanner) {
		if (timerManager == null) {
			throw new IllegalArgumentException("the TimerManager is null");
		}
		if (externalNotificationManager == null) {
			throw new IllegalArgumentException("the ExternalNotificationManager is null");
		}
		if (audioManager == null) {
			throw new IllegalArgumentException("the AudioManager is null");
		}
		if (wifiScanner == null) {
			throw new IllegalArgumentException("the " + WifiScanner.class.getSimpleName() + " is null");
		}
		this.timerManager = timerManager;
		this.externalNotificationManager = externalNotificationManager;
		this.audioManager = audioManager;
		this.wifiScanner = wifiScanner;
	}

	/**
	 * Start the periodic checks to track by wifi.
	 */
	public Result startTrackingByWifi(@SuppressWarnings("hiding") String ssid,
		@SuppressWarnings("hiding") Boolean vibrate, Integer checkInterval) {

		Logger.debug("preparing wifi-based tracking");

		this.ssid = ssid;
		this.vibrate = vibrate;
		this.checkInterval = checkInterval;

		// just in case:
		stopTrackingByWifi();

		if (isTrackingByWifi.compareAndSet(false, true)) {
			try {
				timerManager.activateTrackingMethod(TrackingMethod.WIFI);
				int time = checkInterval * 60 - 30;
				wifiScanner.setMaxScanAge(time);
				wifiScanner.setScanRequestTimeout(time);
				wifiScanner.setWifiScanListener(this);
				Logger.info("started wifi-based tracking");
				return Result.SUCCESS;
			} catch (RuntimeException re) {
				Logger.info(re,"NOT started wifi-based tracking, insufficient privileges detected");
				isTrackingByWifi.set(false);
				return Result.FAILURE_INSUFFICIENT_RIGHTS;
			}
		} else {
			// should not happen as we call stopTrackingByWifi() above, but you never know...
			return Result.FAILURE_ALREADY_RUNNING;
		}
	}

	/**
	 * check if wifi ssid is in range and start/stop tracking
	 */
	public void checkWifi() {
		wifiScanner.requestWifiScanResults();
	}

	@Override
	public void onScanResultsUpdated(@NonNull List<ScanResult> wifiNetworksInRange) {
		Logger.debug("hecking wifi for ssid \"{}\"", ssid);
		final boolean ssidIsNowInRange = isConfiguredSsidInRange(wifiNetworksInRange);
		Logger.debug("wifi ssid \"{}\" in range now: {}, previous state: {}", ssid, ssidIsNowInRange,
				ssidWasPreviouslyInRange);

		if (ssidWasPreviouslyInRange != null && ssidWasPreviouslyInRange && !ssidIsNowInRange) {

			boolean globalStateChanged = timerManager.clockOutWithTrackingMethod(TrackingMethod.WIFI);
			if (globalStateChanged) {
				WorkTimeTrackerActivity.refreshViewIfShown();
				if (vibrate && isVibrationAllowed()) {
					tryVibration();
				}
				tryPebbleNotification("stopped tracking via WiFi");
				Logger.info("clocked out via wifi-based tracking");
			}
		} else if ((ssidWasPreviouslyInRange == null || !ssidWasPreviouslyInRange) && ssidIsNowInRange) {

			boolean globalStateChanged = timerManager.clockInWithTrackingMethod(TrackingMethod.WIFI);
			if (globalStateChanged) {
				WorkTimeTrackerActivity.refreshViewIfShown();
				if (vibrate && isVibrationAllowed()) {
					tryVibration();
				}
				tryPebbleNotification("started tracking via WiFi");
				Logger.info("clocked in via wifi-based tracking");
			}
		}
		// preserve the state of this wifi-check for the next call
		ssidWasPreviouslyInRange = ssidIsNowInRange;
	}

	@Override
	public void onScanRequestFailed(@NonNull WifiScanner.Result failCode) {
		switch (failCode) {
			case FAIL_WIFI_DISABLED:
				Logger.warn("tracking by wifi, but wifi-radio is disabled. Retaining previous tracking state");
				break;
			case FAIL_SCAN_REQUEST_FAILED:
				Logger.info("wifi scan request failed, skipping wifi check - retaining previous tracking state");
				break;
			case FAIL_RESULTS_NOT_UPDATED:
				Logger.info("wifi scan results were not updated, skipping wifi check - retaining previous tracking state");
				break;
			case CANCEL_SPAMMING:
				Logger.warn("wifi scan request canceled, due to too much requests");
				break;
			default:
				throw new UnsupportedOperationException("Unhandled wifi scan result code");
		}
	}

	/**
	 * look for networks in range if wifi-radio is activated
	 * 
	 * @return found, false in case of deactived wifi-radio
	 */
	private boolean isConfiguredSsidInRange(@NonNull List<ScanResult> wifiNetworksInRange) {
		if(wifiNetworksInRange.isEmpty()) {
			Logger.info("tracking by wifi, but wifi network list is empty");
			return false;
		}

		for (ScanResult network : wifiNetworksInRange) {
			if (network.SSID.equalsIgnoreCase(ssid)) {
				return true;
			}
		}

		Logger.info("tracking by wifi, but specified wifi name \"{}\" not found in {} available wifi networks", ssid, wifiNetworksInRange.size());
		return false;
	}

	private boolean isVibrationAllowed() {
		return audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT;
	}

	private void tryVibration() {
		try {
			externalNotificationManager.vibrate(Constants.VIBRATION_PATTERN);
		} catch (RuntimeException re) {
			Logger.warn("vibration not allowed by permissions");
		}
	}

	private void tryPebbleNotification(String message) {
		try {
			externalNotificationManager.notifyPebble(message);
		} catch (Exception e) {
			Logger.warn("Pebble notification failed");
		}
	}

	/**
	 * Stop the periodic checks to track by wifi.
	 */
	public void stopTrackingByWifi() {
		timerManager.deactivateTrackingMethod(TrackingMethod.WIFI);
		wifiScanner.setWifiScanListener(null);

		if (isTrackingByWifi.compareAndSet(true, false)) {
			Logger.info("stopped wifi-based tracking");
			ssidWasPreviouslyInRange = null;
		}
	}

	/**
	 * Get the currently configured ssid.
	 */
	public String getSSID() {
		return ssid;
	}

	/**
	 * Get the vibration setting.
	 */
	public boolean shouldVibrate() {
		return vibrate;
	}

	public int getCheckInterval() {
		return checkInterval;
	}
}
