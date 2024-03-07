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
package org.zephyrsoft.trackworktime.options;

import org.zephyrsoft.trackworktime.R;

import java.util.HashSet;
import java.util.Set;

/**
 * Central holder for all keys which are defined in XML files but have to be used in Java code.
 */
public enum Key {

	HOME_TIME_ZONE("keyHomeTimezone", DataType.TIMEZONEID, null, R.string.homeTimezone),

	ENABLE_FLEXI_TIME("keyEnableFlexiTime", DataType.BOOLEAN, null, R.string.enableFlexiTime),
	FLEXI_TIME_START_VALUE("keyFlexiTimeStartValue", DataType.HOUR_MINUTE, ENABLE_FLEXI_TIME,
		R.string.flexiTimeStartValue),
	FLEXI_TIME_TARGET("keyFlexiTimeTarget", DataType.HOUR_MINUTE, ENABLE_FLEXI_TIME, R.string.flexiTimeTarget),
	FLEXI_TIME_DAY_MONDAY("keyFlexiTimeDayMonday", DataType.BOOLEAN, ENABLE_FLEXI_TIME, R.string.mondayLong),
	FLEXI_TIME_DAY_TUESDAY("keyFlexiTimeDayTuesday", DataType.BOOLEAN, ENABLE_FLEXI_TIME, R.string.tuesdayLong),
	FLEXI_TIME_DAY_WEDNESDAY("keyFlexiTimeDayWednesday", DataType.BOOLEAN, ENABLE_FLEXI_TIME, R.string.wednesdayLong),
	FLEXI_TIME_DAY_THURSDAY("keyFlexiTimeDayThursday", DataType.BOOLEAN, ENABLE_FLEXI_TIME, R.string.thursdayLong),
	FLEXI_TIME_DAY_FRIDAY("keyFlexiTimeDayFriday", DataType.BOOLEAN, ENABLE_FLEXI_TIME, R.string.fridayLong),
	FLEXI_TIME_DAY_SATURDAY("keyFlexiTimeDaySaturday", DataType.BOOLEAN, ENABLE_FLEXI_TIME, R.string.saturdayLong),
	FLEXI_TIME_DAY_SUNDAY("keyFlexiTimeDaySunday", DataType.BOOLEAN, ENABLE_FLEXI_TIME, R.string.sundayLong),
	FLEXI_TIME_RESET_INTERVAL("keyFlexiTimeResetInterval", DataType.ENUM_NAME,
			ENABLE_FLEXI_TIME, R.string.flexiTimeResetInterval),

	DECIMAL_TIME_SUMS("keyShowDecimalTimeAmounts", DataType.BOOLEAN, null, R.string.showDecimalTimeAmounts),

	ROUNDING_ENABLED("keyRoundingEnabled", DataType.BOOLEAN, null, R.string.roundingEnabled),
	SMALLEST_TIME_UNIT("keySmallestTimeUnit", DataType.INTEGER, ROUNDING_ENABLED, R.string.smallestTimeUnit),

	LOCATION_BASED_TRACKING_ENABLED("keyLocationBasedTrackingEnabled", DataType.BOOLEAN, null,
		R.string.enableLocationBasedTracking),
	LOCATION_BASED_TRACKING_VIBRATE("keyLocationBasedTrackingVibrate", DataType.BOOLEAN,
		LOCATION_BASED_TRACKING_ENABLED, R.string.locationBasedTrackingVibrate),
	LOCATION_BASED_TRACKING_LATITUDE("keyLocationBasedTrackingLatitude", DataType.DOUBLE,
		LOCATION_BASED_TRACKING_ENABLED, R.string.workplaceLatitude),
	LOCATION_BASED_TRACKING_LONGITUDE("keyLocationBasedTrackingLongitude", DataType.DOUBLE,
		LOCATION_BASED_TRACKING_ENABLED, R.string.workplaceLongitude),
	LOCATION_BASED_TRACKING_TOLERANCE("keyLocationBasedTrackingTolerance", DataType.INTEGER,
		LOCATION_BASED_TRACKING_ENABLED, R.string.trackingTolerance),
	LOCATION_BASED_TRACKING_IGNORE_BEFORE_EVENTS("keyLocationBasedTrackingIgnoreBeforeEvents",
		DataType.INTEGER_OR_EMPTY, LOCATION_BASED_TRACKING_ENABLED, R.string.ignoreBefore),
	LOCATION_BASED_TRACKING_IGNORE_AFTER_EVENTS("keyLocationBasedTrackingIgnoreAfterEvents", DataType.INTEGER_OR_EMPTY,
		LOCATION_BASED_TRACKING_ENABLED, R.string.ignoreAfter),

