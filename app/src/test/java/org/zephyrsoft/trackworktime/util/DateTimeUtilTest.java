package org.zephyrsoft.trackworktime.util;

import org.junit.Test;

import hirondelle.date4j.DateTime;

public class DateTimeUtilTest {

	@Test
	public static void parseTimeForToday() {
		boolean valid = false;
		try {
			DateTime toTest = DateTimeUtil.parseTimeForToday("");
			valid = toTest != null
					&& toTest.getHour() == 0
					&& toTest.getMinute() == 0;
		} catch (Exception e) {
			throw new AssertionError(": " + e.getMessage());
		}
		if (!valid) {
			throw new AssertionError("parseTimeForToday");
		}
	}

}