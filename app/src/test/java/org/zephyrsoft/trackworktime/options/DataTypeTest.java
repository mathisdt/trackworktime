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