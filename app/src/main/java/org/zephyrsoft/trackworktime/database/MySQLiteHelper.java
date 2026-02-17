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
package org.zephyrsoft.trackworktime.database;

import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.R;

/**
 * Helper class to manage the SQLite database.
 *
 * Database version history:
 *
 * 1: used only in development.
 * 2: initial layout, since 0.5.0.
 * 3: added column "default" in task table, since 0.5.12.
 */
@SuppressWarnings("SyntaxError")
public class MySQLiteHelper extends SQLiteOpenHelper {

	/** name of the task table */
	public static final String TASK = "task";
	/** name of the ID attribute of the task table */
	public static final String TASK_ID = "_id";
	/** name of the name attribute of the task table */
	public static final String TASK_NAME = "name";
	/** name of the active attribute of the task table - 0=false, 1=true */
	public static final String TASK_ACTIVE = "active";
	/** name of the ordering attribute of the task table - for future use */
	public static final String TASK_ORDERING = "ordering";
	/** name of the default attribute of the task table */
	public static final String TASK_DEFAULT = "isdefault";

	/** name of the week table, no longer used */
	public static final String WEEK = "week";

	/** name of the event table */
	public static final String EVENT = "event";
	public static final String EVENT_V1 = "event_v1";

	/** name of the ID attribute of the event table */
	public static final String EVENT_ID = "_id";
	/** name of the type attribute of the event table */
	public static final String EVENT_TYPE = "type";
	/** name of the time attribute of the event table */
	public static final String EVENT_TIME = "time";
	/** name of the time zone offset attribute of the event table */
	public static final String EVENT_ZONE_OFFSET = "zone_offset";
	/** name of the task attribute of the event table - reference to TASK_ID */
	public static final String EVENT_TASK = "task";
	/** name of the customtext attribute of the event table */
	public static final String EVENT_TEXT = "comment";

	/** name of the target table */
	public static final String TARGET = "target";
	/** name of the ID attribute of the target table */
	public static final String TARGET_ID = "_id";
	/** name of the time attribute of the target table */
	public static final String TARGET_TIME = "time";
	/* name of the type attribute of the target table */
	public static final String TARGET_TYPE = "type";
	/** name of the value attribute of the target table */
	public static final String TARGET_VALUE = "value";
	/** name of the comment attribute of the event table */
	public static final String TARGET_TEXT = "comment";

	/** name of the cache table */
	public static final String CACHE = "cache";
	/** name of the date attribute of the cache table, Android needs "_id" column */
	public static final String CACHE_DATE = "_id";
	/** name of the worked attribute of the cache table */
	public static final String CACHE_WORKED = "worked";
	/** name of the target attribute of the cache table */
	public static final String CACHE_TARGET = "target";


	static final String DATABASE_NAME = "trackworktime.db";
	private static final int DATABASE_VERSION = 5;

	private static final String DATABASE_CREATE_TASK = "create table " + TASK + " (" + TASK_ID
		+ " integer primary key autoincrement, " + TASK_NAME + " text not null, " + TASK_ACTIVE + " integer not null, "
		+ TASK_ORDERING + " integer null);";

	private static final String DATABASE_CREATE_EVENT = "create table " + EVENT + " ("
		+ EVENT_ID + " integer primary key autoincrement, "
		+ EVENT_TIME + " integer not null, " + EVENT_ZONE_OFFSET  + " integer not null, "
		+ EVENT_TYPE + " integer not null, " + EVENT_TASK + " integer null, " + EVENT_TEXT + " text null);";

	private static final String DATABASE_CREATE_TARGET = "create table " + TARGET + " (" + TARGET_ID
		+ " integer primary key autoincrement, " + TARGET_TIME + " integer not null, " + TARGET_TYPE + " integer not null, "
		+ TARGET_VALUE + " integer, " + TARGET_TEXT + " text null);";

	private static final String DATABASE_CREATE_CACHE = "create table " + CACHE + " (" + CACHE_DATE
		+ " integer primary key, " + CACHE_WORKED + " integer not null, " + CACHE_TARGET + " integer);";

	private static final String DATABASE_INSERT_TASK = "insert into " + TASK + " (" + TASK_NAME + ", " + TASK_ACTIVE
		+ ", " + TASK_ORDERING + ") values ('Default', 1, 0)";

	private static final String DATABASE_ALTER_TASK_2_TO_3 = "alter table " + TASK
		+ " add column " + TASK_DEFAULT + " integer not null default 0;";
	private static final String DATABASE_UPDATE_TASK_2_TO_3 = "update " + TASK
		+ " set " + TASK_DEFAULT + "=1 where " + TASK_NAME + "='Default';";

	private static final String DATABASE_ALTER_WEEK_3_TO_4 = "alter table " + WEEK
		+ " add column flexi integer null;";

    private final Context context;

    /**
	 * Constructor
	 */
	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

	@Override
	public void onCreate(SQLiteDatabase database) {
		dbSetup(database);
		dbUpgradeFrom2to3(database);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		Logger.warn("upgrading database from version {} to {}", oldVersion, newVersion);
		int currentVersion = oldVersion;
		if (currentVersion <= 1) {
			database.execSQL("drop table if exists " + TASK);
			database.execSQL("drop table if exists " + WEEK);
			database.execSQL("drop table if exists " + EVENT);
			dbSetup(database);
			currentVersion = 2;
		}
		if (currentVersion == 2) {
			dbUpgradeFrom2to3(database);
			currentVersion++;
		}
		if (currentVersion == 3) {
			dbUpgradeFrom3to4(database);
			currentVersion++;
		}
		if (currentVersion == 4) {
			database.execSQL(DATABASE_CREATE_TARGET);
			database.execSQL(DATABASE_CREATE_CACHE);
			dbUpgradeFrom4to5(database);
			currentVersion++;
		}

		// TODO drop old event table on newer database versions
		//database.execSQL("drop table if exists " + EVENT_V1);

		if (currentVersion != newVersion) {
			throw new IllegalStateException("could not upgrade database");
		}
	}

	private void dbSetup(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE_TASK);
		database.execSQL(DATABASE_CREATE_EVENT);
		database.execSQL(DATABASE_CREATE_TARGET);
		database.execSQL(DATABASE_CREATE_CACHE);
		// add default task
		String insertDefaultTask = DATABASE_INSERT_TASK;
		try {
			insertDefaultTask = insertDefaultTask.replaceAll("'Default'",
				"'" + context.getString(R.string.default_task_name) + "'");
		} catch (Resources.NotFoundException nfe) {
			// the current language doesn't have a translated name for the default task
		}
		database.execSQL(insertDefaultTask);
	}

	private void dbUpgradeFrom2to3(SQLiteDatabase database) {
		database.execSQL(DATABASE_ALTER_TASK_2_TO_3);
		// make 'Default' task "really default"
		database.execSQL(DATABASE_UPDATE_TASK_2_TO_3);
	}

	private void dbUpgradeFrom3to4(SQLiteDatabase database) {
		database.execSQL(DATABASE_ALTER_WEEK_3_TO_4);
	}

	private void dbUpgradeFrom4to5(SQLiteDatabase database) {
		// drop obsolete week table
		database.execSQL("drop table if exists " + WEEK);

		// rename existing event database
		database.execSQL("ALTER TABLE " + EVENT + " RENAME TO " + EVENT_V1);

		// create new event table
		database.execSQL(DATABASE_CREATE_EVENT);

		// event migration needs user interaction and will start on next app usage
	}
}
