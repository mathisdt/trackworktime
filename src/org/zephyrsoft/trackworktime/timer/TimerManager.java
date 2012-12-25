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
package org.zephyrsoft.trackworktime.timer;

import hirondelle.date4j.DateTime;
import android.content.SharedPreferences;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * Manages the time tracking.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class TimerManager {
	
	private final DAO dao;
	private final SharedPreferences preferences;
	
	/**
	 * Constructor
	 */
	public TimerManager(DAO dao, SharedPreferences preferences) {
		this.dao = dao;
		this.preferences = preferences;
		
	}
	
	/**
	 * Checks the current state.
	 * 
	 * @return {@code true} if currently clocked in, {@code false} otherwise
	 */
	public boolean isTracking() {
		Event latestEvent = dao.getLatestEvent();
		return latestEvent == null ? false : latestEvent.getType().equals(TypeEnum.CLOCK_IN.getValue());
	}
	
	/**
	 * Returns the currently active task or {@code null} if tracking is disabled at the moment.
	 */
	public Task getCurrentTask() {
		Event latestEvent = dao.getLatestEvent();
		if (latestEvent != null && latestEvent.getType().equals(TypeEnum.CLOCK_IN.getValue())) {
			return dao.getTask(latestEvent.getTask());
		} else {
			return null;
		}
	}
	
	/**
	 * Either starts tracking (from non-tracked time) or changes the task and/or text (from already tracked time).
	 * 
	 * @param selectedTask the task for which the time shall be tracked
	 * @param text free text to describe in detail what was done
	 */
	public void startTracking(Task selectedTask, String text) {
		createEvent((selectedTask == null ? null : selectedTask.getId()), TypeEnum.CLOCK_IN, text);
	}
	
	/**
	 * Stops tracking time.
	 */
	public void stopTracking() {
		createEvent(null, TypeEnum.CLOCK_OUT, null);
	}
	
	/**
	 * Create a new event at current time.
	 * 
	 * @param taskId the task id (may be {@code null})
	 * @param type the type
	 * @param text the text (may be {@code null})
	 */
	public void createEvent(Integer taskId, TypeEnum type, String text) {
		DateTime now = DateTimeUtil.getCurrentDateTime();
		createEvent(now, taskId, type, text);
	}
	
	/**
	 * Create a new event at the given time.
	 * 
	 * @param dateTime the time for which the new event should be created
	 * @param taskId the task id (may be {@code null})
	 * @param type the type
	 * @param text the text (may be {@code null})
	 */
	public void createEvent(DateTime dateTime, Integer taskId, TypeEnum type, String text) {
		if (dateTime == null) {
			throw new IllegalArgumentException("date/time has to be given");
		}
		if (type == null) {
			throw new IllegalArgumentException("type has to be given");
		}
		String weekStart = DateTimeUtil.getWeekStart(dateTime);
		String time = DateTimeUtil.dateTimeToString(dateTime);
		Week currentWeek = dao.getWeek(weekStart);
		if (currentWeek == null) {
			currentWeek = dao.insertWeek(new Week(null, weekStart, 0));
		}
		
		if (type == TypeEnum.CLOCK_OUT) {
			tryToInsertAutoPause(dateTime);
		}
		
		Event event = new Event(null, currentWeek.getId(), taskId, type.getValue(), time, text);
		Logger.debug("TRACKING: " + type.name() + " @ " + time + " taskId=" + taskId + " text=" + text);
		event = dao.insertEvent(event);
		if (type == TypeEnum.CLOCK_OUT) {
			// TODO update this week's sum (and also the sum of the last week if clocked in overnight)
			
		}
	}
	
	private void tryToInsertAutoPause(DateTime dateTime) {
		if (isAutoPauseEnabled() && isAutoPauseApplicable(dateTime)) {
			// insert auto-pause events
			DateTime begin = getAutoPauseBegin(dateTime);
			DateTime end = getAutoPauseEnd(dateTime);
			Logger.debug("inserting auto-pause, begin={0}, end={1}", begin, end);
			createEvent(begin, null, TypeEnum.CLOCK_OUT, null);
			Event lastBeforePause = dao.getLastEventBefore(begin);
			createEvent(end, (lastBeforePause == null ? null : lastBeforePause.getTask()), TypeEnum.CLOCK_IN,
				(lastBeforePause == null ? null : lastBeforePause.getText()));
		} else {
			Logger.debug("NOT inserting auto-pause");
		}
	}
	
	/**
	 * Determines if the auto-pause mechanism is enabled.
	 */
	public boolean isAutoPauseEnabled() {
		return preferences.getBoolean(Key.AUTO_PAUSE_ENABLED.getName(), false);
	}
	
	/**
	 * Determines if the auto-pause can be applied to the given day.
	 */
	public boolean isAutoPauseApplicable(DateTime dateTime) {
		DateTime begin = getAutoPauseBegin(dateTime);
		DateTime end = getAutoPauseEnd(dateTime);
		if (begin.lt(end)) {
			Event lastEventBeforeBegin = dao.getLastEventBefore(begin);
			Event lastEventBeforeEnd = dao.getLastEventBefore(end);
			// is clocked in before begin
			return lastEventBeforeBegin != null && lastEventBeforeBegin.getType().equals(TypeEnum.CLOCK_IN.getValue())
			// no event is in auto-pause interval
				&& lastEventBeforeBegin.getId().equals(lastEventBeforeEnd.getId())
				// given time is after auto-pause end
				&& dateTime.gt(end);
		} else {
			// begin is equal to end or (even worse) begin is after end => no auto-pause
			return false;
		}
	}
	
	/**
	 * Calculates the begin of the auto-pause for the given day.
	 */
	public DateTime getAutoPauseBegin(DateTime dateTime) {
		return DateTimeUtil.parseTimeFor(dateTime, getAutoPauseData(Key.AUTO_PAUSE_BEGIN.getName(), "24.00"));
	}
	
	/**
	 * Calculates the end of the auto-pause for the given day.
	 */
	public DateTime getAutoPauseEnd(DateTime dateTime) {
		return DateTimeUtil.parseTimeFor(dateTime, getAutoPauseData(Key.AUTO_PAUSE_END.getName(), "00.00"));
	}
	
	private String getAutoPauseData(String key, String defaultTime) {
		String ret = preferences.getString(key, defaultTime);
		ret = DateTimeUtil.refineTime(ret);
		return ret;
	}
	
}
