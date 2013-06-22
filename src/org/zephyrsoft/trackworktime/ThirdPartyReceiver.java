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
package org.zephyrsoft.trackworktime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * Hook for clock-in with third-party apps like Tasker or Llama.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class ThirdPartyReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action != null && action.equals("org.zephyrsoft.trackworktime.ClockIn")) {
			Logger.info("TRACKING: clock-in via broadcast");
			Basics.getOrCreateInstance(context).getTimerManager()
				.createEvent(DateTimeUtil.getCurrentDateTime(), null, TypeEnum.CLOCK_IN, null);
		} else if (action != null && action.equals("org.zephyrsoft.trackworktime.ClockOut")) {
			Logger.info("TRACKING: clock-out via broadcast");
			Basics.getOrCreateInstance(context).getTimerManager()
				.createEvent(DateTimeUtil.getCurrentDateTime(), null, TypeEnum.CLOCK_OUT, null);
		} else {
			Logger.warn("TRACKING: unknown intent action");
		}
	}
	
}
