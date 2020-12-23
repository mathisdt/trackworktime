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
import android.os.Bundle;

import org.pmw.tinylog.Logger;
import org.threeten.bp.OffsetDateTime;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;

/**
 * Hook for clock-in with third-party apps like Tasker or Llama.
 * Also handles actions triggered directly from the notification of TWT.
 *
 * @author Mathis Dirksen-Thedens
 */
public class ThirdPartyReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Bundle extras = intent.getExtras();

		if (action != null && action.equals(Constants.CLOCK_IN_ACTION)) {
			Integer taskId = getTaskId(context, extras);
			String text = getText(extras);
			Logger.info("TRACKING: clock-in via broadcast / taskId={} / text={}", taskId, text);
			Basics.getOrCreateInstance(context).getTimerManager().createEvent(OffsetDateTime.now(),
				taskId, TypeEnum.CLOCK_IN, text);
			WorkTimeTrackerActivity instanceOrNull = WorkTimeTrackerActivity.getInstanceOrNull();
			if (instanceOrNull != null) {
				instanceOrNull.refreshView();
			}
			Widget.dispatchUpdateIntent(context);
		} else if (action != null && action.equals(Constants.CLOCK_OUT_ACTION)) {
			Integer taskId = getTaskId(context, extras);
			String text = getText(extras);
			Logger.info("TRACKING: clock-out via broadcast / taskId={} / text={}", taskId, text);
			Basics.getOrCreateInstance(context).getTimerManager().createEvent(OffsetDateTime.now(),
				taskId, TypeEnum.CLOCK_OUT, text);
			WorkTimeTrackerActivity instanceOrNull = WorkTimeTrackerActivity.getInstanceOrNull();
			if (instanceOrNull != null) {
				instanceOrNull.refreshView();
			}
			Widget.dispatchUpdateIntent(context);
		} else {
			Logger.warn("TRACKING: unknown intent action");
		}
	}

	private static Integer getTaskId(Context context, Bundle extras) {
		if (extras == null) {
			return null;
		}
		int taskId = extras.getInt(Constants.INTENT_EXTRA_TASK, -1);
		String task = extras.getString(Constants.INTENT_EXTRA_TASK);
		if (taskId < 0 && task != null) {
			try {
				// try to extract the ID
				Integer parsedTaskId = Integer.parseInt(task);
				Task taskInstance = Basics.getOrCreateInstance(context).getDao().getTask(parsedTaskId);
				if (taskInstance != null && !taskInstance.getActive().equals(0)) {
					return parsedTaskId;
				}
			} catch (NumberFormatException nfe) {
				// apparently it isn't an ID, try to look up the task name
				Task taskInstance = Basics.getOrCreateInstance(context).getDao().getTask(task);
				if (taskInstance != null && !taskInstance.getActive().equals(0)) {
					return taskInstance.getId();
				}
			}
		} else if (taskId >= 0) {
			return taskId;
		}
		return null;
	}

	private static String getText(Bundle extras) {
		if (extras == null) {
			return null;
		}
		return extras.getString(Constants.INTENT_EXTRA_TEXT);
	}

}
