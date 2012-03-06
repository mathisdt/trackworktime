package org.zephyrsoft.trackworktime.util;

import hirondelle.date4j.DateTime;
import java.util.TimeZone;

/**
 * Utility class for handling {@link DateTime} objects and converting them.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class DateTimeUtil {
	
	public static DateTime getCurrentDateTime() {
		DateTime now = DateTime.now(TimeZone.getDefault());
		return now;
	}
	
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
	 * @param dateTime
	 *            the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String dateTimeToString(DateTime dateTime) {
		return dateTime.format("YYYY-MM-DD hh:mm:ss");
	}
	
	/**
	 * Formats a {@link DateTime} to a String which contains the date only
	 * (omitting the time part).
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
	 * Formats a {@link DateTime} to a String which contains the hour and minute only
	 * (omitting the date and the seconds).
	 * 
	 * @param dateTime
	 *            the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String dateTimeToHourMinuteString(DateTime dateTime) {
		return dateTime.format("hh:mm");
	}
	
}
