package org.zephyrsoft.trackworktime.options;

import org.junit.Test;

import static org.zephyrsoft.trackworktime.options.DataType.HOUR_MINUTE;

public class DataTypeTest {

	@Test
	public void hourMinute() {
		boolean valid = HOUR_MINUTE.validate("-1:55");
		valid &= HOUR_MINUTE.validate("1:00");
		valid &= HOUR_MINUTE.validate("1:29");
		valid &= HOUR_MINUTE.validate("37:30");
		if (!valid) {
			throw new AssertionError("HOUR_MINUTE");
		}
	}

}