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
package org.zephyrsoft.trackworktime.database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;

import org.pmw.tinylog.Logger;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.UpgradeActivity;
import org.zephyrsoft.trackworktime.backup.WorkTimeTrackerBackupManager;
import org.zephyrsoft.trackworktime.model.CalcCacheEntry;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.Target;
import org.zephyrsoft.trackworktime.model.TargetEnum;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.timer.TimerManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.CACHE;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.CACHE_DATE;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.CACHE_TARGET;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.CACHE_WORKED;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_ID;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_TASK;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_TEXT;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_TIME;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_TYPE;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_V1;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.EVENT_ZONE_OFFSET;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TARGET_TEXT;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TASK;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TASK_ACTIVE;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TASK_DEFAULT;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TASK_ID;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TASK_NAME;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TASK_ORDERING;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TARGET;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TARGET_ID;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TARGET_TIME;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TARGET_TYPE;
import static org.zephyrsoft.trackworktime.database.MySQLiteHelper.TARGET_VALUE;

/**
 * The data access object for structures from the app's SQLite database. The model consists of three main elements:
 * tasks (which are defined by the user and can be referenced when clocking in), events (which are generated when
 * clocking in or out and when changing task or text) and weeks (which are like a clip around events and also can
 * provide a sum so that not all events have to be read to calculate the flexi time).
 *
 * This class is thread safe.
 *
 * @author Mathis Dirksen-Thedens
 */
public class DAO {

	// TODO use prepared statements as described here: http://stackoverflow.com/questions/7255574

	private volatile SQLiteDatabase db;
	private final MySQLiteHelper dbHelper;
	private final Context context;
	private final WorkTimeTrackerBackupManager backupManager;

	private static final DateTimeFormatter LOCAL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	/**
	 * Constructor
	 */
	public DAO(Context context) {
		this.context = context;
		dbHelper = new MySQLiteHelper(context);
		backupManager = new WorkTimeTrackerBackupManager(context);
	}

	/**
	 * Open the underlying database. Implicitly called before any database operation.
	 */
	private synchronized void open() throws SQLException {
		if(db == null || !db.isOpen()) {
			db = dbHelper.getWritableDatabase();
		}
	}

	/**
	 * Close the underlying database.
	 */
	public synchronized void close() {
		dbHelper.close();
	}

	// =======================================================

	private static final String[] TASK_FIELDS = { TASK_ID, TASK_NAME, TASK_ACTIVE, TASK_ORDERING, TASK_DEFAULT };

	private Task cursorToTask(Cursor cursor) {
		Task task = new Task();
		task.setId(cursor.getInt(0));
		task.setName(cursor.getString(1));
		task.setActive(cursor.getInt(2));
		task.setOrdering(cursor.getInt(3));
		task.setIsDefault(cursor.getInt(4));
		return task;
	}

	private ContentValues taskToContentValues(Task task) {
		ContentValues ret = new ContentValues();
		ret.put(TASK_NAME, task.getName());
		ret.put(TASK_ACTIVE, task.getActive());
		ret.put(TASK_ORDERING, task.getOrdering());
		ret.put(TASK_DEFAULT, task.getIsDefault());
		return ret;
	}

