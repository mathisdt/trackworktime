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
package org.zephyrsoft.trackworktime.model;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeSumTest {

	private TimeSum underTest = new TimeSum();

	@BeforeClass
	public static void defaultState() {
		TimeSum timeSum = new TimeSum();
		assertEquals("Initial time sum", "0:00", timeSum.toString());
	}

	@BeforeClass
	public static void set() {
		TimeSum timeSum = new TimeSum();

		timeSum.set(0, 45);
		assertEquals("0:45", timeSum.toString());

		timeSum.set(-1, 45);
		assertEquals("-1:45", timeSum.toString());
	}

	@Test
	public void add() {
		add(0, 75);
		assertTime("1:15");

		add(2, 65);
		assertTime("4:20");
	}

	private void add(int h, int min) {
		underTest.add(h, min);
	}

	@Test
	public void substract() {
		set(4, 20);

		subtract(0, 140);
		assertTime("2:00");

		subtract(1, 75);
		assertTime("-0:15");

		subtract(1, 50);
		assertTime("-2:05");
	}

	private void subtract(int h, int min) {
		underTest.substract(h, min);
	}

	@Test
	public void addOrSustract() {
		set(-2, 5);

		TimeSum positive = new TimeSum();
		positive.set(2, 30);
		addOrSubtract(positive);
		assertTime("0:25");

		TimeSum negative = new TimeSum();
		negative.set(-1, 25);
		addOrSubtract(negative);
		assertTime("-1:00");
	}

	private void addOrSubtract(TimeSum other) {
		underTest.addOrSubstract(other);
	}

	@Test
	public void reset() {
		set(-1, 0);

		underTest.reset();
		assertTime("0:00");
	}

	private void assertTime(String expectedTime) {
		assertEquals(expectedTime, underTest.toString());
	}

	@Test
	public void getAsMinutes() {
		assertEquals(0, getAsMin());

		set(1, 30);
		assertEquals(90, getAsMin());

		set(-1, 30);
		assertEquals(-90, getAsMin());
	}

	private int getAsMin() {
		return underTest.getAsMinutes();
	}

	private void set(int h, int min) {
		underTest.set(h, min);
	}

}
