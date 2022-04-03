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
package org.zephyrsoft.trackworktime.database;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zephyrsoft.trackworktime.timer.TimerManager;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class DAOTest {

	private static final String OFFSET_AS_STRING = "+01:00";

	private ZoneOffset offset;
	private TimerManager timerManager;

	@Before
	public void setup() {
		offset = ZoneOffset.of(OFFSET_AS_STRING);

		timerManager = new TimerManager(null, null, null) {
			@Override
			public ZoneId getHomeTimeZone() {
				return offset;
			}
		};
	}

	@Test
	public void parseOldDates() {
		Assert.assertEquals(OffsetDateTime.of(2021, 3, 1, 10, 5, 30, 0, offset),
			DAO.parseOffsetDateTime(timerManager, "2021-03-01 10:05:30.000000000"));
		Assert.assertEquals(OffsetDateTime.of(2021, 3, 1, 10, 5, 30, 0, offset),
			DAO.parseOffsetDateTime(timerManager, "2021-03-01 10:05:30.0000"));
		Assert.assertEquals(OffsetDateTime.of(2021, 3, 1, 10, 5, 30, 0, offset),
			DAO.parseOffsetDateTime(timerManager, "2021-03-01 10:05:30"));
		Assert.assertEquals(OffsetDateTime.of(2021, 3, 1, 10, 5, 0, 0, offset),
			DAO.parseOffsetDateTime(timerManager, "2021-03-01 10:05"));
	}

	@Test
	public void parseOffsetDates() {
		Assert.assertEquals(OffsetDateTime.of(2021, 3, 1, 10, 5, 30, 0, offset),
			DAO.parseOffsetDateTime(timerManager, "2021-03-01T10:05:30.000000000" + OFFSET_AS_STRING));
		Assert.assertEquals(OffsetDateTime.of(2021, 3, 1, 10, 5, 30, 0, offset),
			DAO.parseOffsetDateTime(timerManager, "2021-03-01T10:05:30.000000000"));
		Assert.assertEquals(OffsetDateTime.of(2021, 3, 1, 10, 5, 30, 0, offset),
			DAO.parseOffsetDateTime(timerManager, "2021-03-01T10:05:30.0000" + OFFSET_AS_STRING));
		Assert.assertEquals(OffsetDateTime.of(2021, 3, 1, 10, 5, 30, 0, offset),
			DAO.parseOffsetDateTime(timerManager, "2021-03-01T10:05:30.0000"));
		Assert.assertEquals(OffsetDateTime.of(2021, 3, 1, 10, 5, 30, 0, offset),
			DAO.parseOffsetDateTime(timerManager, "2021-03-01T10:05:30" + OFFSET_AS_STRING));
		Assert.assertEquals(OffsetDateTime.of(2021, 3, 1, 10, 5, 30, 0, offset),
			DAO.parseOffsetDateTime(timerManager, "2021-03-01T10:05:30"));
		Assert.assertEquals(OffsetDateTime.of(2021, 3, 1, 10, 5, 0, 0, offset),
			DAO.parseOffsetDateTime(timerManager, "2021-03-01T10:05" + OFFSET_AS_STRING));
		Assert.assertEquals(OffsetDateTime.of(2021, 3, 1, 10, 5, 0, 0, offset),
			DAO.parseOffsetDateTime(timerManager, "2021-03-01T10:05"));
	}

}
