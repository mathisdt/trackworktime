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
package org.zephyrsoft.trackworktime.weektimes;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.model.Week;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
