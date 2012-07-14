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
import android.util.Log;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

/**
 * Manages the time tracking.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class TimerManager {
	
	private final DAO dao;
	
	/**
	 * Constructor
	 */
	public TimerManager(DAO dao) {
		this.dao = dao;
		
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
	 * Either starts tracking (from non-tracked time) or changes the task and/or text (from already tracked
	 * time).
	 * 
	 * @param selectedTask the task for which the time shall be tracked
	 * @param text free text to describe in detail what was done
	 */
	public void startTracking(Task selectedTask, String text) {
		count(selectedTask.getId(), TypeEnum.CLOCK_IN, text);
	}
	
	/**
	 * Stops tracking time.
	 */
	public void stopTracking() {
		count(null, TypeEnum.CLOCK_OUT, null);
	}
	
	private void count(Integer taskId, TypeEnum type, String text) {
		DateTime now = DateTimeUtil.getCurrentDateTime();
		String weekStart = DateTimeUtil.getWeekStart(now);
		String time = DateTimeUtil.dateTimeToString(now);
		Week currentWeek = dao.getWeek(weekStart);
		if (currentWeek == null) {
			currentWeek = dao.insertWeek(new Week(null, weekStart, 0));
		}
		Event event = new Event(null, currentWeek.getId(), taskId, type.getValue(), time, text);
		Log.d(getClass().getName(), "TRACKING: " + type.name() + " @ " + time + " taskId=" + taskId + " text=" + text);
		event = dao.insertEvent(event);
		if (type == TypeEnum.CLOCK_OUT) {
			// TODO update this week's sum (and perhaps also the sum of the last week when clocked in overnight)
			
		}
	}
	
}
