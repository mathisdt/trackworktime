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

import android.app.Activity;

import org.apache.commons.lang3.StringUtils;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.model.Event;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Locale;

/**
 * Utility class for handling dates and times.
 */
public class DateTimeUtil {
	private static final DateTimeFormatter LOCALIZED_DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
	private static final DateTimeFormatter LOCALIZED_DATE_SHORT = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
	private static final DateTimeFormatter LOCALIZED_TIME = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
	private static final DateTimeFormatter LOCALIZED_DAY_AND_DATE = new DateTimeFormatterBuilder()
			.appendPattern("eeee")
			.appendLiteral(", ")
			.appendLocalized(FormatStyle.SHORT, null)
			.toFormatter();

	public static class LocalizedDayAndShortDateFormatter {
		private final Activity activity;
		private Locale locale;
		private DateTimeFormatter formatter;

		public LocalizedDayAndShortDateFormatter(Activity activity) {
			this.activity = activity;
			updateLocale();
		}

		private DateTimeFormatter createLocalizedDayAndShortDateFormat() {
			String shortDate = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
				FormatStyle.SHORT,
				null,
				IsoChronology.INSTANCE,
				locale)
				// remove year and some year-separators (at least in german dates, the dot after the month has to exist)
				.replaceAll("[ /-]? *[yY]+ *[ 年/.-]?", "");
			String pattern = "eee, " + shortDate;
			return DateTimeFormatter.ofPattern(pattern);
		}

		public String format(TemporalAccessor date) {
			String dateString = formatter.format(date)
				// remove possible abbreviation point ("Mo., 27.09." (german) or "Lun., 27/09" (french) looks odd):
				.replaceAll("^(\\p{Alpha}+)\\., ", "$1, ");
			return StringUtils.capitalize(dateString);
		}

		public void updateLocale() {
			locale = Basics.get(activity).getLocale();
			formatter = createLocalizedDayAndShortDateFormat();
		}

		public Locale getLocale() {
			return locale;
		}
	}

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
	 * Formats a {@link OffsetDateTime} to a String.
	 *
	 * @param dateTime the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String formatLocalizedDateTime(OffsetDateTime dateTime, Locale locale) {
		return formatLocalizedDate(dateTime.toLocalDate(), locale) + " / " + formatLocalizedTime(dateTime, locale);
	}

	/**
	 * Formats a {@link LocalDate} to a String.
	 *
	 * @param date the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String formatLocalizedDate(LocalDate date, Locale locale) {
		return date.format(LOCALIZED_DATE.withLocale(locale));
	}

	public static String formatLocalizedDateShort(TemporalAccessor temporal, Locale locale) {
		return LOCALIZED_DATE_SHORT.withLocale(locale).format(temporal);
	}

	/**
	 * Formats a {@link TemporalAccessor} to a String, containing only hours and minutes.
	 *
	 * @param temporal the input (may not be null)
	 * @return the String which corresponds to the given input
	 */
	public static String formatLocalizedTime(TemporalAccessor temporal, Locale locale) {
		return LOCALIZED_TIME.withLocale(locale).format(temporal);
	}

	/**
	 * Formats a {@link TemporalAccessor} to a String.
	 *
	 * @param date the input (may not be null)
	 * @return the String which corresponds to the given input. E.g. "Friday, 22.2.2222".
	 */
	public static String formatLocalizedDayAndDate(TemporalAccessor date, Locale locale) {
		String dateString = LOCALIZED_DAY_AND_DATE.withLocale(locale).format(date);
		return StringUtils.capitalize(dateString);
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

	public static String timestampNow() {
		return LocalDateTime.now().format(TIMESTAMP);
	}

	public static void truncateEventsToMinute(Collection<Event> events) {
		if (events != null) {
			for (Event event : events) {
				truncateEventToMinute(event);
			}
		}
	}

	public static void truncateEventToMinute(Event event) {
		if (event != null && event.getDateTime() != null) {
			event.setDateTime(truncateToMinute(event.getDateTime()));
		}
	}

	public static OffsetDateTime truncateToMinute(OffsetDateTime dt) {
		return dt != null
			? dt.withSecond(0).withNano(0)
			: null;
	}
}
