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

/**
 * Data class for a task.
 * 
 * @see DAO
 */
public class Task extends Base implements Comparable<Task> {
	private Integer id = null;
	private String name = null;
	private Integer active = null;
	private Integer ordering = null;
	private Integer isDefault = null;

	public Task() {
		// do nothing
	}

	public Task(Integer id, String name, Integer active, Integer ordering, Integer isDefault) {
		this.id = id;
		this.name = name;
		this.active = active;
		this.ordering = ordering;
		this.isDefault = isDefault;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Integer getActive() {
		return active;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setActive(Integer active) {
		this.active = active;
	}

	public Integer getOrdering() {
		return ordering;
	}

	public void setOrdering(Integer ordering) {
		this.ordering = ordering;
	}

	public Integer getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Integer isDefault) {
		this.isDefault = isDefault;
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
		return getName() + (isDefault != null && isDefault.equals(1) ? " *" : "");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Task other = (Task) obj;

		if (id == null) {
			return other.id == null;
		} else {
			return id.equals(other.id);
		}
	}
}
