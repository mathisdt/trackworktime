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

import java.util.HashMap;
import java.util.Map;

public enum Unit {
	NULL("null"),
	DAY("day"),
	WEEK("week"),
	MONTH("month"),
	YEAR("year");

	private final String name;

	private static final Map<String, Unit> nameToUnitMap;
	static {
		Unit[] units = values();
		nameToUnitMap = new HashMap<>(units.length);
		for(Unit u : units)
			nameToUnitMap.put(u.name, u);
	}

	Unit(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static Unit getByName(String name) {
		return nameToUnitMap.get(name);
	}

	public static boolean nameExists(String name) {
		return nameToUnitMap.containsKey(name);
	}

}
