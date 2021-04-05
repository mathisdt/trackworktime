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

public class TimeInfo {
	private long actual = 0;
	private long target = 0;

	public Long getActual() {
		return actual;
	}

	public void setActual(long actual) {
		this.actual = actual;
	}

	public Long getTarget() {
		return target;
	}

	public void setTarget(long target) {
		this.target = target;
	}

	public Long getBalance() {
		return actual - target;
	}
}
