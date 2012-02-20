package org.zephyrsoft.worktimetracker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter {
	
	private static final String TASK = "task";
	private static final String TASK_ID = "_id";
	public static final String TASK_NAME = "name";
	private static final String TASK_ACTIVE = "active"; // 0=false, 1=true
	
	private static final String WEEK = "week";
	private static final String WEEK_ID = "_id";
	private static final String WEEK_START = "start"; // date and time of monday 0:00 AM
	private static final String WEEK_SUM = "sum"; // in whole minutes
	
	private static final String EVENT = "event";
	private static final String EVENT_ID = "_id";
	private static final String EVENT_WEEK = "week"; // reference to WEEK_ID
	private static final String EVENT_TYPE = "type";
	private static final String EVENT_TIME = "time";
	private static final String EVENT_TASK = "task"; // reference to TASK_ID
	
	private static final int DATABASE_VERSION = 3;
	private static final String DATABASE_CREATE = "create table " + TASK + " (" + TASK_ID
		+ " integer primary key autoincrement, " + TASK_NAME + " text not null, " + TASK_ACTIVE
		+ " integer not null); create table " + WEEK + " (" + WEEK_ID + " integer primary key autoincrement, "
		+ WEEK_START + " text not null, " + WEEK_SUM + " integer null); create table " + EVENT + " (" + EVENT_ID
		+ " integer primary key autoincrement, " + EVENT_WEEK + " integer null, " + EVENT_TYPE + " integer not null, "
		+ EVENT_TIME + " text not null, " + EVENT_TASK + " integer null);";
	
	private final Context context;
	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;
	
	public DBAdapter(Context ctx) {
		this.context = ctx;
		DBHelper = new DatabaseHelper(context);
	}
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, "worktimetracker", null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
			// add default task
			db.execSQL("insert into " + TASK + " (" + TASK_NAME + ", " + TASK_ACTIVE + ") values ('Default', 1)");
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(DBAdapter.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion
				+ ", which destroys all old data!");
			db.execSQL("drop table if exists " + TASK);
			db.execSQL("drop table if exists " + WEEK);
			db.execSQL("drop table if exists " + EVENT);
			onCreate(db);
		}
	}
	
	/**
	 * open the database
	 */
	public DBAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}
	
	/**
	 * close the database
	 */
	public void close() {
		DBHelper.close();
	}
	
	public void beginTransaction() {
		db.beginTransaction();
	}
	
	public void commitTransaction() {
		db.setTransactionSuccessful();
		db.endTransaction();
	}
	
	public void rollbackTransaction() {
		db.endTransaction();
	}
	
	// =======================================================
	
	public long insertTask(String name) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(TASK_NAME, name);
		initialValues.put(TASK_ACTIVE, 1);
		return db.insert(TASK, null, initialValues);
	}
	
	public boolean deleteTask(long id) {
		return db.delete(TASK, TASK_ID + "=" + id, null) > 0;
	}
	
	public Cursor getAllTasks() {
		return db.query(TASK, new String[] {TASK_ID, TASK_NAME, TASK_ACTIVE}, null, null, null, null, null);
	}
	
	public Cursor getActiveTasks() {
		return db.query(TASK, new String[] {TASK_ID, TASK_NAME}, TASK_ACTIVE + "!=0", null, null, null, null);
	}
	
	public boolean updateTask(long id, String name, Integer active) {
		ContentValues args = new ContentValues();
		args.put(TASK_NAME, name);
		args.put(TASK_ACTIVE, active);
		return db.update(TASK, args, TASK_ID + "=" + id, null) > 0;
	}
	
	// =======================================================
	
	public long insertWeek(String start, Integer sum) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(WEEK_START, start);
		initialValues.put(WEEK_SUM, sum);
		return db.insert(WEEK, null, initialValues);
	}
	
	public boolean deleteWeek(long id) {
		return db.delete(WEEK, WEEK_ID + "=" + id, null) > 0;
	}
	
	public Cursor getAllWeeks() {
		return db.query(WEEK, new String[] {WEEK_ID, WEEK_START, WEEK_SUM}, null, null, null, null, null);
	}
	
	public boolean updateWeek(long id, String start, Integer sum) {
		ContentValues args = new ContentValues();
		args.put(WEEK_START, start);
		args.put(WEEK_SUM, sum);
		return db.update(WEEK, args, WEEK_ID + "=" + id, null) > 0;
	}
	
	// =======================================================
	
	public long insertEvent(Integer week, String time, Integer type, Integer task) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(EVENT_WEEK, week);
		initialValues.put(EVENT_TIME, time);
		initialValues.put(EVENT_TYPE, type);
		initialValues.put(EVENT_TASK, task);
		return db.insert(EVENT, null, initialValues);
	}
	
	public boolean deleteEvent(long id) {
		return db.delete(EVENT, EVENT_ID + "=" + id, null) > 0;
	}
	
	public Cursor getAllEvents() {
		return db.query(EVENT, new String[] {EVENT_ID, EVENT_WEEK, EVENT_TIME, EVENT_TYPE, EVENT_TASK}, null, null,
			null, null, null);
	}
	
	public Cursor getEventsInWeek(Integer week) {
		if (week != null) {
			return db.query(true, EVENT, new String[] {EVENT_ID, EVENT_WEEK, EVENT_TIME, EVENT_TYPE, EVENT_TASK},
				EVENT_WEEK + "=" + week, null, null, null, null, null);
		} else {
			return db.query(true, EVENT, new String[] {EVENT_ID, EVENT_WEEK, EVENT_TIME, EVENT_TYPE, EVENT_TASK},
				EVENT_WEEK + " is null", null, null, null, null, null);
		}
	}
	
	public boolean updateEvent(long id, Integer week, String time, Integer type, Integer task) {
		ContentValues args = new ContentValues();
		args.put(EVENT_WEEK, week);
		args.put(EVENT_TIME, time);
		args.put(EVENT_TYPE, type);
		args.put(EVENT_TASK, task);
		return db.update(EVENT, args, EVENT_ID + "=" + id, null) > 0;
	}
}
