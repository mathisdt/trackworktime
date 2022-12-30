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
package org.zephyrsoft.trackworktime.wear;

import android.app.Application;
import android.util.Log;

/**
 * Application entry point.
 */
public class WearApplication extends Application {

	public static final String TAG = "TRACKWORKTIME_WEAR";

	private boolean appOnPhoneClockedIn = false;

	public WearApplication() {
		Log.i(TAG, "instantiating Wear application");
	}

	public boolean isAppOnPhoneClockedIn() {
		return appOnPhoneClockedIn;
	}

	public void setAppOnPhoneClockedIn(boolean appOnPhoneClockedIn) {
		Log.d(WearApplication.TAG, "setting appOnPhoneClockedIn=" + appOnPhoneClockedIn);
		this.appOnPhoneClockedIn = appOnPhoneClockedIn;
	}

	@Override
	public void onCreate() {
		Log.i(TAG, "creating Wear application");
		super.onCreate();
	}

	@Override
	public void onTerminate() {
		Log.i(TAG, "terminating Wear application");
		super.onTerminate();
	}

	@Override
	public void onLowMemory() {
		Log.i(TAG, "low memory for Wear application");
		super.onLowMemory();
	}

}
