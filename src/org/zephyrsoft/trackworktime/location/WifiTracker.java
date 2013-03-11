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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.zephyrsoft.trackworktime.WorkTimeTrackerActivity;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.Logger;
import org.zephyrsoft.trackworktime.util.VibrationManager;
import android.media.AudioManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

/**
 * Enables the tracking of work time by presence at a specific wifi-ssid. This is an addition to the manual tracking,
 * not a replacement: you can still clock in and out manually.
 * 
 * @author Christoph Loewe
 */
public class WifiTracker {
	
	private final int SECONDS_TO_SLEEP_BETWEEN_CHECKS = 60;
	
	private final long[] vibrationPattern = {0, 200, 250, 500, 250, 200};
	
	private final WifiManager wifiManager;
	private final TimerManager timerManager;
	private final VibrationManager vibrationManager;
	private final AudioManager audioManager;
	
	private final AtomicBoolean isTrackingByWifi = new AtomicBoolean(false);
	
	private String ssid = "";
	private boolean vibrate = false;
	
	/**
	 * store-flag of the previous state of the wifi-ssid occurance from last check
	 */
	private Boolean ssidWasPreviouslyInRange;
	
	/**
	 * Creates a new wifi-based tracker. By only creating it, the tracking does not start yet - you have to call
	 * {@link #startTrackingByWifi(String, boolean)} explicitly.
	 */
	public WifiTracker(WifiManager wifiManager, TimerManager timerManager, VibrationManager vibrationManager,
		AudioManager audioManager) {
		if (wifiManager == null) {
			throw new IllegalArgumentException("the WifiManager is null");
		}
		if (timerManager == null) {
			throw new IllegalArgumentException("the TimerManager is null");
		}
		if (vibrationManager == null) {
			throw new IllegalArgumentException("the VibrationManager is null");
		}
		if (audioManager == null) {
			throw new IllegalArgumentException("the AudioManager is null");
		}
		this.wifiManager = wifiManager;
		this.timerManager = timerManager;
		this.vibrationManager = vibrationManager;
		this.audioManager = audioManager;
	}
	
	/**
	 * Start the periodic checks to track by wifi.
	 */
	public Result startTrackingByWifi(@SuppressWarnings("hiding") String ssid,
		@SuppressWarnings("hiding") boolean vibrate) {
		
		Logger.debug("preparing wifi-based tracking");
		
		this.ssid = ssid;
		this.vibrate = vibrate;
		
		// just in case:
		stopTrackingByWifi();
		
		if (isTrackingByWifi.compareAndSet(false, true)) {
			try {
				Logger.info("started wifi-based tracking");
				return Result.SUCCESS;
			} catch (RuntimeException re) {
				Logger.info("NOT started wifi-based tracking, insufficient privileges detected");
				isTrackingByWifi.set(false);
				return Result.FAILURE_INSUFFICIENT_RIGHTS;
			}
		} else {
			// should not happen as we call stopTrackingByWifi() above, but you never know...
			return Result.FAILURE_ALREADY_RUNNING;
		}
	}
	
	/**
	 * check if wifi-ssid is in range and start/stop tracking
	 */
	public void checkWifi() {
		Logger.debug("checking wifi for ssid \"{0}\"", ssid);
		final boolean ssidIsNowInRange = isConfiguredSsidInRange();
		Logger.debug("wifi-ssid \"{0}\" in now range: {1}, previous state: {2}", ssid, ssidIsNowInRange,
			ssidWasPreviouslyInRange);
		
		if ((ssidWasPreviouslyInRange == null || ssidWasPreviouslyInRange.booleanValue()) && timerManager.isTracking()
			&& !ssidIsNowInRange) {
			
			timerManager.stopTracking(0);
			WorkTimeTrackerActivity.refreshViewIfShown();
			if (vibrate && isVibrationAllowed()) {
				tryVibration();
			}
			Logger.info("clocked out via wifi-based tracking");
		} else if ((ssidWasPreviouslyInRange == null || !ssidWasPreviouslyInRange.booleanValue())
			&& !timerManager.isTracking() && ssidIsNowInRange) {
			
			timerManager.startTracking(0, null, null);
			WorkTimeTrackerActivity.refreshViewIfShown();
			if (vibrate && isVibrationAllowed()) {
				tryVibration();
			}
			Logger.info("clocked in via wifi-based tracking");
		}
		// preserve the state of this wifi-check for the next call
		ssidWasPreviouslyInRange = ssidIsNowInRange;
	}
	
	/**
	 * look for networks in range if wifi-radio is activated
	 * 
	 * @return found, false in case of deactived wifi-radio
	 */
	private boolean isConfiguredSsidInRange() {
		if (wifiManager.isWifiEnabled()) {
			List<ScanResult> wifiNetworksInRange = wifiManager.getScanResults();
			for (ScanResult network : wifiNetworksInRange) {
				if (network.SSID.equalsIgnoreCase(this.ssid)) {
					return true;
				}
			}
		} else {
			Logger.info("tracking by wifi, but wifi-radio is disabled");
		}
		return false;
	}
	
	private boolean isVibrationAllowed() {
		return audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT;
	}
	
	private void tryVibration() {
		try {
			vibrationManager.vibrate(vibrationPattern);
		} catch (RuntimeException re) {
			Logger.warn("vibration not allowed by permissions");
		}
	}
	
	/**
	 * Stop the periodic checks to track by wifi.
	 */
	public void stopTrackingByWifi() {
		
		if (isTrackingByWifi.compareAndSet(true, false)) {
			Logger.info("stopped wifi-based tracking");
			ssidWasPreviouslyInRange = null;
		}
	}
	
	/**
	 * Return the current ssid configured.
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
	
}
