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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * Enables the tracking of work time by presence at a specific location. This is an addition to the manual tracking, not
 * a replacement: you can still clock in and out manually.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class LocationTracker implements LocationListener {
	
	private final int SECONDS_TO_SLEEP_BETWEEN_CHECKS = 60;
	
	private final LocationManager locationManager;
	private final TimerManager timerManager;
	private ExecutorService executor;
	private Future<?> countDownFuture;
	private AtomicBoolean isTrackingByLocation = new AtomicBoolean(false);
	
	private Location targetLocation;
	private double toleranceInMeters;
	
	/**
	 * Creates a new location-based tracker. By only creating it, the tracking does not start yet - you have to call
	 * {@link #startTrackingByLocation(double, double, double)} explicitly.
	 */
	public LocationTracker(LocationManager locationManager, TimerManager timerManager) {
		if (locationManager == null) {
			throw new IllegalArgumentException("the LocationManager is null");
		}
		if (timerManager == null) {
			throw new IllegalArgumentException("the TimerManager is null");
		}
		this.locationManager = locationManager;
		this.timerManager = timerManager;
	}
	
	/**
	 * Start the periodic checks to track by location.
	 */
	public void startTrackingByLocation(double latitude, double longitude,
		@SuppressWarnings("hiding") double toleranceInMeters) {
		
		Logger.info("preparing location-based tracking");
		
		targetLocation = new Location("");
		targetLocation.setLatitude(latitude);
		targetLocation.setLongitude(longitude);
		this.toleranceInMeters = toleranceInMeters;
		
		// just in case:
		stopTrackingByLocation();
		
		if (isTrackingByLocation.compareAndSet(false, true)) {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 0, this);
			Logger.info("started location-based tracking");
		}
	}
	
	private void checkLocation(Location location) {
		boolean locationIsInRange = isInRange(location);
		if (locationIsInRange && !timerManager.isTracking()) {
			timerManager.startTracking(null, null);
			Logger.info("clocked in via location-based tracking");
		} else if (!locationIsInRange && timerManager.isTracking()) {
			timerManager.stopTracking();
			Logger.info("clocked out via location-based tracking");
		}
	}
	
	private boolean isInRange(Location location) {
		float distance = location.distanceTo(targetLocation);
		float actualTolerance = location.getAccuracy();
		Logger.info(
			"comparing: calculated distance={0,number} / actual tolerance={1,number} / allowed tolerance={2,number}",
			distance, actualTolerance, toleranceInMeters);
		return distance + actualTolerance <= toleranceInMeters;
	}
	
	/**
	 * Stop the periodic checks to track by location.
	 */
	public void stopTrackingByLocation() {
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
	
}
