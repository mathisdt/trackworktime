package org.zephyrsoft.trackworktime.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {
	
	public static final String TASK = "task";
	public static final String TASK_ID = "_id";
	public static final String TASK_NAME = "name";
	public static final String TASK_ACTIVE = "active"; // 0=false, 1=true
	public static final String TASK_ORDERING = "ordering"; // for future use
	
	public static final String WEEK = "week";
	public static final String WEEK_ID = "_id";
	public static final String WEEK_START = "start"; // date and time of monday 0:00 AM
	public static final String WEEK_SUM = "sum"; // in whole minutes
	
	public static final String EVENT = "event";
	public static final String EVENT_ID = "_id";
	public static final String EVENT_WEEK = "week"; // reference to WEEK_ID
	public static final String EVENT_TYPE = "type";
	public static final String EVENT_TIME = "time";
	public static final String EVENT_TASK = "task"; // reference to TASK_ID
	public static final String EVENT_TEXT = "customtext";
	
	private static final String DATABASE_NAME = "trackworktime.db";
	private static final int DATABASE_VERSION = 5;
	private static final String DATABASE_CREATE = "create table " + TASK + " (" + TASK_ID
		+ " integer primary key autoincrement, " + TASK_NAME + " text not null, " + TASK_ACTIVE + " integer not null, "
		+ TASK_ORDERING + " integer null); create table " + WEEK + " (" + WEEK_ID
		+ " integer primary key autoincrement, " + WEEK_START + " text not null, " + WEEK_SUM
		+ " integer null); create table " + EVENT + " (" + EVENT_ID + " integer primary key autoincrement, "
		+ EVENT_WEEK + " integer null, " + EVENT_TYPE + " integer not null, " + EVENT_TIME + " text not null, "
		+ EVENT_TASK + " integer null, " + EVENT_TEXT + " text null);";
	
	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
		// add default task
		database.execSQL("insert into " + TASK + " (" + TASK_NAME + ", " + TASK_ACTIVE + ", " + TASK_ORDERING
			+ ") values ('Default', 1, 0)");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(MySQLiteHelper.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion
			+ ", which destroys all old data!");
		db.execSQL("drop table if exists " + TASK);
		db.execSQL("drop table if exists " + WEEK);
		db.execSQL("drop table if exists " + EVENT);
		onCreate(db);
	}
	
}
