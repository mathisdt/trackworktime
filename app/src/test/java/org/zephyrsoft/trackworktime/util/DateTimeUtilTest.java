package org.zephyrsoft.trackworktime.util;

import org.junit.Test;
import org.threeten.bp.LocalTime;

import static com.google.common.truth.Truth.assertThat;

public class DateTimeUtilTest {

	@Test
	public void parseTimeForToday() {
		LocalTime toTest = DateTimeUtil.parseTime("");
		assertThat(toTest.getHour()).isEqualTo(0);
		assertThat(toTest.getMinute()).isEqualTo(0);
	}

}