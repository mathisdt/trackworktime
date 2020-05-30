package org.zephyrsoft.trackworktime.model;

import org.junit.Test;

import hirondelle.date4j.DateTime;

import static com.google.common.truth.Truth.assertWithMessage;

public class FlexiResetTest {

	@Test
	public void isResetDay() {
		// Date, where day is start of the year, month, week
		DateTime startOfEverything = DateTime.forDateOnly(2018, 1, 1);
		// Date, where day is not start of the year, month or week
		DateTime startOfNothing = DateTime.forDateOnly(2018, 2, 2);

		checkIsResetDay(FlexiReset.NONE, startOfEverything, false);
		checkIsResetDay(FlexiReset.NONE, startOfNothing, false);

		checkIsResetDay(FlexiReset.DAILY, startOfEverything, true);
		checkIsResetDay(FlexiReset.DAILY, startOfNothing, true);

		checkIsResetDay(FlexiReset.WEEKLY, startOfEverything, true);
		checkIsResetDay(FlexiReset.WEEKLY, startOfNothing, false);

		checkIsResetDay(FlexiReset.MONTHLY, startOfEverything, true);
		checkIsResetDay(FlexiReset.MONTHLY, startOfNothing, false);

		checkIsResetDay(FlexiReset.QUARTERLY, startOfEverything, true);
		checkIsResetDay(FlexiReset.QUARTERLY, startOfNothing, false);

		checkIsResetDay(FlexiReset.HALF_YEARLY, startOfEverything, true);
		checkIsResetDay(FlexiReset.HALF_YEARLY, startOfNothing, false);

		checkIsResetDay(FlexiReset.YEARLY, startOfEverything, true);
		checkIsResetDay(FlexiReset.YEARLY, startOfNothing, false);
	}

	private void checkIsResetDay(FlexiReset flexiReset, DateTime date, boolean expectedResetDay) {
		boolean actualResetDay = flexiReset.isResetDay(date);
		assertWithMessage(flexiReset + ", " + date)
				.that(actualResetDay)
				.isEqualTo(expectedResetDay);
	}

}