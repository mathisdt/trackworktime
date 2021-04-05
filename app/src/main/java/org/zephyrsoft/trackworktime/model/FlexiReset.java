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
package org.zephyrsoft.trackworktime.model;

import android.content.SharedPreferences;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.TemporalAdjusters;
import org.zephyrsoft.trackworktime.options.Key;

public enum FlexiReset {
	NONE(1, Unit.NULL, "none"),
	DAILY(1, Unit.DAY, "daily"),
	WEEKLY(1, Unit.WEEK, "weekly"),
	MONTHLY(1, Unit.MONTH, "monthly"),
	QUARTERLY(3, Unit.MONTH, "quarterly"),
	HALF_YEARLY(6, Unit.MONTH, "half-yearly"),
	YEARLY(12, Unit.MONTH, "yearly");

	private final int intervalSize;
	private final Unit intervalUnit;
	private final String friendlyName;

	FlexiReset(@IntRange(from=1) int intervalSize, @NonNull Unit intervalUnit,
			@NonNull String friendlyName) {
		this.intervalSize = intervalSize;
		this.intervalUnit = intervalUnit;
		this.friendlyName = friendlyName;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public @NonNull LocalDate getLastResetDate(LocalDate fromDate) {
		switch (intervalUnit) {
			case NULL:
				return LocalDate.ofEpochDay(0);
			case DAY:
				return calcLastResetDayForDay(fromDate);
			case WEEK:
				return calcLastResetDayForWeek(fromDate);
			case MONTH:
				return calcLastResetDayForMonth(fromDate);
			default:
				throw new UnsupportedOperationException(intervalUnit.toString());
		}
	}

	public @NonNull LocalDate getNextResetDate(LocalDate fromDate) {
		switch (intervalUnit) {
			case NULL:
				return LocalDate.ofEpochDay(0);
			case DAY:
				return calcLastResetDayForDay(fromDate).plusDays(intervalSize);
			case WEEK:
				return calcLastResetDayForWeek(fromDate).plusWeeks(intervalSize);
			case MONTH:
				return calcLastResetDayForMonth(fromDate).plusMonths(intervalSize);
			default:
				throw new UnsupportedOperationException(intervalUnit.toString());
		}
	}

	public boolean isResetDay(LocalDate day) {
		LocalDate resetDay;
		switch(intervalUnit) {
			case NULL:
				return false;
			case DAY:
				resetDay = calcLastResetDayForDay(day); break;
			case WEEK:
				resetDay = calcLastResetDayForWeek(day); break;
			case MONTH:
				resetDay = calcLastResetDayForMonth(day); break;
			default: throw new UnsupportedOperationException(intervalUnit.toString());
		}

		return resetDay.isEqual(day);
	}

	private LocalDate calcLastResetDayForDay(LocalDate fromDate) {
		int daysDelta = (fromDate.getDayOfYear() - 1) % intervalSize;
		return fromDate.minusDays(daysDelta);
	}

	private LocalDate calcLastResetDayForWeek(LocalDate fromDate) {
		// starting point is the first Monday of the year
		// FIXME fails with intervalSize != 1
		LocalDate startDate =
				fromDate.withDayOfYear(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

		long weekDelta = ChronoUnit.WEEKS.between(startDate, fromDate) % intervalSize;

		return fromDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(weekDelta);
	}

	private LocalDate calcLastResetDayForMonth(LocalDate fromDate) {
		int monthsDelta = (fromDate.getMonthValue() - 1) % intervalSize;
		return fromDate.minusMonths(monthsDelta).withDayOfMonth(1);
	}

	public static FlexiReset loadFromPreferences(SharedPreferences preferences) {
		String key = Key.FLEXI_TIME_RESET_INTERVAL.getName();
		String defaultValue = FlexiReset.NONE.name();
		String string = preferences.getString(key, defaultValue);
		return valueOf(string);
	}

}
