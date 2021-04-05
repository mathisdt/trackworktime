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

import androidx.annotation.NonNull;

import org.threeten.bp.DayOfWeek;

public class WeekState {
	
	public static class DayRowState {
		public String label = "";
		public boolean labelHighlighted = false;
		public String in = "";
		public String out = "";
		public String worked = "";
		public boolean workedHighlighted = false;
		public String flexi = "";

		public boolean highlighted = false;

		@NonNull @Override
		public String toString() {
			return "values: " + label + ", " + in + ", " + out + ", " + worked + ", " + flexi
					+ ", highlighted: " + highlighted;
		}
	}
	
	public static class SummaryRowState {
		public String label = "";
		public String worked = "";
		public String flexi = "";

		@NonNull @Override
		public String toString() {
			return "values: " + label + ", " + worked + ", " + flexi;
		}
	}
	
	public String topLeftCorner = "";
	public final SummaryRowState totals = new SummaryRowState();

	private final DayRowState[] dayRowStates = {
			new DayRowState(), new DayRowState(), new DayRowState(), new DayRowState(),
			new DayRowState(), new DayRowState(), new DayRowState()
	};
	
	public DayRowState getRowForDay(DayOfWeek dayOfWeek) {
		return dayRowStates[dayOfWeek.ordinal()];
	}

	@NonNull @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(topLeftCorner); sb.append("\n");

		for (DayOfWeek day : DayOfWeek.values()) {
			sb.append(getRowForDay(day).toString());
			sb.append("\n");
		}

		sb.append(totals); sb.append("\n");

		return sb.toString();
	}
}
