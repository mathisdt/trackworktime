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

import org.zephyrsoft.trackworktime.database.DAO;

import java.time.LocalDate;

/**
 * Data class for an event.
 *
 * @see DAO
 */
public class CalcCacheEntry extends Base implements Comparable<CalcCacheEntry> {
	private LocalDate date = null;
	private Long worked = null;
	private Long target = null;

	public CalcCacheEntry() {
		// do nothing
	}

	public CalcCacheEntry(LocalDate date, Long worked, Long target) {
		this.date = date;
		this.worked = worked;
		this.target = target;
	}

	public Long getDateAsId() {
		return this.date.toEpochDay();
	}

	public LocalDate getDate() {
		return this.date;
	}

	public Long getWorked() {
		return worked;
	}

	public Long getTarget() {
		return target;
	}

	public void setDateFromId(Long id) {
		this.date = LocalDate.ofEpochDay(id);
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public void setWorked(Long worked) {
		this.worked = worked;
	}

	public void setTarget(Long target) {
		this.target = target;
	}

	@Override
	public int compareTo(CalcCacheEntry another) {
		return compare(getDate(), another.getDate(), compare(getDateAsId(), another.getDateAsId(), 0));
	}

	/**
	 * This is used e.g. by an ArrayAdapter in a ListView and it is also useful for debugging.
	 *
	 * @see Base#toString()
	 */
	@Override
	public String toString() {
		return date.toString() + " / " + getWorked() + " / " + getTarget();
	}

}
