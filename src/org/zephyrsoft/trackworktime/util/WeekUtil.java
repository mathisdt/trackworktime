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
package org.zephyrsoft.trackworktime.util;

import hirondelle.date4j.DateTime;
import org.zephyrsoft.trackworktime.model.Week;

/**
 * Utility methods for handling {@link Week}s.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class WeekUtil {
	
	/**
	 * Check if a date is in a specific week.
	 * 
	 * @param date the date to check
	 * @param week the referenced week
	 * @return {@code true} if the date is inside the week, {@code false} otherwise
	 */
	public static boolean isDateInWeek(DateTime date, Week week) {
		DateTime weekStart = DateTimeUtil.stringToDateTime(week.getStart());
		DateTime weekEnd = weekStart.plusDays(7);
		return weekStart.lteq(date) && weekEnd.gt(date);
	}
	
}
