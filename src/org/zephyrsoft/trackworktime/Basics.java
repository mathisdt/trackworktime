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
package org.zephyrsoft.trackworktime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.location.LocationTrackerService;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * Creates the database connection on device boot and starts the location-based tracking service (if location-based
 * tracking is enabled).
 * 
 * @author Mathis Dirksen-Thedens
 */
public class Basics extends BroadcastReceiver {
	
	private Context context = null;
	private SharedPreferences preferences = null;
	private DAO dao = null;
	private TimerManager timerManager = null;
	
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
	
	/**
	 * Forwarding from {@link #onReceive(Context, Intent)}, but this method is only called on the singleton instance -
	 * no matter how many instances Android might choose to create of this class.
	 */
	public void receivedIntent(Context androidContext) {
		context = androidContext;
		init();
		checkLocationBasedTracking();
	}
	
	@Override
	public void onReceive(Context androidContext, Intent intent) {
		// always only use the one instance
		instance.receivedIntent(androidContext);
		
	}
	
	private void init() {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		dao = new DAO(context);
		dao.open();
		timerManager = new TimerManager(dao);
	}
	
	/**
	 * Check if location-based tracking has to be en- or disabled.
	 */
	public void checkLocationBasedTracking() {
		if (preferences.getBoolean("keyLocationBasedTrackingEnabled", false)) {
			String latitudeString = preferences.getString("keyLocationBasedTrackingLatitude", "0");
			String longitudeString = preferences.getString("keyLocationBasedTrackingLongitude", "0");
			String toleranceString = preferences.getString("keyLocationBasedTrackingTolerance", "0");
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
			Intent startIntent = buildServiceIntent(latitude, longitude, tolerance);
			context.startService(startIntent);
		} else {
			Intent stopIntent = buildServiceIntent(null, null, null);
			context.stopService(stopIntent);
		}
	}
	
	private Intent buildServiceIntent(Double latitude, Double longitude, Double tolerance) {
		Intent intent = new Intent(context, LocationTrackerService.class);
		intent.putExtra(LocationTrackerService.INTENT_EXTRA_LATITUDE, latitude);
		intent.putExtra(LocationTrackerService.INTENT_EXTRA_LONGITUDE, longitude);
		intent.putExtra(LocationTrackerService.INTENT_EXTRA_TOLERANCE, tolerance);
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
	
}
