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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import android.location.Location;
import android.location.LocationManager;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * Enables the tracking of work time by presence at a specific location. This is an addition to the manual tracking, not
 * a replacement: you can still clock in and out manually.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class LocationTracker {
	
	private final int SECONDS_TO_SLEEP_BETWEEN_CHECKS = 60;
	
	private final LocationManager locationManager;
	private final TimerManager timerManager;
	private ExecutorService executor;
	private Future<?> countDownFuture;
	private boolean isTrackingByLocation = false;
	
	private Location targetLocation;
	private double toleranceInMeters;
	
	/**
	 * Creates a new location-based tracker. By only creating it, the tracking does not start yet - you have to call
	 * {@link #startTrackingByLocation(double, double, double)} explicitly.
	 */
	public LocationTracker(LocationManager locationManager, TimerManager timerManager) {
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
		
		executor = Executors.newSingleThreadExecutor();
		Runnable checkLocationRunnable = new Runnable() {
			@Override
			public void run() {
				while (isTrackingByLocation) {
					Logger.info("location-based tracking: checking location");
					Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					Logger.info("last known location: latitude={} / longitude={} / accuracy={}",
						lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
						lastKnownLocation.getAccuracy());
					if (lastKnownLocation != null) {
						checkLocation(lastKnownLocation);
					}
					Logger.info("location-based tracking: starting to sleep");
					try {
						Thread.sleep(SECONDS_TO_SLEEP_BETWEEN_CHECKS * 1000);
					} catch (InterruptedException e) {
						// just continue, the while loop will take care of stopping
					}
				}
			}
		};
		
		if (executor.isShutdown()) {
			throw new IllegalStateException("background executor is already stopped");
		} else {
			isTrackingByLocation = true;
			countDownFuture = executor.submit(checkLocationRunnable);
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
		return location.distanceTo(targetLocation) + location.getAccuracy() <= toleranceInMeters;
	}
	
	/**
	 * Stop the periodic checks to track by location.
	 */
	public void stopTrackingByLocation() {
		isTrackingByLocation = false;
		if (countDownFuture != null) {
			countDownFuture.cancel(true);
			countDownFuture = null;
		}
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
		Logger.info("stopped location-based tracking");
	}
	
}
