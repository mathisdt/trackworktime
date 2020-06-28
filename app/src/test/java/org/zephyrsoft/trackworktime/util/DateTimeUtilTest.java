package org.zephyrsoft.trackworktime.util;

import org.junit.Test;

import hirondelle.date4j.DateTime;

import static com.google.common.truth.Truth.assertThat;

public class DateTimeUtilTest {

	@Test
	public void parseTimeForToday() {
		DateTime toTest = DateTimeUtil.parseTimeForToday("");
		assertThat(toTest.getHour()).isEqualTo(0);
		assertThat(toTest.getMinute()).isEqualTo(0);
	}

}