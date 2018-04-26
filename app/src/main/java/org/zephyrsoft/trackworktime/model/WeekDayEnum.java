/*
 * This file is part of TrackWorkTime (TWT).
 * 
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime.model;

import hirondelle.date4j.DateTime;

/**
 * Represents the days of the week.
 * 
 * @author Mathis Dirksen-Thedens
 */
public enum WeekDayEnum {

	SUNDAY(1, null), SATURDAY(7, SUNDAY), FRIDAY(6, SATURDAY), THURSDAY(5, FRIDAY), WEDNESDAY(4, THURSDAY), TUESDAY(3,
		WEDNESDAY), MONDAY(2, TUESDAY);

	private int value;
	private WeekDayEnum nextWeekDay;

	WeekDayEnum(int value, WeekDayEnum nextWeekDay) {
		this.value = value;
		this.nextWeekDay = nextWeekDay;
	}

	/** Get the value of this weekday for comparison with {@link DateTime#getWeekDay()}. */
	public int getValue() {
		return value;
	}

	/** Get the next week day. {@link WeekDayEnum#SUNDAY} has no next day. */
	public WeekDayEnum getNextWeekDay() {
		return nextWeekDay;
	}

	/**
	 * Fetch the right enum when you only have a {@link DateTime} object: use the construct
	 * {@code WeekDayEnum.getByValue(dateTime.getWeekDay())}
	 */
	public static WeekDayEnum getByValue(int value) {
		for (WeekDayEnum val : values()) {
			if (val.getValue() == value) {
				return val;
			}
		}
		return null;
	}
}
