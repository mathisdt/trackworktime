package org.zephyrsoft.trackworktime.database;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.zephyrsoft.trackworktime.timer.TimerManager;

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
