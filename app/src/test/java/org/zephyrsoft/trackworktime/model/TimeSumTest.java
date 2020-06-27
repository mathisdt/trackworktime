package org.zephyrsoft.trackworktime.model;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeSumTest {

	private TimeSum underTest = new TimeSum();

	@BeforeClass
	public static void defaultState() {
		TimeSum timeSum = new TimeSum();
		assertEquals("Initial time sum", timeSum.toString(), "0:00");
	}

	@BeforeClass
	public static void set() {
		TimeSum timeSum = new TimeSum();

		timeSum.set(0, 45);
		assertEquals(timeSum.toString(), "0:45");

		timeSum.set(-1, 45);
		assertEquals(timeSum.toString(), "-1:45");
	}

	@Test
	public void add() {
		underTest.add(0, 75);
		assertEquals(underTest.toString(), "1:15");

		underTest.add(2, 65);
		assertEquals(underTest.toString(), "4:20");
	}

	@Test
	public void substract() {
		underTest.set(4, 20);

		underTest.substract(0, 140);
		assertEquals(underTest.toString(), "2:00");

		underTest.substract(1, 75);
		assertEquals(underTest.toString(), "-0:15");

		underTest.substract(1, 50);
		assertEquals(underTest.toString(), "-2:05");
	}

	@Test
	public void addOrSustract() {
		underTest.set(-2, 5);

		TimeSum positive = new TimeSum();
		positive.set(2, 30);
		assertEquals(positive.toString(), "2:30");
		underTest.addOrSubstract(positive);
		assertEquals(underTest.toString(), "0:25");

		TimeSum negative = new TimeSum();
		negative.set(-1, 25);
		assertEquals(negative.toString(), "-1:25");
		underTest.addOrSubstract(negative);
		assertEquals(underTest.toString(), "-1:00");
	}

	@Test
	public void reset() {
		underTest.set(-1, 0);

		underTest.reset();
		assertEquals(underTest.toString(), "0:00");
	}

	@Test
	public void getAsMinutes() {
		assertEquals(underTest.getAsMinutes(), 0);

		underTest.set(1, 30);
		assertEquals(underTest.getAsMinutes(), 90);

		underTest.set(-1, 30);
		assertEquals(underTest.getAsMinutes(), -90);
	}

}
