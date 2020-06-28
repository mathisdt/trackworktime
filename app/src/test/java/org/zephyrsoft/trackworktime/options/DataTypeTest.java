package org.zephyrsoft.trackworktime.options;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.zephyrsoft.trackworktime.options.DataType.HOUR_MINUTE;

public class DataTypeTest {

	@Test
	public void hourMinute() {
		assertTrue(HOUR_MINUTE.validate("-1:55"));
		assertTrue(HOUR_MINUTE.validate("1:00"));
		assertTrue(HOUR_MINUTE.validate("1:29"));
		assertTrue(HOUR_MINUTE.validate("37:30"));
	}

}