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
package org.zephyrsoft.trackworktime.util;

import org.apache.commons.lang3.StringUtils;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;
import org.threeten.bp.format.FormatStyle;
import org.threeten.bp.temporal.TemporalAccessor;
import org.threeten.bp.temporal.TemporalAdjusters;

import java.util.Locale;

/**
 * Utility class for handling dates and times.
 */
public class DateTimeUtil {
	private static final DateTimeFormatter LOCALIZED_DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
	private static final DateTimeFormatter LOCALIZED_DATE_SHORT = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
	private static final DateTimeFormatter LOCALIZED_TIME = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter HOUR_MINUTES = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter TIME_PRECISE = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	private static final DateTimeFormatter LOCALIZED_DAY_AND_DATE = new DateTimeFormatterBuilder()
			.appendPattern("eeee")
			.appendLiteral(", ")
			.appendLocalized(FormatStyle.SHORT, null)
			.toFormatter();

	/**
	 * Determines if the given {@link LocalDateTime} is in the future.
	 */
	public static boolean isInFuture(LocalDateTime dateTime) {
		return dateTime.isAfter(LocalDateTime.now());
	}

	public static boolean isInFuture(OffsetDateTime dateTime) {
		return dateTime.isAfter(OffsetDateTime.now());
	}

	/**
	 * Determines if the given {@link LocalDateTime} is in the past.
	 */
	public static boolean isInPast(LocalDateTime dateTime) {
		return dateTime.isBefore(LocalDateTime.now());
	}

	public static boolean isInPast(OffsetDateTime dateTime) {
		return dateTime.isBefore(OffsetDateTime.now());
	}

	public static LocalDate getWeekStart(LocalDate date) {
		return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	public static LocalDateTime getWeekStart(LocalDateTime dateTime) {
		return dateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	public static ZonedDateTime getWeekStart(ZonedDateTime dateTime) {
		return dateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	/**
	 * Formats a {@link LocalDateTime} to a String.
	 *
	 * @param dateTime the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String formatTimePrecise(LocalDateTime dateTime) {
		return dateTime.format(TIME_PRECISE);
	}

	/**
	 * Formats a {@link OffsetDateTime} to a String.
	 *
	 * @param dateTime the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String formatLocalizedDateTime(OffsetDateTime dateTime) {
		return formatLocalizedDate(dateTime.toLocalDate()) + " / " + formatLocalizedTime(dateTime);
	}

	/**
	 * Formats a {@link LocalDate} to a String.
	 *
	 * @param date the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String formatLocalizedDate(LocalDate date) {
		return date.format(LOCALIZED_DATE);
	}

	public static String formatLocalizedDateShort(TemporalAccessor temporal) {
		return LOCALIZED_DATE_SHORT.format(temporal);
	}

	/**
	 * Formats a {@link OffsetDateTime} to a String, containing only hours and minutes.
	 *
	 * @param dateTime the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String formatLocalizedTime(OffsetDateTime dateTime) {
		return dateTime.format(LOCALIZED_TIME);
	}

	/**
	 * Formats a {@link TemporalAccessor} to a String.
	 *
	 * @param date the input (may not be null)
	 * @return the String which corresponds to the given input. E.g. "Friday, 22.2.2222".
	 */
	public static String formatLocalizedDayAndDate(TemporalAccessor date) {
		String dateString = LOCALIZED_DAY_AND_DATE.format(date);
		return StringUtils.capitalize(dateString);
	}

	/**
	 * Formats a {@link LocalDate} to a String.
	 *
	 * @param date the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	@Deprecated
	public static String dateToString(LocalDate date) {
		return date.format(LOCALIZED_DATE);
	}

	/**
	 * Formats a {@link LocalDate} to a String.
	 *
	 * @param date the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String dateToULString(LocalDate date) {
		return date.format(DATE);
	}

	/**
	 * Formats a {@link ZonedDateTime} to a String.
	 *
	 * @param dateTime the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String dateToULString(ZonedDateTime dateTime) {
		return dateTime.format(DATE);
	}

	/**
	 * Formats a {@link LocalDateTime} to a String which contains the hour and minute only.
	 *
	 * @param dateTime the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String dateTimeToHourMinuteString(LocalDateTime dateTime) {
		return dateTime.format(HOUR_MINUTES);
	}

	/**
	 * Parse a time string to a LocalTime
	 *
	 * @param timeString a String which contains the hour and minute only, e.g. "14:30"
	 * @return a LocalTime which represents the given time
	 */
	public static LocalTime parseTime(String timeString) {
		return LocalTime.parse(refineTime(timeString));
	}

	/**
	 * Prepare the a user-entered time string to suffice for {@link #parseTime}.
	 */
	public static String refineTime(String timeString) {
		String ret = refineHourMinute(timeString);
		// append seconds
		ret += ":00";
		return ret;
	}

	/**
	 * Prepare the a user-entered time string to represent "hours:minutes".
	 * TODO DateTimeFormatterBuilder
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
			return "0" + number;
		} else {
			return String.valueOf(number);
		}
	}

	public static long dateToEpoch(ZonedDateTime accessor) {
		return Instant.from(accessor).toEpochMilli();
	}

	public static boolean isDurationValid(String value) {
		String[] pieces = value.split("[:.]");
		if (pieces.length == 2) {
			try {
				Integer.parseInt(pieces[0]);
				Integer.parseInt(pieces[1]);
				return true;
			} catch (NumberFormatException e) {
				// ignore and return false
			}
		}
		return false;
	}

	public static String formatDuration(Integer duration) {
		if (duration != null) {
			return String.format(Locale.US, "%d:%02d", duration / 60, duration % 60);
		} else {
			return "0:00";
		}
	}
}
