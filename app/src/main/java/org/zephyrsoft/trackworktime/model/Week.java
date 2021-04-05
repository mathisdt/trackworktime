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

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;

/**
 * Data class for a week.
 *
 * @author Mathis Dirksen-Thedens
 */
public class Week extends Base implements Comparable<Week> {
	private final LocalDate startDay;

	public Week(LocalDate date) {
		// TODO consider locale
		startDay = date.with(DayOfWeek.MONDAY);
	}

	public Week(long epochDay) {
		startDay = LocalDate.ofEpochDay(epochDay);
	}

	public LocalDate getStart() {
		return startDay;
	}

	public LocalDate getEnd() {
		// // TODO consider locale
		return startDay.with(DayOfWeek.SUNDAY);
	}

	public long toEpochDay() {
		return startDay.toEpochDay();
	}

	public Week plusWeeks(long weeksToAdd) {
		return new Week(startDay.plusWeeks(weeksToAdd));
	}

	public boolean isInWeek(LocalDate date) {
		return !date.isBefore(startDay) && !date.isAfter(getEnd());
	}

	@Override
	public int compareTo(Week another) {
		//return compare(getStart(), another.getStart(), compare(getId(), another.getId(), 0));
		return compare(getStart(), another.getStart(), 0);
	}

	/**
	 * This is used e.g. by an ArrayAdapter in a ListView and it is also useful for debugging.
	 *
	 * @see Base#toString()
	 */
	@Override
	public String toString() {
		return startDay.toString();
	}
}
