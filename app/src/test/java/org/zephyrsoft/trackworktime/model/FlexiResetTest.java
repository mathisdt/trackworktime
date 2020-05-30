package org.zephyrsoft.trackworktime.model;

import org.junit.Test;

import hirondelle.date4j.DateTime;

import static com.google.common.truth.Truth.assertWithMessage;

public class FlexiResetTest {

	@Test
	public void isResetDay() {
		// Date, where day is start of the year, month, week
		DateTime startOfEverything = DateTime.forDateOnly(2018, 1, 1);
		DateTime endLoopDate = DateTime.forDateOnly(2019, 1, 1);

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

}