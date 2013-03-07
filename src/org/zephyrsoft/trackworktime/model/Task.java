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

import org.zephyrsoft.trackworktime.database.DAO;

/**
 * Data class for a task.
 * 
 * @see DAO
 * @author Mathis Dirksen-Thedens
 */
public class Task extends Base implements Comparable<Task> {
	private Integer id = null;
	private String name = null;
	private Integer active = null;
	private Integer ordering = null;
	
	/**
	 * Constructor
	 */
	public Task() {
		// do nothing
	}
	
	/**
	 * Constructor
	 */
	public Task(Integer id, String name, Integer active, Integer ordering) {
		this.id = id;
		this.name = name;
		this.active = active;
		this.ordering = ordering;
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
	public String getName() {
		return name;
	}
	
	/**
	 * Getter
	 */
	public Integer getActive() {
		return active;
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
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Setter
	 */
	public void setActive(Integer active) {
		this.active = active;
	}
	
	/**
	 * Getter
	 */
	public Integer getOrdering() {
		return ordering;
	}
	
	/**
	 * Setter
	 */
	public void setOrdering(Integer ordering) {
		this.ordering = ordering;
	}
	
	@Override
	public int compareTo(Task another) {
		return compare(getName(), another.getName(), compare(getId(), another.getId(), 0));
	}
	
	/**
	 * This is used e.g. by an ArrayAdapter in a ListView and it is also useful for debugging.
	 * 
	 * @see org.zephyrsoft.trackworktime.model.Base#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}
}
