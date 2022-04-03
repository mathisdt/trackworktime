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
import android.os.Bundle;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.timer.TimerManager;

import java.time.OffsetDateTime;

/**
 * Hook for clock-in with third-party apps like Tasker or Llama.
 * Also handles actions triggered directly from the notification and from the widget of TWT.
 */
public class ThirdPartyReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Bundle extras = intent.getExtras();

		TimerManager timerManager = Basics.getOrCreateInstance(context).getTimerManager();
		if (Constants.CLOCK_IN_ACTION.equals(action)) {
			Integer taskId = getTaskId(context, extras);
			if (taskId == null) {
				taskId = getDefaultTaskId(context);
			}

			String text = getText(extras);
			Logger.info("TRACKING: clock-in via broadcast / taskId={} / text={}", taskId, text);
			timerManager.createEvent(OffsetDateTime.now(),
				taskId, TypeEnum.CLOCK_IN, text);
			WorkTimeTrackerActivity instanceOrNull = WorkTimeTrackerActivity.getInstanceOrNull();
			if (instanceOrNull != null) {
				instanceOrNull.refreshView();
			}
			Widget.dispatchUpdateIntent(context);
		} else if (Constants.CLOCK_OUT_ACTION.equals(action)) {
			Integer taskId = getTaskId(context, extras);
			String text = getText(extras);
			Logger.info("TRACKING: clock-out via broadcast / taskId={} / text={}", taskId, text);
			timerManager.createEvent(OffsetDateTime.now(),
				taskId, TypeEnum.CLOCK_OUT, text);
			WorkTimeTrackerActivity instanceOrNull = WorkTimeTrackerActivity.getInstanceOrNull();
			if (instanceOrNull != null) {
				instanceOrNull.refreshView();
			}
			Widget.dispatchUpdateIntent(context);
		} else if (Constants.STATUS_REQUEST_ACTION.equals(action)) {
			String replyIntentName = getReplyIntent(extras);
			if (replyIntentName == null) {
				Logger.warn("no reply intent given, can't respond without it");
			} else {
				Intent replyIntent = new Intent(replyIntentName);

				replyIntent.putExtra(Constants.INTENT_EXTRA_REPLY_STATUS,
					timerManager.isTracking() ? "clocked-in" : "clocked-out");
				Task currentTask = timerManager.getCurrentTask();
				if (currentTask != null) {
					replyIntent.putExtra(Constants.INTENT_EXTRA_REPLY_CURRENT_TASK_NAME, currentTask.getName());
					replyIntent.putExtra(Constants.INTENT_EXTRA_REPLY_CURRENT_TASK_ID, currentTask.getId());
				}
				replyIntent.putExtra(Constants.INTENT_EXTRA_REPLY_MINUTES_REMAINING,
					timerManager.getMinutesRemaining());
				Logger.debug("sending status reply: {} {}", replyIntent.getAction(), replyIntent.getExtras());
				context.sendBroadcast(replyIntent);
			}
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

	// also used by ShortcutReceiver
	static Integer getDefaultTaskId(Context context) {
		DAO dao = Basics.getOrCreateInstance(context).getDao();
		Task task = dao.getDefaultTask();
		return task == null ? null : task.getId();
	}

	private static String getText(Bundle extras) {
		if (extras == null) {
			return null;
		}
		return extras.getString(Constants.INTENT_EXTRA_TEXT);
	}

	private static String getReplyIntent(Bundle extras) {
		if (extras == null) {
			return null;
		}
		return extras.getString(Constants.INTENT_EXTRA_REPLY_INTENT);
	}

}