	/**
	 * Insert a new task.
	 *
	 * @param task
	 *            the task to add
	 * @return the newly created task as read from the database (complete with ID)
	 */
	public synchronized Task insertTask(Task task) {
		open();
		ContentValues args = taskToContentValues(task);
		long insertId = db.insert(TASK, null, args);
		// now fetch the newly inserted row and return it as Task object
		List<Task> created = getTasksWithConstraint(TASK_ID + "=" + insertId);
		dataChanged();
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
	 * Get the default task.
	 *
	 * @return the default task or {@code null} (if no task was marked as default or if the default task is deactivated)
	 */
	public Task getDefaultTask() {
		List<Task> tasks = getTasksWithConstraint(TASK_ACTIVE + "!=0 AND " + TASK_DEFAULT + "!=0");
		return tasks.isEmpty() ? null : tasks.get(0);
	}

	/**
	 * Get the task with a specific ID.
	 *
	 * @param id
	 *            the ID
	 * @return the task or {@code null} if the specified ID does not exist
	 */
	public Task getTask(Integer id) {
		List<Task> tasks = getTasksWithConstraint(TASK_ID + "=" + id);
		return tasks.isEmpty() ? null : tasks.get(0);
	}

	/**
	 * Get the first task with a specific name.
	 *
	 * @param name
	 *            the name
	 * @return the task (first if more than one exist) or {@code null} if the specified name does not exist at all
	 */
	public Task getTask(String name) {
		List<Task> tasks = getTasksWithConstraint(TASK_NAME + "=\"" + name + "\"");
		return tasks.isEmpty() ? null : tasks.get(0);
	}

	/**
	 * Return if the task with the given ID is used in an event.
	 */
	public synchronized boolean isTaskUsed(Integer id) {
		Cursor cursor = db.query(EVENT, new String[] { "count(*)" }, EVENT_TASK + " = " + id, null,
			null, null, null, null);
		cursor.moveToFirst();
		int count = 0;
		if (!cursor.isAfterLast()) {
			count = cursor.getInt(0);
		}
		cursor.close();
		return count > 0;
	}

	private synchronized List<Task> getTasksWithConstraint(String constraint) {
		open();
		List<Task> ret = new ArrayList<>();
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
	 * @param task
	 *            the task to update - the ID has to be set!
	 * @return the task as newly read from the database
	 */
	public synchronized Task updateTask(Task task) {
		open();
		ContentValues args = taskToContentValues(task);
		db.update(TASK, args, TASK_ID + "=" + task.getId(), null);
		// now fetch the newly updated row and return it as Task object
		List<Task> updated = getTasksWithConstraint(TASK_ID + "=" + task.getId() + "");
		dataChanged();
		return updated.get(0);
	}

	/**
	 * Remove a task.
	 *
	 * @param task
	 *            the task to delete - the ID has to be set!
	 * @return {@code true} if successful, {@code false} if not
	 */
	public synchronized boolean deleteTask(Task task) {
		open();
		final boolean result = db.delete(TASK, TASK_ID + "=" + task.getId(), null) > 0;
		dataChanged();
		return result;
	}


	// =======================================================

	private static final String[] EVENT_FIELDS = { EVENT_ID, EVENT_TIME, EVENT_ZONE_OFFSET, EVENT_TYPE, EVENT_TASK, EVENT_TEXT };
	private static final String[] COUNT_FIELDS = { "count(*)" };
	private static final String[] MAX_EVENT_FIELDS = { EVENT_ID, "max(" + EVENT_TIME + ")", EVENT_ZONE_OFFSET, EVENT_TYPE,
		EVENT_TASK, EVENT_TEXT };

	private Event cursorToEvent(Cursor cursor) {
		Event event = new Event();
		event.setId(cursor.getInt(0));

		Instant instant = Instant.ofEpochSecond(cursor.getLong(1));
		ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(cursor.getInt(2) * 60);
		event.setDateTime(instant.atOffset(zoneOffset));

		event.setType(cursor.getInt(3));
		event.setTask(cursor.getInt(4));
		event.setText(cursor.getString(5));
		return event;
	}

	private ContentValues eventToContentValues(Event event) {
		ContentValues ret = new ContentValues();

		OffsetDateTime dateTime = event.getDateTime();
		ret.put(EVENT_TIME, dateTime.toEpochSecond());
		ret.put(EVENT_ZONE_OFFSET, dateTime.getOffset().getTotalSeconds() / 60);

		ret.put(EVENT_TYPE, event.getType());
		ret.put(EVENT_TASK, event.getTask());
		ret.put(EVENT_TEXT, event.getText());
		return ret;
	}

	/**
	 * Insert a new event.
	 *
	 * @param event
	 *            the event to add
	 * @return the newly created event as read from the database (complete with ID)
	 */
	public synchronized Event insertEvent(Event event) {
		open();
		ContentValues args = eventToContentValues(event);
		long insertId = db.insert(EVENT, null, args);
		// now fetch the newly created row and return it as Event object
		List<Event> created = getEventsWithConstraint(EVENT_ID + "=" + insertId);
		if (created.size() > 0) {
			dataChanged();
			return created.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Return all events - attention: this may be slow if many events exist!
	 */
	public List<Event> getAllEvents() {
		return getEventsWithConstraint(null);
	}

	/**
	 * Return the events that are in the specified time frame.
	 */
	public List<Event> getEvents(Instant beginOfTimeFrame, Instant endOfTimeFrame) {
		return getEventsWithConstraint(EVENT_TIME + " >= \"" + beginOfTimeFrame.getEpochSecond()
			+ "\" AND " + EVENT_TIME + " < \"" + endOfTimeFrame.getEpochSecond() + "\"");
	}

	/**
	 * Return all events in a certain week.
	 *
	 * @param week
	 *            the week in which the events are searched - the ID has to be set!
	 */
	public List<Event> getEventsInWeek(Week week, ZoneId zoneId) {
		// FIXME
		if (week == null) {
			return Collections.emptyList();
		} else {
			Instant instantA = week.getStart().atStartOfDay(zoneId).toInstant();
			Instant instantB = week.getEnd().atTime(LocalTime.MAX).atZone(zoneId).toInstant();

			return getEvents(instantA, instantB);
		}
	}

	/**
	 * Return all events on a certain day.
	 *
	 * @param date
	 *            the day on which the events are searched
	 */
	public List<Event> getEventsOnDay(ZonedDateTime date) {
		return getEventsOnDayBefore(date.with(LocalTime.MAX));
	}

	/**
	 * Return all events on a certain day before the given timestamp.
	 *
	 * @param date the day on which the events are searched
	 */
	public List<Event> getEventsOnDayBefore(ZonedDateTime date) {
		long instantA = date.with(LocalTime.MIN).toEpochSecond();
		long instantB = date.toEpochSecond();

		return getEventsWithConstraint(EVENT_TIME + " >= \"" + instantA +
			"\" AND " + EVENT_TIME + " < \"" + instantB + "\"");
	}

	/**
	 * Return all events on a certain day up to and including the given timestamp.
	 *
	 * @param date the day on which the events are searched
	 */
	public List<Event> getEventsOnDayUpTo(ZonedDateTime date) {
		long instantA = date.with(LocalTime.MIN).toEpochSecond();
		long instantB = date.toEpochSecond();

		return getEventsWithConstraint(EVENT_TIME + " >= \"" + instantA +
				"\" AND " + EVENT_TIME + "<= \"" + instantB + "\"");
	}

	/**
	 * Return all events on a certain day after the given timestamp.
	 *
	 * @param date the day on which the events are searched
	 */
	public List<Event> getEventsOnDayAfter(ZonedDateTime date) {
		long instantA = date.toEpochSecond();
		long instantB = date.with(LocalTime.MAX).toEpochSecond();


		return getEventsWithConstraint(EVENT_TIME + " > \"" + instantA +
				"\" AND " + EVENT_TIME + "<= \"" + instantB + "\"");
	}

	/**
	 * Fetch a specific event.
	 *
	 * @param id
	 *            the ID of the event
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
	 * @param dateTime
	 *            the date and time before which the event is searched
	 */
	public Event getLastEventBefore(OffsetDateTime dateTime) {
		List<Event> lastEvent = getEventsWithParameters(EVENT_FIELDS, EVENT_TIME + " < \""
			+ dateTime.toEpochSecond() + "\"", true, true);
		// if lastEvent is empty, then there is no such event in the database
		return lastEvent.isEmpty() ? null : lastEvent.get(0);
	}

	/**
	 * Return the last event before a certain date and time (including the hour and minute given!)
	 * or {@code null} if there is no such event.
	 *
	 * @param dateTime
	 *            the date and time before which the event is searched
	 */
	public Event getLastEventUpTo(OffsetDateTime dateTime) {
		List<Event> lastEvent = getEventsWithParameters(EVENT_FIELDS, EVENT_TIME + " <= \""
			+ dateTime.toEpochSecond() + "\"", true, true);
		// if lastEvent is empty, then there is no such event in the database
		return lastEvent.isEmpty() ? null : lastEvent.get(0);
	}

	/**
	 * Return the first event after a certain date and time or {@code null} if there is no such event.
	 *
	 * @param dateTime
	 *            the date and time after which the event is searched
	 */
	public Event getFirstEventAfter(OffsetDateTime dateTime) {
		List<Event> firstEvent = getEventsWithParameters(EVENT_FIELDS, EVENT_TIME + " > \""
			+ dateTime.toEpochSecond() + "\"", false, true);
		// if firstEvent is empty, then there is no such event in the database
		return firstEvent.isEmpty() ? null : firstEvent.get(0);
	}

	/**
	 * Return the first event with the given type on a certain day after a certain time
	 * or {@code null} if there is no such event.
	 *
	 * @param dateTime
	 *            the date and time after which the event is searched
	 */
	public Event getFirstEventAfterWithType(ZonedDateTime dateTime, TypeEnum type) {
		long instantA = dateTime.toEpochSecond();

		String constraint =
				EVENT_TIME + " > \"" + instantA + "\" AND " + EVENT_TYPE + " = " + type.toString();

		List<Event> firstEvent = getEventsWithParameters(EVENT_FIELDS, constraint, false, true);

		// if firstEvent is empty, then there is no such event in the database
		return firstEvent.isEmpty() ? null : firstEvent.get(0);
	}

	/**
	 * Return the first recorded event or {@code null} if no event exists.
	 */
	public Event getFirstEvent() {
		List<Event> firstEvent = getEventsWithParameters(EVENT_FIELDS, null, false, true);
		// if firstEvent is empty, then there is no event in the database
		return firstEvent.isEmpty() ? null : firstEvent.get(0);
	}

	/**
	 * Return the last recorded event or {@code null} if no event exists.
	 */
	public Event getLatestEvent() {
		List<Event> latestEvent = getEventsWithParameters(MAX_EVENT_FIELDS, null, false, true);
		// if latestEvent is empty, then there is no event in the database
		return latestEvent.isEmpty() ? null : latestEvent.get(0);
	}

	private synchronized List<Event> getEventsWithParameters(String[] fields, String constraint,
			boolean descending,
		boolean limitedToOne) {
		open();
		List<Event> ret = new ArrayList<>();
		Cursor cursor = db.query(EVENT, fields, constraint, null, null, null, EVENT_TIME + (descending ? " desc" : "")
			+ "," + EVENT_ID + (descending ? " desc" : ""), (limitedToOne ? "1" : null));
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
	 * @param event
	 *            the event to update - the ID has to be set!
	 * @return the event as newly read from the database
	 */
	public synchronized Event updateEvent(Event event) {
		open();
		ContentValues args = eventToContentValues(event);
		db.update(EVENT, args, EVENT_ID + "=" + event.getId(), null);
		// now fetch the newly updated row and return it as Event object
		List<Event> updated = getEventsWithConstraint(EVENT_ID + "=" + event.getId());
		dataChanged();
		return updated.get(0);
	}

	/**
	 * Remove an event.
	 *
	 * @param event
	 *            the event to delete - the ID has to be set!
	 * @return {@code true} if successful, {@code false} if not
	 */
	public synchronized boolean deleteEvent(Event event) {
		open();
		final boolean result = db.delete(EVENT, EVENT_ID + "=" + event.getId(), null) > 0;
		dataChanged();
		return result;
	}

	private synchronized boolean deleteAll() {
		open();
		boolean result = db.delete(TASK, null, null) > 0;
		result |= db.delete(EVENT, null, null) > 0;
		dataChanged();
		return result;
	}

	public synchronized Cursor getAllEventsAndTasks() {
		open();
		final String querySelectPart = "SELECT"
			+ " " + MySQLiteHelper.EVENT + "." + MySQLiteHelper.EVENT_ID + " AS eventId"
			+ ", " + MySQLiteHelper.EVENT_TYPE
			+ ", " + MySQLiteHelper.EVENT_TIME
			+ ", " + MySQLiteHelper.EVENT_ZONE_OFFSET
			+ ", " + MySQLiteHelper.EVENT_TASK
			+ ", " + MySQLiteHelper.EVENT_TEXT
			+ ", " + MySQLiteHelper.TASK + "." + MySQLiteHelper.TASK_ID + " AS taskId"
			+ ", " + MySQLiteHelper.TASK_NAME
			+ ", " + MySQLiteHelper.TASK_ACTIVE
			+ ", " + MySQLiteHelper.TASK_ORDERING
			+ ", " + MySQLiteHelper.TASK_DEFAULT;

		// this is a FULL OUTER JOIN.
		// see http://stackoverflow.com/questions/1923259/full-outer-join-with-sqlite
		final String query = ""
			+ querySelectPart
			+ " FROM"
			+ " " + MySQLiteHelper.EVENT
			+ " LEFT JOIN"
			+ " " + MySQLiteHelper.TASK
			+ " ON "
			+ " taskId = " + MySQLiteHelper.EVENT_TASK

			+ " UNION ALL "

			+ querySelectPart
			+ " FROM"
			+ " " + MySQLiteHelper.TASK
			+ " LEFT JOIN"
			+ " " + MySQLiteHelper.EVENT
			+ " ON "
			+ " taskId = " + MySQLiteHelper.EVENT_TASK

			+ " WHERE"
			+ " eventId IS NULL"

			+ " ORDER BY"
			+ " eventId";

		return db.rawQuery(query, new String[] {});
	}

	// ---------------------------------------------------------------------------------------------
	// backup/restore methods for Google servers
	// ---------------------------------------------------------------------------------------------
	public long getLastDbModification() {
		final File dbFile = context.getDatabasePath(MySQLiteHelper.DATABASE_NAME);
		return dbFile.lastModified();
	}

	/**
	 * Called internally by the data base methods where data is changed.
	 */
	private void dataChanged() {
		backupManager.dataChanged();
	}

	// ---------------------------------------------------------------------------------------------
	// backup/restore methods
	// ---------------------------------------------------------------------------------------------
	public void backupEventsToWriter(final Writer writer) throws IOException {
		final String eol = System.getProperty("line.separator");
		final Cursor cur = getAllEventsAndTasks();
		cur.moveToFirst();

		final int eventIdCol = cur.getColumnIndex("eventId");
		final int eventTypeCol = cur.getColumnIndex(MySQLiteHelper.EVENT_TYPE);
		final int eventTimeCol = cur.getColumnIndex(MySQLiteHelper.EVENT_TIME);
		final int eventZoneCol = cur.getColumnIndex(MySQLiteHelper.EVENT_ZONE_OFFSET);
		final int eventTaskCol = cur.getColumnIndex(MySQLiteHelper.EVENT_TASK);
		final int eventTextCol = cur.getColumnIndex(MySQLiteHelper.EVENT_TEXT);

		final int taskIdCol = cur.getColumnIndex("taskId");
		final int taskNameCol = cur.getColumnIndex(MySQLiteHelper.TASK_NAME);
		final int taskActiveCol = cur.getColumnIndex(MySQLiteHelper.TASK_ACTIVE);
		final int taskOrderingCol = cur.getColumnIndex(MySQLiteHelper.TASK_ORDERING);
		final int taskDefaultCol = cur.getColumnIndex(MySQLiteHelper.TASK_DEFAULT);

		writer.write(
			MySQLiteHelper.EVENT_TYPE
				+ ";" + MySQLiteHelper.EVENT_TIME
				+ ";" + MySQLiteHelper.EVENT_TASK
				+ ";" + MySQLiteHelper.EVENT_TEXT
				+ ";taskId"
				+ ";" + MySQLiteHelper.TASK_NAME
				+ ";" + MySQLiteHelper.TASK_ACTIVE
				+ ";" + MySQLiteHelper.TASK_ORDERING
				+ ";" + MySQLiteHelper.TASK_DEFAULT
				+ eol);

		final StringBuilder buf = new StringBuilder();
		while (!cur.isAfterLast()) {
			if (!cur.isNull(eventIdCol)) {
				buf.append(TypeEnum.byValue(cur.getInt(eventTypeCol)).getReadableName());
				buf.append(";");

				Instant instant = Instant.ofEpochSecond(cur.getLong(eventTimeCol));
				ZoneOffset offset = ZoneOffset.ofTotalSeconds(cur.getInt(eventZoneCol) * 60);
				buf.append(instant.atOffset(offset).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
				buf.append(";");

				buf.append(cur.getInt(eventTaskCol));
				buf.append(";");

				String eventComment = cur.getString(eventTextCol);
				buf.append(eventComment == null ? "" : eventComment);
				buf.append(";");
			} else {
				// this is a task that has no events
				buf.append(";;;;");
			}
			if (!cur.isNull(taskIdCol)) {
				buf.append(cur.getInt(taskIdCol));
				buf.append(";");
				buf.append(cur.getString(taskNameCol));
				buf.append(";");
				buf.append(cur.getInt(taskActiveCol));
				buf.append(";");
				buf.append(cur.getInt(taskOrderingCol));
				buf.append(";");
				buf.append(cur.getInt(taskDefaultCol));
				buf.append(";");
			} else {
				// this is an event that has no task (TypeEnum.CLOCK_OUT)
				buf.append(";;;;;");
			}
			buf.append(eol);
			writer.write(buf.toString());
			buf.setLength(0);
			cur.moveToNext();
		}
		cur.close();
	}

	private static final int INDEX_EVENT_TYPE = 0;
	private static final int INDEX_EVENT_TIME = 1;
	private static final int INDEX_EVENT_TASK = 2;
	private static final int INDEX_EVENT_TEXT = 3;
	private static final int INDEX_TASK_ID = 4;
	private static final int INDEX_TASK_NAME = 5;
	private static final int INDEX_TASK_ACTIVE = 6;
	private static final int INDEX_TASK_ORDERING = 7;
	private static final int INDEX_TASK_DEFAULT = 8;

	public void restoreEventsFromReader(final BufferedReader reader) throws IOException {
        final String eol = System.getProperty("line.separator");
        final TimerManager timerManager = Basics.getInstance().getTimerManager();

        deleteAll();

		reader.readLine();	// skip header

        String line;
        // cache values
        final String clockInReadableName = TypeEnum.CLOCK_IN.getReadableName();
        final String clockOutNowReadableName = TypeEnum.CLOCK_OUT_NOW.getReadableName();
        while ((line = reader.readLine()) != null) {
            final String[] columns = line.split("[;\t]");
            try {
                if (columns.length > 8 && columns[INDEX_TASK_ID].length() > 0) {
                    final int taskId = Integer.parseInt(columns[INDEX_TASK_ID]);
                    if (getTask(taskId) == null) {
                        final Task task = new Task(
                                taskId,
                                columns[INDEX_TASK_NAME],
                                Integer.parseInt(columns[INDEX_TASK_ACTIVE]),
                                Integer.parseInt(columns[INDEX_TASK_ORDERING]),
                                Integer.parseInt(columns[INDEX_TASK_DEFAULT])
                        );
                        final ContentValues args = taskToContentValues(task);
                        args.put(MySQLiteHelper.TASK_ID, taskId);
                        db.insert(TASK, null, args);
                    }
                }

                if (columns.length > 2 && columns[INDEX_EVENT_TYPE].length() > 0) {
                	final OffsetDateTime dateTime = OffsetDateTime.parse(columns[INDEX_EVENT_TIME]);
                    final TypeEnum typeEnum;
                    if (clockInReadableName.equalsIgnoreCase(columns[INDEX_EVENT_TYPE])) {
                        typeEnum = TypeEnum.CLOCK_IN;
                    } else if (clockOutNowReadableName.equalsIgnoreCase(columns[INDEX_EVENT_TYPE])) {
                        typeEnum = TypeEnum.CLOCK_OUT_NOW;
                    } else {
                        typeEnum = TypeEnum.CLOCK_OUT;
                    }
                    timerManager.createEvent(dateTime,
                            Integer.parseInt(columns[INDEX_EVENT_TASK]),
                            typeEnum,
                            columns.length > INDEX_EVENT_TEXT ?
                                    columns[INDEX_EVENT_TEXT] : "", true);
                }
            } catch (NumberFormatException e) {
                // ignore rest of current row
            }
        }
	}


	// =======================================================

	private static final String[] TARGET_FIELDS = { TARGET_ID, TARGET_TIME, TARGET_TYPE, TARGET_VALUE, TARGET_TEXT };

	private Target cursorToTarget(Cursor cursor) {
		Target target = new Target();
		target.setId(cursor.getInt(0));
		target.setDate(LocalDate.ofEpochDay(cursor.getLong(1)));
		target.setType(cursor.getInt(2));
		target.setValue(cursor.getInt(3));
		target.setComment(cursor.getString(4));
		return target;
	}

	private ContentValues targetToContentValues(Target target) {
		ContentValues ret = new ContentValues();
		ret.put(TARGET_TIME, target.getDate().toEpochDay());
		ret.put(TARGET_TYPE, target.getType());
		ret.put(TARGET_VALUE, target.getValue());
		ret.put(TARGET_TEXT, target.getComment());
		return ret;
	}


	/**
	 * Insert a new target.
	 *
	 * @param target the target to add
	 * @return the newly created target as read from the database (complete with ID)
	 */
	public synchronized Target insertTarget(Target target) {
		open();
		ContentValues args = targetToContentValues(target);
		long insertId = db.insert(TARGET, null, args);

		// now fetch the newly created row and return it as Target object
		List<Target> created = getTargetsWithConstraint(TARGET_ID + "=" + insertId);
		if (created.size() > 0) {
			//dataChanged();    // TODO
			return created.get(0);
		} else {
			return null;
		}
	}

	private List<Target> getTargetsWithConstraint(String constraint) {
		open();
		List<Target> ret = new ArrayList<>();

		Cursor cursor = db.query(TARGET, TARGET_FIELDS, constraint, null, null, null, TARGET_TIME);
		cursor.moveToFirst();

		while (!cursor.isAfterLast()) {
			Target target = cursorToTarget(cursor);
			ret.add(target);
			cursor.moveToNext();
		}
		cursor.close();

		return ret;
	}

	public Target getDayTarget(LocalDate day) {
		List<Target> ret = getTargetsWithConstraint(TARGET_TIME + "=" + day.toEpochDay() +
				" AND " + TARGET_TYPE + "<=" + TargetEnum.DAY_IGNORE.getValue());

		if (ret.size() == 1) {
			Logger.debug("Got day target: " + ret.get(0));

			return ret.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Update an target.
	 *
	 * @param target the target to update - the ID has to be set!
	 * @return the target as newly read from the database
	 */
	public synchronized Target updateTarget(Target target) {
		open();
		ContentValues args = targetToContentValues(target);
		db.update(TARGET, args, TARGET_ID + "=" + target.getId(), null);

		// now fetch the newly updated row and return it as Target object
		List<Target> updated = getTargetsWithConstraint(TARGET_ID + "=" + target.getId());
		//dataChanged(); // TODO
		return updated.get(0);
	}

	/**
	 * Remove an target.
	 *
	 * @param target the target to delete - the ID has to be set!
	 * @return {@code true} if successful, {@code false} if not
	 */
	public boolean deleteTarget(Target target) {
		open();
		final boolean result = db.delete(TARGET, TARGET_ID + "=" + target.getId(), null) > 0;
		//dataChanged(); // TODO
		return result;
	}

	private synchronized boolean deleteAllTargets() {
		open();
		boolean result = db.delete(TARGET, null, null) > 0;
		dataChanged();
		return result;
	}

	public synchronized Cursor getAllTargets() {
		open();
		final String query = "SELECT"
				+ " "  + MySQLiteHelper.TARGET_TIME
				+ ", " + MySQLiteHelper.TARGET_TYPE
				+ ", " + MySQLiteHelper.TARGET_VALUE
				+ ", " + MySQLiteHelper.TARGET_TEXT
				+ " FROM " + MySQLiteHelper.TARGET
				+ " ORDER BY " + MySQLiteHelper.TARGET_TIME;

		return db.rawQuery(query, new String[] {});
	}


	// ---------------------------------------------------------------------------------------------
	// backup/restore methods
	// ---------------------------------------------------------------------------------------------
	public void backupTargetsToWriter(final Writer writer) throws IOException {
		final String eol = System.getProperty("line.separator");
		final Cursor cur = getAllTargets();
		cur.moveToFirst();

		final int targetTimeCol = cur.getColumnIndex(MySQLiteHelper.TARGET_TIME);
		final int targetTypeCol = cur.getColumnIndex(MySQLiteHelper.TARGET_TYPE);
		final int targetValueCol = cur.getColumnIndex(MySQLiteHelper.TARGET_VALUE);
		final int targetTextCol = cur.getColumnIndex(MySQLiteHelper.TARGET_TEXT);

		writer.write(
				MySQLiteHelper.TARGET_TIME
						+ ";" + MySQLiteHelper.TARGET_TYPE
						+ ";" + MySQLiteHelper.TARGET_VALUE
						+ ";" + MySQLiteHelper.TARGET_TEXT
						+ eol);

		final StringBuilder buf = new StringBuilder();
		while (!cur.isAfterLast()) {
			LocalDate date = LocalDate.ofEpochDay(cur.getLong(targetTimeCol));
			buf.append(date.format(DateTimeFormatter.ISO_DATE));
			buf.append(";");

			buf.append(TargetEnum.byValue(cur.getInt(targetTypeCol)).toString());
			buf.append(";");

			buf.append(cur.getInt(targetValueCol));
			buf.append(";");

			String targetComment = cur.getString(targetTextCol);
			buf.append(targetComment == null ? "" : targetComment);
			buf.append(";");
			buf.append(eol);

			writer.write(buf.toString());
			buf.setLength(0);
			cur.moveToNext();
		}
		cur.close();
	}

	private static final int INDEX_TARGET_TIME = 0;
	private static final int INDEX_TARGET_TYPE = 1;
	private static final int INDEX_TARGET_VALUE = 2;
	private static final int INDEX_TARGET_TEXT = 3;

	public void restoreTargetsFromReader(final BufferedReader reader) throws IOException {
		deleteAllTargets();

		reader.readLine();	// skip header
		String line;
		while ((line = reader.readLine()) != null) {
			final String[] columns = line.split("[;\t]");

			try {
				if (columns.length >= 4 && columns[INDEX_TARGET_TIME].length() > 0) {

					final Target target = new Target(0,
							TargetEnum.byName(columns[INDEX_TARGET_TYPE]).getValue(),
							Integer.parseInt(columns[INDEX_TARGET_VALUE]),
							LocalDate.parse(columns[INDEX_TARGET_TIME]),
							columns[INDEX_TARGET_TEXT]
					);
					final ContentValues args = targetToContentValues(target);
					db.insert(TARGET, null, args);
				}
			} catch (NumberFormatException e) {
				// ignore rest of current row
			}
		}
	}


	// =======================================================

	private static final String[] CACHE_FIELDS = {CACHE_DATE, CACHE_WORKED, CACHE_TARGET};

	private CalcCacheEntry cursorToCache(Cursor cursor) {
		CalcCacheEntry cache = new CalcCacheEntry();
		cache.setDateFromId(cursor.getLong(0));
		cache.setWorked(cursor.getLong(1));
		cache.setTarget(cursor.getLong(2));
		return cache;
	}

	private ContentValues cacheToContentValues(CalcCacheEntry cache) {
		ContentValues ret = new ContentValues();
		ret.put(CACHE_DATE, cache.getDateAsId());
		ret.put(CACHE_WORKED, cache.getWorked());
		ret.put(CACHE_TARGET, cache.getTarget());
		return ret;
	}


	/**
	 * Insert a new target.
	 *
	 * @param cache the cache entry to add
	 * @return the newly created cache entry as read from the database (complete with ID)
	 */
	public synchronized CalcCacheEntry insertCache(CalcCacheEntry cache) {
		open();
		ContentValues args = cacheToContentValues(cache);
		long insertId = db.insert(CACHE, null, args);

		// now fetch the newly created row and return it as Target object
		return getCacheWithConstraint(CACHE_DATE + "=" + insertId);
	}


	private CalcCacheEntry getCacheWithConstraint(String constraint) {
		open();

		CalcCacheEntry ret = null;

		Cursor cursor = db.query(CACHE, CACHE_FIELDS, constraint, null, null, null, CACHE_DATE + " DESC", "1");
		cursor.moveToFirst();

		if (!cursor.isAfterLast()) {
			ret = cursorToCache(cursor);
		}

		cursor.close();

		return ret;
	}

	public CalcCacheEntry getCacheAt(LocalDate date) {
		return getCacheWithConstraint(CACHE_DATE + "<=" + date.toEpochDay());
	}

	/**
	 * Remove cache entries.
	 *
	 * @param date the date from which to delete, deletes everything if {@code null}
	 * @return {@code true} if successful, {@code false} if not
	 */
	public boolean deleteCacheFrom(LocalDate date) {
		open();

		if (date != null) {
			return db.delete(CACHE, CACHE_DATE + ">=" + date.toEpochDay(), null) > 0;
		} else {
			return db.delete(CACHE, null, null) > 0;
		}
	}


	public void executePendingMigrations() {
		open();

		SQLiteStatement s = db.compileStatement("SELECT count(*) FROM sqlite_master WHERE type=\"table\" AND name=\"" + EVENT_V1 + "\"");

		if (s.simpleQueryForLong() > 0) {
			Logger.debug("Starting upgrade activity.");
			//close();

			Intent i = new Intent(context, UpgradeActivity.class);
			if (!(context instanceof Activity)) {
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			}
			context.startActivity(i);
		}
	}

	/*
	Migrate events to v2
	 */
	public void migrateEventsToV2(ZoneId zoneId, MigrationCallback callback) {
		new MigrateEventsV2(zoneId, callback).execute();
	}

	private class MigrateEventsV2 extends AsyncTask<Void, Integer, Void> {

		private final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS");
		private final int TYPE_FLEX = 2;

		private final ZoneId zoneId;
		private final WeakReference<MigrationCallback> callback;

		private MigrateEventsV2(ZoneId zoneId, MigrationCallback callback) {
			this.zoneId = zoneId;
			this.callback = new WeakReference<>(callback);
		}

		@Override
		protected Void doInBackground(Void... voids) {
			open();

			long numEntries = DatabaseUtils.queryNumEntries(db, EVENT_V1);
			long count = 0;

			Logger.debug("Migrating {} rows.", numEntries);

			db.beginTransaction();
			try {
				Cursor cursor = db.query(EVENT_V1, null, null, null,
						null, null, EVENT_TIME + "," + EVENT_ID, null);
				cursor.moveToFirst();

				while (!cursor.isAfterLast()) {
					publishProgress((int)((count * 100) / numEntries));

					int type = cursor.getInt(2);
					LocalDateTime eventDateTime =
							LocalDateTime.parse(cursor.getString(3), DATETIME_FORMAT);

					String text = cursor.getString(5);

					Integer task = null;

					if (type == TYPE_FLEX) {
						// migrate flex event to target table, time -> target work time for the day
						LocalTime localTime = eventDateTime.toLocalTime();
						long targetTime = localTime.getHour() * 60 + localTime.getMinute();

						ContentValues cv = new ContentValues();
						cv.put(TARGET_TIME, eventDateTime.toLocalDate().toEpochDay());
						cv.put(TARGET_TYPE,TargetEnum.DAY_SET.getValue());
						cv.put(TARGET_VALUE, targetTime);
						cv.put(TARGET_TEXT, text);

						db.insert(TARGET, null, cv);

					} else {
						if (!cursor.isNull(4)) {
							task = cursor.getInt(4);
						}

						// migrate local datetime assuming home time zone
						OffsetDateTime newTime = eventDateTime.atZone(zoneId).toOffsetDateTime();

						// entry in new event table
						ContentValues cv = new ContentValues();
						cv.put(EVENT_TIME, newTime.toInstant().getEpochSecond());
						cv.put(EVENT_ZONE_OFFSET, newTime.getOffset().getTotalSeconds() / 60);
						cv.put(EVENT_TYPE, type);
						cv.put(EVENT_TASK, task);
						cv.put(EVENT_TEXT, text);

						db.insert(EVENT, null, cv);
					}

					count++;
					cursor.moveToNext();
				}
				cursor.close();

				// for now only rename the old event table
				db.execSQL("ALTER TABLE " + EVENT_V1 + " RENAME TO " + EVENT_V1 + "_mig");

				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			MigrationCallback cb = callback.get();

			if (cb != null) {
				cb.onProgressUpdate(values[0]);
			}
		}

		@Override
		protected void onPostExecute(Void v) {
			Logger.debug("Migration done.");

			MigrationCallback cb = callback.get();
			if (cb != null) {
				cb.migrationDone();
			}
		}
	}
}
