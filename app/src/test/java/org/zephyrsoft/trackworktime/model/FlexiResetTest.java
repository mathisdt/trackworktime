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

import org.junit.Test;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;

import static com.google.common.truth.Truth.assertWithMessage;

public class FlexiResetTest {

	// Date, where day is start of the year, month, week
	private static final LocalDate startOfEverything = LocalDate.of(2018,1,1);
	private static final LocalDate endLoopDate = LocalDate.of(2019,1,1);

	@Test
	public void isResetDay() {
		for (LocalDate date = startOfEverything; date.isBefore(endLoopDate); date=date.plusDays(1)) {
			int month = date.getMonthValue();
			int monthDay = date.getDayOfMonth();
			boolean firstDayOfMonth = monthDay==1;

			checkIsResetDay(FlexiReset.NONE, date, false);
			checkIsResetDay(FlexiReset.DAILY, date, true);
			checkIsResetDay(FlexiReset.WEEKLY, date, date.getDayOfWeek() == DayOfWeek.MONDAY);
			checkIsResetDay(FlexiReset.MONTHLY, date, firstDayOfMonth);
			checkIsResetDay(FlexiReset.QUARTERLY, date, month%3==1 && firstDayOfMonth);
			checkIsResetDay(FlexiReset.HALF_YEARLY, date, month%6==1 && firstDayOfMonth);
			checkIsResetDay(FlexiReset.YEARLY, date, date.getDayOfYear()==1);
		}
	}

	private void checkIsResetDay(FlexiReset flexiReset, LocalDate date, boolean expectedResetDay) {
		boolean actualResetDay = flexiReset.isResetDay(date);
		assertWithMessage(flexiReset + ", " + date)
				.that(actualResetDay)
				.isEqualTo(expectedResetDay);
	}

	@Test
	public void calcLastResetDayFromDay() {
		LocalDate epoch = LocalDate.ofEpochDay(0);

		for (LocalDate date = startOfEverything; date.isBefore(endLoopDate); date=date.plusDays(1)) {
			checkLastResetDay(FlexiReset.NONE, date, epoch);
			checkLastResetDay(FlexiReset.DAILY, date, date);
			checkLastResetDay(FlexiReset.WEEKLY, date, getStartOfWeek(date));
			checkLastResetDay(FlexiReset.MONTHLY, date, date.withDayOfMonth(1));
			checkLastResetDay(FlexiReset.QUARTERLY, date, getMonthResetDayForInterval(date, 3));
			checkLastResetDay(FlexiReset.HALF_YEARLY, date, getMonthResetDayForInterval(date, 6));
			checkLastResetDay(FlexiReset.YEARLY, date, startOfEverything);
		}
	}

	private void checkLastResetDay(FlexiReset flexiReset, LocalDate fromDate, LocalDate expectedDate) {
		LocalDate actualDate = flexiReset.getLastResetDate(fromDate);
		assertWithMessage(flexiReset + ", " + fromDate)
				.that(actualDate)
				.isEqualTo(expectedDate);
	}

	private LocalDate getStartOfWeek(LocalDate dateTime) {
		LocalDate date = dateTime;
		while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
			date = date.minusDays(1);
		}
		return date;
	}

	/**
	 * Determines last reset date, for monthly resets, that occur at given intervals
	 * @param forDate date, from which to find most recent reset day-date
	 * @param interval number of months between reset dates. E.g. 6 means every 6 months.
	 * @return last reset date. E.g. when #interval is 3, and #forDate-s is 1.1., 2.2., 3.3., 4.4.,
	 * it will return 1.1., 1.1., 1.1., 4.1.
	 */
	private LocalDate getMonthResetDayForInterval(LocalDate forDate, int interval) {
		int monthIndex = forDate.getMonthValue() - 1;
		// Subtract and multiply, to lose decimals. If month is 8 and interval is 6, it becomes 6
		int resetMonthIndex = monthIndex / interval * interval;
		// Adjust back to 1-12 month values
		int month = resetMonthIndex + 1;
		return LocalDate.of(forDate.getYear(), month, 1);
	}

}
