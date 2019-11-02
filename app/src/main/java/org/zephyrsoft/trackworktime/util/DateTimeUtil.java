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
package org.zephyrsoft.trackworktime.util;

import org.zephyrsoft.trackworktime.model.WeekDayEnum;

import java.util.TimeZone;

import hirondelle.date4j.DateTime;

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
		DateTime now = DateTime.now(getCurrentTimeZone());
		return now;
	}

	/**
	 * Gets the current date as formatted string.
	 *
	 * @return date in format "YYYY-MM-DD"
	 */
	public static String getCurrentDateAsString() {
		DateTime now = DateTime.now(getCurrentTimeZone());
		return now.format("YYYY-MM-DD");
	}

	/**
	 * Gets the current time zone
	 *
	 * @return {@link TimeZone} object
	 */
	public static TimeZone getCurrentTimeZone() {
		return TimeZone.getDefault();
	}

	/**
	 * Determines if the given {@link DateTime} is in the future.
	 */
	public static boolean isInFuture(DateTime dateTime) {
		DateTime now = getCurrentDateTime();
		return now.lt(dateTime);
	}

	/**
	 * Determines if the given {@link DateTime} is in the past.
	 */
	public static boolean isInPast(DateTime dateTime) {
		DateTime now = getCurrentDateTime();
		return now.gt(dateTime);
	}

	/**
	 * Gets the week's start related to the specified date and time.
	 * 
	 * @param dateTime
	 *            the date and time
	 * @return a string representing the week start
	 */
	public static String getWeekStartAsString(DateTime dateTime) {
		return DateTimeUtil.dateTimeToString(getWeekStart(dateTime));
	}

	/**
	 * Gets the week's start related to the specified date and time.
	 * 
	 * @param dateTime
	 *            the date and time
	 * @return a DateTime representing the week start
	 */
	public static DateTime getWeekStart(DateTime dateTime) {
		// go back to this day's start
		DateTime ret = dateTime.getStartOfDay();
		// go back to last Monday
		while (ret.getWeekDay() != WeekDayEnum.MONDAY.getValue()) {
			ret = ret.minusDays(1);
		}
		return ret;
	}

	/**
	 * Formats a {@link DateTime} to a String.
	 * 
	 * @param dateTime
	 *            the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String dateTimeToString(DateTime dateTime) {
		return dateTime.format("YYYY-MM-DD hh:mm:ss.ffff");
	}

	/**
	 * Formats a {@link DateTime} to a String which contains the date only (omitting the time part).
	 * 
	 * @param dateTime
	 *            the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String dateTimeToDateString(DateTime dateTime) {
		return dateTime.format("YYYY-MM-DD");
	}

	/**
	 * Formats a String to a {@link DateTime}.
	 * 
	 * @param string
	 *            the input (may not be null)
	 * @return the DateTime which corresponds to the given input
	 */
	public static DateTime stringToDateTime(String string) {
		return new DateTime(string);
	}

	/**
	 * Formats a {@link DateTime} to a String which contains the hour and minute only (omitting the date and the
	 * seconds).
	 * 
	 * @param dateTime
	 *            the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String dateTimeToHourMinuteString(DateTime dateTime) {
		// if the format is changed here, change it also in the method getCompleteDayAsHourMinuteString()
		return dateTime.format("hh:mm");
	}

	/**
	 * Parse a time as if it was today, resulting in a complete DateTime object containing date AND time.
	 * 
	 * @param timeString
	 *            a String which contains the hour and minute only (omitting the date and the seconds), e.g.
	 *            "14:30"
	 * @return a DateTime which represents the given time on the current day
	 */
	public static DateTime parseTimeForToday(String timeString) {
		return parseTimeFor(getCurrentDateTime(), timeString);
	}

	/**
	 * Parse a time as if it was on a specific day, resulting in a complete DateTime object containing date AND time.
	 * 
	 * @param day
	 *            the date for which the time should be parsed (only the year, month and day fields are read)
	 * @param timeString
	 *            a String which contains the hour and minute only (omitting the date and the seconds), e.g.
	 *            "14:30"
	 * @return a DateTime which represents the given time on the given day
	 */
	public static DateTime parseTimeFor(DateTime day, String timeString) {
		DateTime ret = new DateTime(dateTimeToDateString(day) + " " + refineTime(timeString));
		ret.truncate(DateTime.Unit.MINUTE);
		return ret;
	}

	/**
	 * Returns a {@code String} representing the amount of time for a complete day (24 hours). The format is the same as
	 * in {@link #dateTimeToHourMinuteString(DateTime)}.
	 */
	public static String getCompleteDayAsHourMinuteString() {
		return "24:00";
	}

	/**
	 * Prepare the a user-entered time string to suffice for {@link #parseTimeForToday}.
	 */
	public static String refineTime(String timeString) {
		String ret = refineHourMinute(timeString);
		// append seconds
		ret += ":00";
		return ret;
	}

	/**
	 * Prepare the a user-entered time string to represent "hours:minutes".
	 */
	public static String refineHourMinute(String timeString) {
		if (timeString == null || timeString.isEmpty()) {
			// empty means midnight
			return "00:00";
		}
		String ret = timeString.replace('.', ':');
		// cut off seconds if present
		ret = ret.replaceAll("^(\\d\\d?):(\\d\\d?):.*$", "$1:$2");
		// fix only one digit as hour
		ret = ret.replaceAll("^(\\d):", "0$1:");
		// fix only one digit as minute
		ret = ret.replaceAll(":(\\d)$", ":0$1");
		return ret;
	}

	/**
	 * Return the given number left-padded with 0 if applicable.
	 */
	public static String padToTwoDigits(int number) {
		if (number < 0) {
			throw new IllegalArgumentException("number has to be >= 0");
		} else if (number < 10) {
			return "0" + String.valueOf(number);
		} else {
			return String.valueOf(number);
		}
	}

	/**
	 * Get the week start date of the first week of the given year, according to ISO 8601.
	 */
	public static DateTime getBeginOfFirstWeekFor(int year) {
		DateTime date = new DateTime(String.valueOf(year) + "-01-01 00:00:00");
		while (date.getWeekDay() != WeekDayEnum.THURSDAY.getValue()) {
			date = date.plusDays(1);
		}
		date.minusDays(3);
		return date;
	}

	/**
	 * Add number of weeks to provided date
	 */
	public static DateTime plusWeeks(DateTime fromDate, int weekCount) {
		int plusDays = weekCount * 7;
		return fromDate.plusDays(plusDays);
	}

	public static void test() {
		boolean valid = false;
		try {
			DateTime toTest = parseTimeForToday("");
			valid = toTest != null
				&& toTest.getHour() == 0
				&& toTest.getMinute() == 0;
		} catch (Exception e) {
			throw new AssertionError(": " + e.getMessage());
		}
		if (!valid) {
			throw new AssertionError("parseTimeForToday");
		}
	}
}
