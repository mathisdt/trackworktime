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

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import org.zephyrsoft.trackworktime.WorkTimeTrackerActivity;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.Logger;
import org.zephyrsoft.trackworktime.util.VibrationManager;

/**
 * Enables the tracking of work time by presence at a specific location. This is an addition to the manual tracking, not
 * a replacement: you can still clock in and out manually.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class LocationTracker implements LocationListener {
	
	private final int SECONDS_TO_SLEEP_BETWEEN_CHECKS = 60;
	
	private final long[] vibrationPattern = {0, 200, 250, 500, 250, 200};
	
	private final LocationManager locationManager;
	private final TimerManager timerManager;
	private final VibrationManager vibrationManager;
	private final AudioManager audioManager;
	private final AtomicBoolean isTrackingByLocation = new AtomicBoolean(false);
	
	private Location targetLocation;
	private double toleranceInMeters;
	private boolean vibrate = false;
	
	private Location previousLocation = null;
	
	/**
	 * Creates a new location-based tracker. By only creating it, the tracking does not start yet - you have to call
	 * {@link #startTrackingByLocation(double, double, double, boolean)} explicitly.
	 */
	public LocationTracker(LocationManager locationManager, TimerManager timerManager,
		VibrationManager vibrationManager, AudioManager audioManager) {
		if (locationManager == null) {
			throw new IllegalArgumentException("the LocationManager is null");
		}
		if (timerManager == null) {
			throw new IllegalArgumentException("the TimerManager is null");
		}
		if (vibrationManager == null) {
			throw new IllegalArgumentException("the VibrationManager is null");
		}
		this.locationManager = locationManager;
		this.timerManager = timerManager;
		this.vibrationManager = vibrationManager;
		this.audioManager = audioManager;
	}
	
	/**
	 * Start the periodic checks to track by location.
	 */
	public Result startTrackingByLocation(double latitude, double longitude,
		@SuppressWarnings("hiding") double toleranceInMeters, @SuppressWarnings("hiding") boolean vibrate) {
		
		Logger.debug("preparing location-based tracking");
		
		targetLocation = new Location("");
		targetLocation.setLatitude(latitude);
		targetLocation.setLongitude(longitude);
		this.toleranceInMeters = toleranceInMeters;
		this.vibrate = vibrate;
		
		// just in case:
		stopTrackingByLocation();
		
		if (isTrackingByLocation.compareAndSet(false, true)) {
			try {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 0, this);
				Logger.info("started location-based tracking");
				return Result.SUCCESS;
			} catch (RuntimeException re) {
				Logger.info("NOT started location-based tracking, insufficient privileges detected");
				isTrackingByLocation.set(false);
				return Result.FAILURE_INSUFFICIENT_RIGHTS;
			}
		} else {
			// should not happen as we call stopTrackingByLocation() above, but you never know...
			return Result.FAILURE_ALREADY_RUNNING;
		}
	}
	
	private void checkLocation(Location location) {
		Boolean previousLocationWasInRange =
			(previousLocation == null ? null : isInRange(previousLocation, "previous location"));
		boolean locationIsInRange = isInRange(location, "current location");
		if ((previousLocationWasInRange == null || !previousLocationWasInRange.booleanValue()) && locationIsInRange
			&& !timerManager.isTracking()) {
			timerManager.startTracking(0, null, null);
			WorkTimeTrackerActivity.refreshViewIfShown();
			if (vibrate && isVibrationAllowed()) {
				vibrationManager.vibrate(vibrationPattern);
			}
			Logger.info("clocked in via location-based tracking");
		} else if ((previousLocationWasInRange == null || previousLocationWasInRange.booleanValue())
			&& !locationIsInRange && timerManager.isTracking()) {
			timerManager.stopTracking(0);
			WorkTimeTrackerActivity.refreshViewIfShown();
			if (vibrate && isVibrationAllowed()) {
				vibrationManager.vibrate(vibrationPattern);
			}
			Logger.info("clocked out via location-based tracking");
		}
	}
	
	private boolean isVibrationAllowed() {
		return audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT;
	}
	
	private boolean isInRange(Location location, String descriptionForLog) {
		float distance = location.distanceTo(targetLocation);
		// round to whole meters
		distance = (float) Math.floor(distance);
		float actualTolerance = location.getAccuracy();
		Logger
			.info(
				"comparing"
					+ (descriptionForLog != null ? " " + descriptionForLog : "")
					+ ": calculated distance={0,number} / complete tolerance={1,number} (composed by actual position tolerance={2,number} + allowed tolerance={3,number})",
				distance, actualTolerance + toleranceInMeters, actualTolerance, toleranceInMeters);
		return distance <= toleranceInMeters + actualTolerance;
	}
	
	/**
	 * Stop the periodic checks to track by location.
	 */
	public void stopTrackingByLocation() {
		// execute this anyway to prevent confusion, e.g. after crashes:
		locationManager.removeUpdates(this);
		
		if (isTrackingByLocation.compareAndSet(true, false)) {
			Logger.info("stopped location-based tracking");
		}
	}
	
	@Override
	public void onLocationChanged(Location location) {
		if (location != null) {
			Date recordedTime = new Date(location.getTime());
			Logger
				.info(
					"location: latitude={0,number,#.######} / longitude={1,number,#.######} / accuracy={2,number} / recorded on {3,date} at {3,time} UTC",
					location.getLatitude(), location.getLongitude(), location.getAccuracy(), recordedTime);
			checkLocation(location);
			previousLocation = location;
		} else {
			Logger.info("last known location is null");
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
		// nothing to do
	}
	
	/**
	 * Return the current target's latitude.
	 */
	public Double getLatitude() {
		return (targetLocation == null ? null : targetLocation.getLatitude());
	}
	
	/**
	 * Return the current target's longitude.
	 */
	public Double getLongitude() {
		return (targetLocation == null ? null : targetLocation.getLongitude());
	}
	
	/**
	 * Return the current tolerance.
	 */
	public Double getTolerance() {
		return toleranceInMeters;
	}
	
	/**
	 * Get the vibration setting.
	 */
	public boolean shouldVibrate() {
		return vibrate;
	}
	
}
