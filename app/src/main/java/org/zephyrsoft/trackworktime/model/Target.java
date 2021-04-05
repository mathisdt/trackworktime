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

import org.threeten.bp.LocalDate;
import org.zephyrsoft.trackworktime.database.DAO;

import static org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE;

/**
 * Data class for an event.
 *
 * @author dliw
 * @see DAO
 */
public class Target extends Base implements Comparable<Target> {
	private Integer id = null;
	private Integer type = null;
	private Integer value = null;
	private LocalDate date = null;
	private String comment = null;

	public Target() {
		// do nothing
	}

	public Target(Integer id, Integer type, Integer value, LocalDate date, String comment) {
		this.id = id;
		this.type = type;
		this.value = value;
		this.date = date;
		this.comment = comment;
	}

	public Integer getId() {
		return id;
	}

	public Integer getType() {
		return type;
	}

	public Integer getValue() {
		return value;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public int compareTo(Target another) {
		return compare(getDate(), another.getDate(), compare(getId(), another.getId(), 0));
	}

	/**
	 * This is used e.g. by an ArrayAdapter in a ListView and it is also useful for debugging.
	 *
	 * @see Base#toString()
	 */
	@Override
	public String toString() {
		return date.format(ISO_LOCAL_DATE) + " / " + TargetEnum.byValue(getType()).name() + " / " + getValue() + " / " + getComment();
	}

}
