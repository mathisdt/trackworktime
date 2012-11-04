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

import java.util.concurrent.atomic.AtomicBoolean;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.IBinder;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.WorkTimeTrackerActivity;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * The background service providing the location-based tracking without having the app open. <br/>
 * <br/>
 * <b>ATTENTION: THIS SERVICE NEEDS AN INSTANCE OF {@link WorkTimeTrackerActivity} TO FUNCTION PROPERLY!</b>
 * 
 * @author Mathis Dirksen-Thedens
 */
public class LocationTrackerService extends Service {
	
	/** the key for the {@link Double} which determines the latitude in the intent's extras */
	public static String INTENT_EXTRA_LATITUDE = "LATITUDE";
	
	/** the key for the {@link Double} which determines the longitude in the intent's extras */
	public static String INTENT_EXTRA_LONGITUDE = "LONGITUDE";
	
	/** the key for the {@link Double} which determines the tolerance in the intent's extras */
	public static String INTENT_EXTRA_TOLERANCE = "TOLERANCE";
	
	private LocationTracker locationTracker = null;
	private int startId;
	
	private static AtomicBoolean isRunning = new AtomicBoolean(false);
	
	@Override
	public void onCreate() {
		Logger.info("creating LocationTrackerService");
		locationTracker =
			new LocationTracker((LocationManager) getSystemService(Context.LOCATION_SERVICE), Basics.getInstance()
				.getTimerManager());
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// do nothing here as we don't bind the service to an activity
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, @SuppressWarnings("hiding") int startId) {
		if (isRunning.compareAndSet(false, true)) {
			this.startId = startId;
			locationTracker.startTrackingByLocation((Double) intent.getExtras().get(INTENT_EXTRA_LATITUDE),
				(Double) intent.getExtras().get(INTENT_EXTRA_LONGITUDE),
				(Double) intent.getExtras().get(INTENT_EXTRA_TOLERANCE));
			return Service.START_REDELIVER_INTENT;
		} else {
			return Service.START_NOT_STICKY;
		}
	}
	
	@Override
	public void onDestroy() {
		Logger.info("destroying LocationTrackerService");
		locationTracker.stopTrackingByLocation();
		stopSelf(startId);
		isRunning.set(false);
	}
	
}
