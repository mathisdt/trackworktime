/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.model.WeekDayEnum;

/**
 * Utilities for handling week days.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class WeekDayHelper {
	
	/**
	 * Get the complete name of the week day indicated by the given date.
	 */
	public static String getWeekDayLongName(DateTime date) {
		WeekDayEnum weekDay = WeekDayEnum.getByValue(date.getWeekDay());
		switch (weekDay) {
			case MONDAY:
				return Basics.getInstance().getContext().getText(R.string.mondayLong).toString();
			case TUESDAY:
				return Basics.getInstance().getContext().getText(R.string.tuesdayLong).toString();
			case WEDNESDAY:
				return Basics.getInstance().getContext().getText(R.string.wednesdayLong).toString();
			case THURSDAY:
				return Basics.getInstance().getContext().getText(R.string.thursdayLong).toString();
			case FRIDAY:
				return Basics.getInstance().getContext().getText(R.string.fridayLong).toString();
			case SATURDAY:
				return Basics.getInstance().getContext().getText(R.string.saturdayLong).toString();
			case SUNDAY:
				return Basics.getInstance().getContext().getText(R.string.sundayLong).toString();
			default:
				throw new IllegalStateException("unknown weekday");
		}
	}
	
}
