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
 * Keeps accounts of tracked time.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class TimerManager {
	
	private final DAO dao;
	
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
