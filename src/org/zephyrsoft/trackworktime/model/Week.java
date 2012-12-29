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
package org.zephyrsoft.trackworktime.model;

import org.zephyrsoft.trackworktime.database.DAO;

/**
 * Data class for a week.
 * 
 * @see DAO
 * @author Mathis Dirksen-Thedens
 */
public class Week extends Base implements Comparable<Week> {
	private Integer id = null;
	private String start = null;
	/** amount of minutes worked in this week */
	private Integer sum = null;
	
	/**
	 * Constructor
	 */
	public Week() {
		// do nothing
	}
	
	/**
	 * Constructor
	 */
	public Week(Integer id, String start, Integer sum) {
		this.id = id;
		this.start = start;
		this.sum = sum;
	}
	
	/**
	 * Getter
	 */
	public Integer getId() {
		return id;
	}
	
	/**
	 * Getter
	 */
	public String getStart() {
		return start;
	}
	
	/**
	 * Getter
	 */
	public Integer getSum() {
		return sum;
	}
	
	/**
	 * Setter
	 */
	public void setId(Integer id) {
		this.id = id;
	}
	
	/**
	 * Setter
	 */
	public void setStart(String start) {
		this.start = start;
	}
	
	/**
	 * Setter
	 */
	public void setSum(Integer sum) {
		this.sum = sum;
	}
	
	@Override
	public int compareTo(Week another) {
		return compare(getStart(), another.getStart(), compare(getId(), another.getId(), 0));
	}
	
	/**
	 * This is used e.g. by an ArrayAdapter in a ListView and it is also useful for debugging.
	 * 
	 * @see org.zephyrsoft.trackworktime.model.Base#toString()
	 */
	@Override
	public String toString() {
		return getStart() + " - " + getSum();
	}
}
