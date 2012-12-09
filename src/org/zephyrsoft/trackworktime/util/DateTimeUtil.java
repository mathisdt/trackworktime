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
package org.zephyrsoft.trackworktime.util;

import hirondelle.date4j.DateTime;
import java.util.TimeZone;

/**
 * Utility class for handling {@link DateTime} objects and converting them.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class DateTimeUtil {
	
	/**
	 * Gets the current date and time.
	 * 
	 * @return {@link DateTime} object
	 */
	public static DateTime getCurrentDateTime() {
		DateTime now = DateTime.now(TimeZone.getDefault());
		return now;
	}
	
	/**
	 * Gets the week's start related to the specified date and time.
	 * 
	 * @param dateTime the date and time
	 * @return {@code DateTime} object
	 */
	public static String getWeekStart(DateTime dateTime) {
		// go back to this day's start
		DateTime ret = dateTime.getStartOfDay();
		// go back to last Monday
		while (ret.getWeekDay() != 2) {
			ret = ret.minusDays(1);
		}
		return DateTimeUtil.dateTimeToString(ret);
	}
	
	/**
	 * Formats a {@link DateTime} to a String.
	 * 
	 * @param dateTime the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String dateTimeToString(DateTime dateTime) {
		return dateTime.format("YYYY-MM-DD hh:mm:ss.ffff");
	}
	
	/**
	 * Formats a {@link DateTime} to a String which contains the date only (omitting the time part).
	 * 
	 * @param dateTime the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String dateTimeToDateString(DateTime dateTime) {
		return dateTime.format("YYYY-MM-DD");
	}
	
	/**
	 * Formats a String to a {@link DateTime}.
	 * 
	 * @param string the input (may not be null)
	 * @return the DateTime which corresponds to the given input
	 */
	public static DateTime stringToDateTime(String string) {
		return new DateTime(string);
	}
	
	/**
	 * Formats a {@link DateTime} to a String which contains the hour and minute only (omitting the date and the
	 * seconds).
	 * 
	 * @param dateTime the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String dateTimeToHourMinuteString(DateTime dateTime) {
		// if the format is changed here, change it also in the method getCompleteDayAsHourMinuteString()
		return dateTime.format("hh:mm");
	}
	
	/**
	 * Parse a time as if it was today, resulting in a complete DateTime object containing date AND time.
	 * 
	 * @param timeString a String which contains the hour and minute only (omitting the date and the seconds), e.g.
	 *            "14:30"
	 * @return a DateTime which represents the given time on the current day
	 */
	public static DateTime parseTimeForToday(String timeString) {
		DateTime now = getCurrentDateTime();
		DateTime ret = new DateTime(dateTimeToDateString(now) + " " + timeString);
		return ret;
	}
	
	/**
	 * Returns a {@code String} representing the amount of time for a complete day (24 hours). The format is the same as
	 * in {@link #dateTimeToHourMinuteString(DateTime)}.
	 */
	public static String getCompleteDayAsHourMinuteString() {
		return "24:00";
	}
	
}
