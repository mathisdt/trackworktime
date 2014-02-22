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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.zephyrsoft.trackworktime.util.Logger;

/**
 * Helper class to manage the SQLite database.
 * 
 * Database version history:
 * 
 * 1: used only in development.
 * 2: initial layout, since 0.5.0.
 * 3: added column "default" in task table, since 0.5.12.
 * 
 * @author Mathis Dirksen-Thedens
 */
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

	/** name of the week table */
	public static final String WEEK = "week";
	/** name of the ID attribute of the week table */
	public static final String WEEK_ID = "_id";
	/** name of the start attribute of the week table - date and time of monday 0:00 AM */
	public static final String WEEK_START = "start";
	/** name of the sum attribute of the week table - in whole minutes */
	public static final String WEEK_SUM = "sum";

	/** name of the event table */
	public static final String EVENT = "event";
	/** name of the ID attribute of the event table */
	public static final String EVENT_ID = "_id";
	/** name of the week attribute of the event table - reference to WEEK_ID */
	public static final String EVENT_WEEK = "week";
	/** name of the type attribute of the event table */
	public static final String EVENT_TYPE = "type";
	/** name of the time attribute of the event table */
	public static final String EVENT_TIME = "time";
	/** name of the task attribute of the event table - reference to TASK_ID */
	public static final String EVENT_TASK = "task";
	/** name of the customtext attribute of the event table */
	public static final String EVENT_TEXT = "customtext";

	private static final String DATABASE_NAME = "trackworktime.db";
	private static final int DATABASE_VERSION = 3;

	private static final String DATABASE_CREATE_TASK = "create table " + TASK + " (" + TASK_ID
		+ " integer primary key autoincrement, " + TASK_NAME + " text not null, " + TASK_ACTIVE + " integer not null, "
		+ TASK_ORDERING + " integer null);";
	private static final String DATABASE_CREATE_WEEK = "create table " + WEEK + " (" + WEEK_ID
		+ " integer primary key autoincrement, " + WEEK_START + " text not null, " + WEEK_SUM + " integer null);";
	private static final String DATABASE_CREATE_EVENT = "create table " + EVENT + " (" + EVENT_ID
		+ " integer primary key autoincrement, " + EVENT_WEEK + " integer null, " + EVENT_TYPE + " integer not null, "
		+ EVENT_TIME + " text not null, " + EVENT_TASK + " integer null, " + EVENT_TEXT + " text null);";

	private static final String DATABASE_INSERT_TASK = "insert into " + TASK + " (" + TASK_NAME + ", " + TASK_ACTIVE
		+ ", " + TASK_ORDERING + ") values ('Default', 1, 0)";

	private static final String DATABASE_ALTER_TASK_2_TO_3 = "alter table " + TASK
		+ " add column " + TASK_DEFAULT + " integer not null default 0;";
	private static final String DATABASE_UPDATE_TASK_2_TO_3 = "update " + TASK
		+ " set " + TASK_DEFAULT + "=1 where " + TASK_NAME + "='Default';";

	/**
	 * Constructor
	 */
	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		dbSetup(database);
		dbUpgradeFrom2to3(database);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		Logger.warn("upgrading database from version {0} to {1}", oldVersion, newVersion);
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
		if (currentVersion != newVersion) {
			throw new IllegalStateException("could not upgrade database");
		}
	}

	private void dbSetup(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE_TASK);
		database.execSQL(DATABASE_CREATE_WEEK);
		database.execSQL(DATABASE_CREATE_EVENT);
		// add default task
		database.execSQL(DATABASE_INSERT_TASK);
	}

	private void dbUpgradeFrom2to3(SQLiteDatabase database) {
		database.execSQL(DATABASE_ALTER_TASK_2_TO_3);
		// make 'Default' task "really default"
		database.execSQL(DATABASE_UPDATE_TASK_2_TO_3);
	}

}
