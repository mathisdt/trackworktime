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
