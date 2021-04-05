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
package org.zephyrsoft.trackworktime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.zephyrsoft.trackworktime.location.LocationTrackerService;

/**
 * Watchdog, e.g. for {@link LocationTrackerService}. It gets periodic intents scheduled by {@link Basics}.
 */
public class Watchdog extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Basics.getOrCreateInstance(context).periodicHook();
	}

}
