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

import java.util.Arrays;
import java.util.List;

/**
 * The possible event types - clock in or clock out.
 *
 * @author Mathis Dirksen-Thedens
 */
public enum TypeEnum {

	/**
	 * clock-in type of event
	 */
	CLOCK_IN(Values.CLOCK_IN_VALUE, "in"),
	/**
	 * clock-out type of event
	 */
	CLOCK_OUT(Values.CLOCK_OUT_VALUE, "out"),
	/**
	 * clock-out now type of event used to display correct amount of worked time on current day when currently clocked
	 * in - THIS TYPE NEVER COMES FROM THE DATABASE
	 */
	CLOCK_OUT_NOW(Values.CLOCK_OUT_NOW_VALUE, "out (current time)"),
    /**
     * flex type defines a individual flex time for this day
     */
    FLEX(Values.FLEX_VALUE, "flex");

	private Integer value = null;
	private String readableName = null;

	TypeEnum(Integer value, String readableName) {
		this.value = value;
		this.readableName = readableName;
	}

	/**
	 * Gets the value of this enum for storing it in database.
	 */
	public Integer getValue() {
		return value;
	}

	public String getReadableName() {
		return readableName;
	}

	@Override
	public String toString() {
		return String.valueOf(getValue());
	}

	/**
	 * Gets the "default" types, meaning those that get saved. At the moment, only CLOCK_IN and CLOCK_OUT make sense
	 * here.
	 */
	public static List<TypeEnum> getDefaultTypes() {
		return Arrays.asList(CLOCK_IN, CLOCK_OUT);
	}

	/**
	 * Get the enum for a specific value.
	 *
	 * @param value
	 *            the value
	 * @return the corresponding enum
	 */
	public static TypeEnum byValue(Integer value) {
		if (value == null) {
			return null;
		} else if (value == Values.CLOCK_IN_VALUE) {
			return CLOCK_IN;
		} else if (value == Values.CLOCK_OUT_VALUE) {
			return CLOCK_OUT;
		} else if (value == Values.CLOCK_OUT_NOW_VALUE) {
			return CLOCK_OUT_NOW;
        } else if (value == Values.FLEX_VALUE) {
            return FLEX;
		} else {
			throw new IllegalArgumentException("unknown value");
		}
	}

	private static class Values {
		private static final int CLOCK_IN_VALUE = 1;
		private static final int CLOCK_OUT_VALUE = 0;
		private static final int FLEX_VALUE = 2;
		private static final int CLOCK_OUT_NOW_VALUE = -1;
	}
}
