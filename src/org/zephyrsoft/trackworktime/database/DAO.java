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
import java.util.ArrayList;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.Week;

public class DAO {
	
	private SQLiteDatabase db;
	private MySQLiteHelper dbHelper;
	
	public DAO(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}
	
	public void open() throws SQLException {
		db = dbHelper.getWritableDatabase();
	}
	
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
	
	public Task insertTask(Task task) {
		ContentValues args = taskToContentValues(task);
		long insertId = db.insert(TASK, null, args);
		// now fetch the newly inserted row and return it as Task object
		List<Task> created = getTasksWithConstraint(TASK_ID + "=" + insertId);
		return created.get(0);
	}
	
	public List<Task> getAllTasks() {
		return getTasksWithConstraint(null);
	}
	
	public List<Task> getActiveTasks() {
		return getTasksWithConstraint(TASK_ACTIVE + "!=0");
	}
	
	public Task getTask(Integer id) {
		List<Task> tasks = getTasksWithConstraint(TASK_ID + "=" + id);
		return tasks.isEmpty() ? null : tasks.get(0);
	}
	
	private List<Task> getTasksWithConstraint(String constraint) {
		List<Task> ret = new ArrayList<Task>();
		// TODO sort tasks by TASK_ORDERING when we have UI support for manually ordering the tasks
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
	
	public Task updateTask(Task task) {
		ContentValues args = taskToContentValues(task);
		db.update(TASK, args, TASK_ID + "=" + task.getId(), null);
		// now fetch the newly updated row and return it as Task object
		List<Task> updated = getTasksWithConstraint(TASK_ID + "=" + task.getId() + "");
		return updated.get(0);
	}
	
	public boolean deleteTask(Task task) {
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
	
	public Week insertWeek(Week week) {
		ContentValues args = weekToContentValues(week);
		long insertId = db.insert(WEEK, null, args);
		// now fetch the newly inserted row and return it as Week object
		List<Week> created = getWeeksWithConstraint(WEEK_ID + "=" + insertId);
		return created.get(0);
	}
	
	public List<Week> getAllWeeks() {
		return getWeeksWithConstraint(null);
	}
	
	public Week getWeek(String start) {
		List<Week> weeks = getWeeksWithConstraint(WEEK_START + "=\"" + start + "\"");
		return weeks.isEmpty() ? null : weeks.get(0);
	}
	
	public Week getWeek(Integer id) {
		List<Week> weeks = getWeeksWithConstraint(WEEK_ID + "=" + id);
		return weeks.isEmpty() ? null : weeks.get(0);
	}
	
	private List<Week> getWeeksWithConstraint(String constraint) {
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
	
	public Week updateWeek(Week week) {
		ContentValues args = weekToContentValues(week);
		db.update(WEEK, args, WEEK_ID + "=" + week.getId(), null);
		// now fetch the newly updated row and return it as Week object
		List<Week> updated = getWeeksWithConstraint(WEEK_ID + "=" + week.getId());
		return updated.get(0);
	}
	
	public boolean deleteWeek(Week week) {
		return db.delete(WEEK, WEEK_ID + "=" + week.getId(), null) > 0;
	}
	
	// =======================================================
	
	private static final String[] EVENT_FIELDS = {EVENT_ID, EVENT_WEEK, EVENT_TIME, EVENT_TYPE, EVENT_TASK, EVENT_TEXT};
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
	
	public Event insertEvent(Event event) {
		ContentValues args = eventToContentValues(event);
		long insertId = db.insert(EVENT, null, args);
		// now fetch the newly created row and return it as Event object
		List<Event> created = getEventsWithConstraint(EVENT_ID + "=" + insertId);
		return created.get(0);
	}
	
	public List<Event> getAllEvents() {
		return getEventsWithConstraint(null);
	}
	
	public List<Event> getEventsInWeek(Week week) {
		return getEventsWithConstraint(EVENT_WEEK + "=" + week.getId());
	}
	
	public Event getLatestEvent() {
		List<Event> latestEvent = getEventsWithParameters(MAX_EVENT_FIELDS, null);
		// if latestEvent is empty, then there is no event in the database
		return latestEvent.isEmpty() ? null : latestEvent.get(0);
	}
	
	private List<Event> getEventsWithParameters(String[] fields, String constraint) {
		List<Event> ret = new ArrayList<Event>();
		Cursor cursor = db.query(EVENT, fields, constraint, null, null, null, EVENT_TIME);
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
		return getEventsWithParameters(EVENT_FIELDS, constraint);
	}
	
	public Event updateEvent(Event event) {
		ContentValues args = eventToContentValues(event);
		db.update(EVENT, args, EVENT_ID + "=" + event.getId(), null);
		// now fetch the newly updated row and return it as Event object
		List<Event> updated = getEventsWithConstraint(EVENT_ID + "=" + event.getId());
		return updated.get(0);
	}
	
	public boolean deleteEvent(Event event) {
		return db.delete(EVENT, EVENT_ID + "=" + event.getId(), null) > 0;
	}
}
