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

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Data class for an event.
 * 
 * @see DAO
 */
public class Event extends Base implements Comparable<Event> {
	private Integer id = null;
	private Integer task = null;
	private Integer type = null;
	private OffsetDateTime time = null;
	private String text = null;

	public Event() {
		// do nothing
	}

	public Event(Integer id, Integer task, Integer type, OffsetDateTime time, String text) {
		this.id = id;
		this.task = task;
		this.type = type;
		this.time = time;
		this.text = text;
	}

	public Integer getId() {
		return id;
	}

	public Integer getTask() {
		return task;
	}

	public Integer getType() {
		return type;
	}

	public TypeEnum getTypeEnum() {
		return TypeEnum.byValue(type);
	}

	// used for report generation
	public OffsetDateTime getTime() {
		return time;
	}

	public OffsetDateTime getDateTime() {
		return time;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setTask(Integer task) {
		this.task = task;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public void setDateTime(OffsetDateTime datetime) {
		this.time = datetime;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public int compareTo(Event another) {
		return compare(getDateTime(), another.getDateTime(), compare(getId(), another.getId(), 0));
	}

	/**
	 * This is used e.g. by an ArrayAdapter in a ListView and it is also useful for debugging.
	 * 
	 * @see org.zephyrsoft.trackworktime.model.Base#toString()
	 */
	@Override
	public String toString() {
		return getDateTime() + " / " + TypeEnum.byValue(getType()).name() + " / " + getTask() + " - " + getText();
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;
		Event event = (Event) o;
		return Objects.equals(id, event.id) &&
				Objects.equals(task, event.task) &&
				Objects.equals(type, event.type) &&
				Objects.equals(time, event.time) &&
				Objects.equals(text, event.text);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, task, type, time, text);
	}
}
