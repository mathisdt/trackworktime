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

import androidx.annotation.NonNull;

/**
 * The possible event types - clock in or clock out.
 *
 * @author Mathis Dirksen-Thedens
 */
public enum TargetEnum {

	/**
	 * set day target
	 */
	DAY_SET(Values.DAY_SET, "day-set"),

	/**
	 * grant day target
	 */
	DAY_GRANT(Values.DAY_GRANT, "day-grant"),

	/**
	 * ignore day target
	 */
	DAY_IGNORE(Values.DAY_IGNORE, "day-ignore"),

	/**
	 * flexi set target
	 */
	FLEXI_SET(Values.FLEXI_SET, "flexi-set"),

	/**
	 * flexi add target
	 */
	FLEXI_ADD(Values.FLEXI_ADD, "flexi-add");

	private final Integer value;
	private final String readableName;

	TargetEnum(Integer value, String readableName) {
		this.value = value;
		this.readableName = readableName;
	}

	/**
	 * Gets the value of this enum for storing it in database.
	 */
	public Integer getValue() {
		return value;
	}

	@NonNull @Override
	public String toString() {
		return (readableName == null) ? String.valueOf(getValue()) : readableName;
	}

	/**
	 * Get the enum for a specific value.
	 *
	 * @param value the value
	 * @return the corresponding enum
	 */
	public static TargetEnum byValue(Integer value) {
		if (value == null) {
			return null;
		} else if (value == Values.DAY_SET) {
			return DAY_SET;
		} else if (value == Values.DAY_GRANT) {
			return DAY_GRANT;
		} else if (value == Values.DAY_IGNORE) {
			return DAY_IGNORE;
		} else if (value == Values.FLEXI_SET) {
			return FLEXI_SET;
		} else if (value == Values.FLEXI_ADD) {
			return FLEXI_ADD;
		} else {
			throw new IllegalArgumentException("unknown value");
		}
	}

	/**
	 * Get the enum for a specific name.
	 *
	 * @param name the value
	 * @return the corresponding enum
	 */
	public static TargetEnum byName(String name) {
		for (TargetEnum te : TargetEnum.values()) {
			if (te.readableName.equalsIgnoreCase(name)) {
				return te;
			}
		}

		throw new IllegalArgumentException("unknown value");
	}

	private static class Values {
		private static final int DAY_SET = 0;
		private static final int DAY_GRANT = 1;
		private static final int DAY_IGNORE = 2;
		private static final int FLEXI_SET = 5;
		private static final int FLEXI_ADD = 6;
	}
}
