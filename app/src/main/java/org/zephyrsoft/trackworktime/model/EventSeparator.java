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

/**
 * Separator for event lists.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class EventSeparator extends Event {

	private final String caption;

	/**
	 * Create the new separator.
	 */
	public EventSeparator(String caption) {
		this.caption = caption;
	}

	@Override
	public Integer getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer getTask() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer getType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setId(Integer id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTask(Integer task) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setType(Integer type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getText() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setText(String text) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int compareTo(Event another) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return caption;
	}

}
