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
 * Placeholder for a week which does not (yet) need to be persisted, probably because no event exists for this week.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class WeekPlaceholder extends Week {

	/**
	 * Constructor
	 */
	public WeekPlaceholder(String start) {
		super(null, start, null);
	}

	@Override
	public Integer getId() {
		throw new IllegalStateException("operation not permitted for a week placeholder");
	}

	@Override
	public void setId(Integer id) {
		throw new IllegalStateException("operation not permitted for a week placeholder");
	}

	@Override
	public Integer getSum() {
		return 0;
	}

	@Override
	public void setSum(Integer sum) {
		throw new IllegalStateException("operation not permitted for a week placeholder");
	}

	@Override
	public String toString() {
		return getStart() + " - NON-PERSISTENT PLACEHOLDER";
	}

	@Override
	public int compareTo(Week another) {
		return compare(getStart(), another.getStart(), 0);
	}

}