	AUTO_PAUSE_ENABLED("keyAutoPauseEnabled", DataType.BOOLEAN, null, R.string.autoPauseEnabled),
	AUTO_PAUSE_BEGIN("keyAutoPauseBegin", DataType.TIME, AUTO_PAUSE_ENABLED, R.string.autoPauseBegin),
	AUTO_PAUSE_END("keyAutoPauseEnd", DataType.TIME, AUTO_PAUSE_ENABLED, R.string.autoPauseEnd),

	NOTIFICATION_ENABLED("keyNotificationEnabled", DataType.BOOLEAN, null, R.string.notificationEnabled),
	NOTIFICATION_ALWAYS("keyNotificationAlways", DataType.BOOLEAN, NOTIFICATION_ENABLED, R.string.notificationAlways),
	NOTIFICATION_NONPERSISTENT("keyNotificationNonPersistent", DataType.BOOLEAN, NOTIFICATION_ENABLED, R.string.notificationAlways),
	NOTIFICATION_SILENT("keyNotificationSilent", DataType.BOOLEAN, NOTIFICATION_ENABLED, R.string.notificationSilent),
	NOTIFICATION_USES_FLEXI_TIME_AS_TARGET("keyNotificationUsesFlexiTimeAsTarget", DataType.BOOLEAN,
		NOTIFICATION_ENABLED, R.string.notificationUsesFlexiTimeAsTarget),
	NEVER_UPDATE_PERSISTENT_NOTIFICATION("keyNeverUpdatePersistentNotification", DataType.BOOLEAN,
		NOTIFICATION_ENABLED, R.string.neverUpdatePersistentNotification),
	FLEXI_TIME_TO_ZERO_ON_EVERY_DAY("keyFlexiTimeToZeroOnEveryDay", DataType.BOOLEAN, NOTIFICATION_ENABLED,
		R.string.flexiTimeToZeroOnEveryDay),
	NOTIFICATION_ON_PEBBLE("keyPebbleNotification", DataType.BOOLEAN, null, R.string.pebbleNotification),

	WIFI_BASED_TRACKING_ENABLED("keyWifiBasedTrackingEnabled", DataType.BOOLEAN, null, R.string.enableWifiBasedTracking),
	WIFI_BASED_TRACKING_VIBRATE("keyWifiBasedTrackingVibrate", DataType.BOOLEAN, WIFI_BASED_TRACKING_ENABLED,
		R.string.wifiBasedTrackingVibrate),
	WIFI_BASED_TRACKING_SSID("keyWifiBasedTrackingSSID", DataType.SSID, WIFI_BASED_TRACKING_ENABLED,
		R.string.workplaceWifiSSID),
	WIFI_BASED_TRACKING_CHECK_INTERVAL("keyWifiBasedTrackingCheckInterval", DataType.INTEGER, WIFI_BASED_TRACKING_ENABLED,
		R.string.wifiBasedTrackingCheckInterval),

	AUTOMATIC_TRACKING_METHODS_GENERATE_EVENTS_SEPARATELY(
		"keyEachTrackingMethodGeneratesEventsSeparately", DataType.BOOLEAN, null,
		R.string.methodsGenerateEventsSeparately),

	REPORT_LAST_RANGE("keyReportLastUsedRange", DataType.INTEGER, null, null),
	REPORT_LAST_UNIT("keyReportLastUsedUnit", DataType.INTEGER, null, null),
	REPORT_LAST_GROUPING("keyReportLastUsedGrouping", DataType.INTEGER, null, null),

	AUTOMATIC_BACKUP_LAST_TIME("keyAutomaticBackupLastTime", DataType.LONG, null, null);

	private final String name;
	private final DataType dataType;
	private final Key parent;
	private final Integer readableNameResourceId;

	Key(String name, DataType dataType, Key parent, Integer readableNameResourceId) {
		this.name = name;
		this.dataType = dataType;
		this.parent = parent;
		this.readableNameResourceId = readableNameResourceId;
	}

	/**
	 * Get the name for use in SharedPreferences.getXXX(name, ...).
	 */
	public String getName() {
		return name;
	}

	public DataType getDataType() {
		return dataType;
	}

	public Key getParent() {
		return parent;
	}

	public Integer getReadableNameResourceId() {
		return readableNameResourceId;
	}

	public static Key getKeyWithName(String name) {
		for (Key key : values()) {
			if (key.getName().equals(name)) {
				return key;
			}
		}
		return null;
	}

	public static Set<Key> getChildKeys(Key parentKey) {
		Set<Key> ret = new HashSet<>();
		for (Key key : values()) {
			if (key.getParent() == parentKey) {
				ret.add(key);
			}
		}
		return ret;
	}

}
