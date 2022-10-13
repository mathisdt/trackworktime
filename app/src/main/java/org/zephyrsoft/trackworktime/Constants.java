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
package org.zephyrsoft.trackworktime;

/**
 * Constants for the app.
 */
public class Constants {

	/**
	 * The time to sleep between checks for automatic clock-in/clock-out and between calls to
	 * {@link Basics#periodicHook()} (in milliseconds).
	 */
	public static final long REPEAT_TIME = 1000 * 60;

	/** The pattern for vibration on automatic clock-in/clock-out (values in milliseconds). */
	public static final long[] VIBRATION_PATTERN = { 0, 200, 250, 500, 250, 200 };

	/** the directory in which backups and log files should be saved */
	public static final String DATA_DIR = "trackworktime";
	/** the name of the currently active log file */
	public static final String CURRENT_LOG_FILE_NAME = "log.txt";
	/** the path of the currently active log file */
	public static final String CURRENT_LOG_FILE_PATH = DATA_DIR + "/" + CURRENT_LOG_FILE_NAME;

	// notification & permission request IDs
	/** used for the persistent notification when tracking via location and/or WiFi */
	public static final int PERSISTENT_TRACKING_ID = 1;
	/** used for the status notification when clocked in */
	public static final int PERSISTENT_STATUS_ID = 2;
	/** used for the message about ACCESS_*_LOCATION */
	public static final int MISSING_PRIVILEGE_ACCESS_LOCATION_ID = 4;
	/** used when the automatic backup is running */
	public static final int AUTOMATIC_BACKUP_ID = 5;
	/** used for the message about ACCESS_WIFI_STATE */
	public static final int MISSING_PRIVILEGE_ACCESS_WIFI_STATE_ID = 16;
	public static final int PERMISSION_REQUEST_CODE_DOCUMENT_TREE_ON_STARTUP = 128;
	public static final int PERMISSION_REQUEST_CODE_DOCUMENT_TREE_ON_MANUAL_BACKUP = 256;
	public static final int PERMISSION_REQUEST_CODE_DOCUMENT_TREE_ON_MANUAL_RESTORE = 512;
	public static final int PERMISSION_REQUEST_CODE_DOCUMENT_TREE_ON_REPORT = 1024;
	public static final int PERMISSION_REQUEST_CODE_DOCUMENT_TREE_ON_LOGEXPORT = 2048;
	public static final int PERMISSION_REQUEST_CODE_DOCUMENT_TREE_ON_RESELECT = 4096;

	// intent extra keys
	/** used to transport the date via an intent's extended data */
	public static final String DATE_EXTRA_KEY = "date";
	/** key for the intent extra "week start" */
	public static final String WEEK_START_EXTRA_KEY = "WEEK_START_EXTRA_KEY";
	/** key for the intent extra "event id" */
	public static final String EVENT_ID_EXTRA_KEY = "EVENT_ID_EXTRA_KEY";
	/** key for the intent extra "period" */
	public static final String PERIOD_EXTRA_KEY = "PERIOD_EXTRA_KEY";
	/** used to transport the message via an intent's extended data */
	public static final String MESSAGE_EXTRA_KEY = "message";
	/** used to transport the notification' ID via an intent's extended data */
	public static final String ID_EXTRA_KEY = "notificationId";
	/** used to transport the type via an intent's extended data: 0 = clock in / 1 = clock out */
	public static final String TYPE_EXTRA_KEY = "type";
	/** used to transport the type string via an intent's extended data */
	public static final String TYPE_STRING_EXTRA_KEY = "typeString";
	/** the key for the {@link String} which determines the ssid to look for */
	public static final String INTENT_EXTRA_SSID = "SSID";
	/** the key for the {@link Boolean} which determines if vibration should be used */
	public static final String INTENT_EXTRA_VIBRATE = "VIBRATE";
	/** the key for the {@link Integer} which determines the interval between Wi-Fi checks */
	public static final String INTENT_EXTRA_WIFI_CHECK_INTERVAL = "WIFI_CHECK_INTERVAL";
	/** the key for the {@link Double} which determines the latitude */
	public static final String INTENT_EXTRA_LATITUDE = "LATITUDE";
	/** the key for the {@link Double} which determines the longitude */
	public static final String INTENT_EXTRA_LONGITUDE = "LONGITUDE";
	/** the key for the {@link Double} which determines the tolerance */
	public static final String INTENT_EXTRA_TOLERANCE = "TOLERANCE";

	/** the key for the new event's task (used for third-party events) */
	public static final String INTENT_EXTRA_TASK = "task";
	/** the key for the new event's text (used for third-party events) */
	public static final String INTENT_EXTRA_TEXT = "text";
	/** the key for the intent name to reply to on status request (used for third-party events) */
	public static final String INTENT_EXTRA_REPLY_INTENT = "reply_intent";
	/** the key in the reply to on status request for status (checked-in or checked-out) */
	public static final String INTENT_EXTRA_REPLY_STATUS = "status";
	/** the key in the reply to on status request for current task (only if checked in currently) */
	public static final String INTENT_EXTRA_REPLY_CURRENT_TASK_NAME = "current-task-name";
	/** the key in the reply to on status request for current task (only if checked in currently) */
	public static final String INTENT_EXTRA_REPLY_CURRENT_TASK_ID = "current-task-id";
	/** the key in the reply to on status request for remaining time (in minutes) */
	public static final String INTENT_EXTRA_REPLY_MINUTES_REMAINING = "minutes-remaining";

	public static final String CLOCK_IN_ACTION = BuildConfig.APPLICATION_ID + ".ClockIn";
    public static final String CLOCK_OUT_ACTION = BuildConfig.APPLICATION_ID + ".ClockOut";
    public static final String STATUS_REQUEST_ACTION = BuildConfig.APPLICATION_ID + ".StatusRequest";
    public static final String EVENT_CREATED_ACTION = BuildConfig.APPLICATION_ID + ".event.Created";
    public static final String EVENT_UPDATED_ACTION = BuildConfig.APPLICATION_ID + ".event.Updated";
    public static final String EVENT_DELETED_ACTION = BuildConfig.APPLICATION_ID + ".event.Deleted";

	// Work manager unique name
	public static final String WORK_AUTOBACKUP = "AUTOBACKUP";
}
