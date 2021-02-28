package org.zephyrsoft.trackworktime.weektimes;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.ChronoUnit;
import org.zephyrsoft.trackworktime.model.Week;

/**
 * Converts week-index to {@link Week} and vice versa, where week-index is number of weeks after epoch,
 * starting with 0.
 *
 * E.g. index of 0 means 1st week after epoch.
 */
public class WeekIndexConverter {
	private static final LocalDate epochDate = LocalDate.ofEpochDay(0);

	public static @NonNull Week getWeekForIndex(@IntRange(from=0) int weekIndex) {
		if(weekIndex < 0) {
			throw new IllegalArgumentException("Week index should be positive");
		}
		
		return new Week(epochDate.plusWeeks(weekIndex));
	}

	public static @IntRange(from=0) int getIndexForDate(LocalDate date) {
		return (int) ChronoUnit.WEEKS.between(epochDate, date.with(DayOfWeek.MONDAY)) + 1;
	}

	public static @NonNull Week getWeekForDate(LocalDate date) {
		return new Week(date);
	}
}
