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
package org.zephyrsoft.trackworktime.database;

import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_ID;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_TASK;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_TEXT;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_TIME;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_TYPE;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_WEEK;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TASK;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TASK_ACTIVE;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TASK_ID;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TASK_NAME;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TASK_ORDERING;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.WEEK;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.WEEK_ID;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.WEEK_START;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.WEEK_SUM;
import hirondelle.date4j.DateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekPlaceholder;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

/**
 * The data access object for structures from the app's SQLite database. The model consists of three main elements:
 * tasks (which are defined by the user and can be referenced when clocking in), events (which are generated when
 * clocking in or out and when changing task or text) and weeks (which are like a clip around events and also can
 * provide a sum so that not all events have to be read to calculate the flexi time).
 * 
 * @author Mathis Dirksen-Thedens
 */
public class DAO {
	
	// TODO use prepared statements as described here: http://stackoverflow.com/questions/7255574
	
	private SQLiteDatabase db;
	private MySQLiteHelper dbHelper;
	
	/**
	 * Constructor
	 */
	public DAO(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}
	
	/**
	 * Open the underlying database. Implicitly called before any database operation.
	 */
	private void open() throws SQLException {
		db = dbHelper.getWritableDatabase();
	}
	
	/**
	 * Close the underlying database.
	 */
	public void close() {
		dbHelper.close();
	}
	
	// =======================================================
	
	private static final String[] TASK_FIELDS = {TASK_ID, TASK_NAME, TASK_ACTIVE, TASK_ORDERING};
	
	private Task cursorToTask(Cursor cursor) {
		Task task = new Task();
		task.setId(cursor.getInt(0));
		task.setName(cursor.getString(1));
		task.setActive(cursor.getInt(2));
		task.setOrdering(cursor.getInt(3));
		return task;
	}
	
	private ContentValues taskToContentValues(Task task) {
		ContentValues ret = new ContentValues();
		ret.put(TASK_NAME, task.getName());
		ret.put(TASK_ACTIVE, task.getActive());
		ret.put(TASK_ORDERING, task.getOrdering());
		return ret;
	}
	
	/**
	 * Insert a new task.
	 * 
	 * @param task the task to add
	 * @return the newly created task as read from the database (complete with ID)
	 */
	public Task insertTask(Task task) {
		open();
		ContentValues args = taskToContentValues(task);
		long insertId = db.insert(TASK, null, args);
		// now fetch the newly inserted row and return it as Task object
		List<Task> created = getTasksWithConstraint(TASK_ID + "=" + insertId);
		return created.get(0);
	}
	
	/**
	 * Get all tasks.
	 * 
	 * @return all existing tasks
	 */
	public List<Task> getAllTasks() {
		return getTasksWithConstraint(null);
	}
	
	/**
	 * Get all active tasks.
	 * 
	 * @return all existing tasks that are active at the moment
	 */
	public List<Task> getActiveTasks() {
		return getTasksWithConstraint(TASK_ACTIVE + "!=0");
	}
	
	/**
	 * Get the task with a specific ID.
	 * 
	 * @param id the ID
	 * @return the task or {@code null} if the specified ID does not exist
	 */
	public Task getTask(Integer id) {
		List<Task> tasks = getTasksWithConstraint(TASK_ID + "=" + id);
		return tasks.isEmpty() ? null : tasks.get(0);
	}
	
	/**
	 * Return if the task with the given ID is used in an event.
	 */
	public boolean isTaskUsed(Integer id) {
		Cursor cursor =
			db.query(EVENT, new String[] {"count(*)"}, EVENT_TASK + " = " + String.valueOf(id), null, null, null, null,
				null);
		cursor.moveToFirst();
		int count = 0;
		if (!cursor.isAfterLast()) {
			count = cursor.getInt(0);
		}
		cursor.close();
		return count > 0;
	}
	
