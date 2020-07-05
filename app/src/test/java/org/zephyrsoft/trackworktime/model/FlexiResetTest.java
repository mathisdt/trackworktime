package org.zephyrsoft.trackworktime.model;

import org.junit.Test;

import java.util.TimeZone;

import hirondelle.date4j.DateTime;

import static com.google.common.truth.Truth.assertWithMessage;

public class FlexiResetTest {

	// Date, where day is start of the year, month, week
	private static final DateTime startOfEverything = DateTime.forDateOnly(2018, 1, 1).getStartOfDay();
	private static final DateTime endLoopDate = DateTime.forDateOnly(2019, 1, 1);

	@Test
	public void isResetDay() {
		for(DateTime date = startOfEverything; date.lt(endLoopDate); date=date.plusDays(1)) {
			int month = date.getMonth();
			int monthDay = date.getDay();
			boolean firstDayOfMonth = monthDay==1;

			checkIsResetDay(FlexiReset.NONE, date, false);
			checkIsResetDay(FlexiReset.DAILY, date, true);
			checkIsResetDay(FlexiReset.WEEKLY, date, date.getWeekDay()==2);
			checkIsResetDay(FlexiReset.MONTHLY, date, firstDayOfMonth);
			checkIsResetDay(FlexiReset.QUARTERLY, date, month%3==1 && firstDayOfMonth);
			checkIsResetDay(FlexiReset.HALF_YEARLY, date, month%6==1 && firstDayOfMonth);
			checkIsResetDay(FlexiReset.YEARLY, date, date.getDayOfYear()==1);
		}
	}

	private void checkIsResetDay(FlexiReset flexiReset, DateTime date, boolean expectedResetDay) {
		boolean actualResetDay = flexiReset.isResetDay(date);
		assertWithMessage(flexiReset + ", " + date)
				.that(actualResetDay)
				.isEqualTo(expectedResetDay);
	}

	@Test
	public void calcLastResetDayFromDay() {
		DateTime epoch = DateTime.forInstant(0, TimeZone.getTimeZone("UTC"));

		for(DateTime date = startOfEverything; date.lt(endLoopDate); date=date.plusDays(1)) {
			checkLastResetDay(FlexiReset.NONE, date, epoch);
			checkLastResetDay(FlexiReset.DAILY, date, date.getStartOfDay());
			checkLastResetDay(FlexiReset.WEEKLY, date, getStartOfWeek(date));
			checkLastResetDay(FlexiReset.MONTHLY, date, date.getStartOfMonth());
			checkLastResetDay(FlexiReset.QUARTERLY, date, getMonthResetDayForInterval(date, 3));
			checkLastResetDay(FlexiReset.HALF_YEARLY, date, getMonthResetDayForInterval(date, 6));
			checkLastResetDay(FlexiReset.YEARLY, date, startOfEverything);
		}
	}

	private void checkLastResetDay(FlexiReset flexiReset, DateTime fromDate, DateTime expectedDate) {
		DateTime actualDate = flexiReset.calcLastResetDayFromDay(fromDate);
		assertWithMessage(flexiReset + ", " + fromDate)
				.that(actualDate)
				.isEqualTo(expectedDate);
	}

	private DateTime getStartOfWeek(DateTime dateTime) {
		DateTime date = dateTime.getStartOfDay();
		while (date.getWeekDay() != 2) {
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
	private DateTime getMonthResetDayForInterval(DateTime forDate, int interval) {
		int monthIndex = forDate.getMonth() - 1;
		// Subtract and multiply, to lose decimals. If month is 8 and interval is 6, it becomes 6
		int resetMonthIndex = monthIndex / interval * interval;
		// Adjust back to 1-12 month values
		int month = resetMonthIndex + 1;
		return DateTime.forDateOnly(forDate.getYear(), month, 1).getStartOfDay();
	}

}