	private List<Task> getTasksWithConstraint(String constraint) {
		open();
		List<Task> ret = new ArrayList<Task>();
		// TODO sort tasks by TASK_ORDERING when the UI supports manual ordering of tasks
		Cursor cursor = db.query(TASK, TASK_FIELDS, constraint, null, null, null, TASK_NAME);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Task task = cursorToTask(cursor);
			ret.add(task);
			cursor.moveToNext();
		}
		cursor.close();
		return ret;
	}
	
	/**
	 * Update a task.
	 * 
	 * @param task the task to update - the ID has to be set!
	 * @return the task as newly read from the database
	 */
	public Task updateTask(Task task) {
		open();
		ContentValues args = taskToContentValues(task);
		db.update(TASK, args, TASK_ID + "=" + task.getId(), null);
		// now fetch the newly updated row and return it as Task object
		List<Task> updated = getTasksWithConstraint(TASK_ID + "=" + task.getId() + "");
		return updated.get(0);
	}
	
	/**
	 * Remove a task.
	 * 
	 * @param task the task to delete - the ID has to be set!
	 * @return {@code true} if successful, {@code false} if not
	 */
	public boolean deleteTask(Task task) {
		open();
		return db.delete(TASK, TASK_ID + "=" + task.getId(), null) > 0;
	}
	
	// =======================================================
	
	private static final String[] WEEK_FIELDS = {WEEK_ID, WEEK_START, WEEK_SUM};
	
	private Week cursorToWeek(Cursor cursor) {
		Week week = new Week();
		week.setId(cursor.getInt(0));
		week.setStart(cursor.getString(1));
		week.setSum(cursor.getInt(2));
		return week;
	}
	
	private ContentValues weekToContentValues(Week week) {
		ContentValues ret = new ContentValues();
		ret.put(WEEK_START, week.getStart());
		ret.put(WEEK_SUM, week.getSum());
		return ret;
	}
	
	/**
	 * Insert a new week.
	 * 
	 * @param week the week to add
	 * @return the newly created week as read from the database (complete with ID)
	 */
	public Week insertWeek(Week week) {
		open();
		ContentValues args = weekToContentValues(week);
		long insertId = db.insert(WEEK, null, args);
		// now fetch the newly inserted row and return it as Week object
		List<Week> created = getWeeksWithConstraint(WEEK_ID + "=" + insertId);
		return created.get(0);
	}
	
	/**
	 * Return all weeks.
	 */
	public List<Week> getAllWeeks() {
		return getWeeksWithConstraint(null);
	}
	
	/**
	 * Returns the week identified by the given start date or {@code null} if no week exists for that date.
	 * 
	 * @param start the start date
	 */
	public Week getWeek(String start) {
		List<Week> weeks = getWeeksWithConstraint(WEEK_START + "=\"" + start + "\"");
		return weeks.isEmpty() ? null : weeks.get(0);
	}
	
	/**
	 * Returns all weeks up to the given date. If "start" is a week start date, the week with that start date is also
	 * included in the result.
	 * 
	 * @param date the limiting date
	 */
	public List<Week> getWeeksUpTo(String date) {
		List<Week> weeks = getWeeksWithConstraint(WEEK_START + "<=\"" + date + "\"");
		return weeks;
	}
	
	/**
	 * Returns the week identified by the given ID or {@code null} if no week exists for that ID.
	 * 
	 * @param id the ID
	 */
	public Week getWeek(Integer id) {
		List<Week> weeks = getWeeksWithConstraint(WEEK_ID + "=" + id);
		return weeks.isEmpty() ? null : weeks.get(0);
	}
	
	private List<Week> getWeeksWithConstraint(String constraint) {
		open();
		List<Week> ret = new ArrayList<Week>();
		Cursor cursor = db.query(WEEK, WEEK_FIELDS, constraint, null, null, null, WEEK_START);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Week week = cursorToWeek(cursor);
			ret.add(week);
			cursor.moveToNext();
		}
		cursor.close();
		return ret;
	}
	
	/**
	 * Update a week.
	 * 
	 * @param week the week to update - the ID has to be set!
	 * @return the week as newly read from the database
	 */
	public Week updateWeek(Week week) {
		open();
		ContentValues args = weekToContentValues(week);
		db.update(WEEK, args, WEEK_ID + "=" + week.getId(), null);
		// now fetch the newly updated row and return it as Week object
		List<Week> updated = getWeeksWithConstraint(WEEK_ID + "=" + week.getId());
		return updated.get(0);
	}
	
	/**
	 * Remove a week.
	 * 
	 * @param week the week to delete - the ID has to be set!
	 * @return {@code true} if successful, {@code false} if not
	 */
	public boolean deleteWeek(Week week) {
		open();
		return db.delete(WEEK, WEEK_ID + "=" + week.getId(), null) > 0;
	}
	
	// =======================================================
	
	private static final String[] EVENT_FIELDS = {EVENT_ID, EVENT_WEEK, EVENT_TIME, EVENT_TYPE, EVENT_TASK, EVENT_TEXT};
	private static final String[] COUNT_FIELDS = {"count(*)"};
	private static final String[] MAX_EVENT_FIELDS = {EVENT_ID, EVENT_WEEK, "max(" + EVENT_TIME + ")", EVENT_TYPE,
		EVENT_TASK, EVENT_TEXT};
	
	private Event cursorToEvent(Cursor cursor) {
		Event event = new Event();
		event.setId(cursor.getInt(0));
		event.setWeek(cursor.getInt(1));
		event.setTime(cursor.getString(2));
		event.setType(cursor.getInt(3));
		event.setTask(cursor.getInt(4));
		event.setText(cursor.getString(5));
		return event;
	}
	
	private ContentValues eventToContentValues(Event event) {
		ContentValues ret = new ContentValues();
		ret.put(EVENT_WEEK, event.getWeek());
		ret.put(EVENT_TIME, event.getTime());
		ret.put(EVENT_TYPE, event.getType());
		ret.put(EVENT_TASK, event.getTask());
		ret.put(EVENT_TEXT, event.getText());
		return ret;
	}
	
	/**
	 * Insert a new event.
	 * 
	 * @param event the event to add
	 * @return the newly created event as read from the database (complete with ID)
	 */
	public Event insertEvent(Event event) {
		open();
		ContentValues args = eventToContentValues(event);
		long insertId = db.insert(EVENT, null, args);
		// now fetch the newly created row and return it as Event object
		List<Event> created = getEventsWithConstraint(EVENT_ID + "=" + insertId);
		return created.get(0);
	}
	
	/**
	 * Return all events - attention: this may be slow if many events exist!
	 */
	public List<Event> getAllEvents() {
		return getEventsWithConstraint(null);
	}
	
	/**
	 * Return all events in a certain week.
	 * 
	 * @param week the week in which the events are searched - the ID has to be set!
	 */
	public List<Event> getEventsInWeek(Week week) {
		if (week == null || week instanceof WeekPlaceholder) {
			return Collections.EMPTY_LIST;
		} else {
			return getEventsWithConstraint(EVENT_WEEK + "=" + week.getId());
		}
	}
	
	/**
	 * Return all events on a certain day.
	 * 
	 * @param day the day on which the events are searched
	 */
	public List<Event> getEventsOnDay(DateTime day) {
		return getEventsWithConstraint(EVENT_TIME + " like \"" + DateTimeUtil.dateTimeToDateString(day) + "%\"");
	}
	
	/**
	 * Fetch a specific event.
	 * 
	 * @param id the ID of the event
	 * @return the event, or {@code null} if the id does not exist
	 */
	public Event getEvent(Integer id) {
		List<Event> event = getEventsWithParameters(EVENT_FIELDS, EVENT_ID + " = " + id, false, true);
		// if event is empty, then there is no such event in the database
		return event.isEmpty() ? null : event.get(0);
	}
	
	/**
	 * Return the last event before a certain date and time or {@code null} if there is no such event.
	 * 
	 * @param dateTime the date and time before which the event is searched
	 */
	public Event getLastEventBefore(DateTime dateTime) {
		List<Event> lastEvent =
			getEventsWithParameters(EVENT_FIELDS,
				EVENT_TIME + " < \"" + DateTimeUtil.dateTimeToString(dateTime) + "\"", true, true);
		// if lastEvent is empty, then there is no such event in the database
		return lastEvent.isEmpty() ? null : lastEvent.get(0);
	}
	
	/**
	 * Return the first event after a certain date and time or {@code null} if there is no such event.
	 * 
	 * @param dateTime the date and time after which the event is searched
	 */
	public Event getFirstEventAfter(DateTime dateTime) {
		List<Event> firstEvent =
			getEventsWithParameters(EVENT_FIELDS,
				EVENT_TIME + " > \"" + DateTimeUtil.dateTimeToString(dateTime) + "\"", false, true);
		// if firstEvent is empty, then there is no such event in the database
		return firstEvent.isEmpty() ? null : firstEvent.get(0);
	}
	
	/**
	 * Return the last recorded event or {@code null} if no event exists.
	 */
	public Event getLatestEvent() {
		List<Event> latestEvent = getEventsWithParameters(MAX_EVENT_FIELDS, null, false, true);
		// if latestEvent is empty, then there is no event in the database
		Event event = latestEvent.isEmpty() ? null : latestEvent.get(0);
		return event;
	}
	
	private List<Event> getEventsWithParameters(String[] fields, String constraint, boolean descending,
		boolean limitedToOne) {
		open();
		List<Event> ret = new ArrayList<Event>();
		Cursor cursor =
			db.query(EVENT, fields, constraint, null, null, null, EVENT_TIME + (descending ? " desc" : "") + ","
				+ EVENT_ID + (descending ? " desc" : ""), (limitedToOne ? "1" : null));
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Event event = cursorToEvent(cursor);
			ret.add(event);
			cursor.moveToNext();
		}
		cursor.close();
		return ret;
	}
	
	private List<Event> getEventsWithConstraint(String constraint) {
		return getEventsWithParameters(EVENT_FIELDS, constraint, false, false);
	}
	
	/**
	 * Update an event.
	 * 
	 * @param event the event to update - the ID has to be set!
	 * @return the event as newly read from the database
	 */
	public Event updateEvent(Event event) {
		open();
		ContentValues args = eventToContentValues(event);
		db.update(EVENT, args, EVENT_ID + "=" + event.getId(), null);
		// now fetch the newly updated row and return it as Event object
		List<Event> updated = getEventsWithConstraint(EVENT_ID + "=" + event.getId());
		return updated.get(0);
	}
	
	/**
	 * Remove an event.
	 * 
	 * @param event the event to delete - the ID has to be set!
	 * @return {@code true} if successful, {@code false} if not
	 */
	public boolean deleteEvent(Event event) {
		open();
		return db.delete(EVENT, EVENT_ID + "=" + event.getId(), null) > 0;
	}
}
