package org.zephyrsoft.trackworktime.model;

import androidx.annotation.NonNull;

public class WeekState {

	@NonNull public WeekRowState header = new WeekRowState();
	@NonNull public WeekRowState monday = new WeekRowState();
	@NonNull public WeekRowState tuesday = new WeekRowState();
	@NonNull public WeekRowState wednesday = new WeekRowState();
	@NonNull public WeekRowState thursday = new WeekRowState();
	@NonNull public WeekRowState friday = new WeekRowState();
	@NonNull public WeekRowState saturday = new WeekRowState();
	@NonNull public WeekRowState sunday = new WeekRowState();
	@NonNull public WeekRowState totals = new WeekRowState();

	@NonNull @Override public String toString() {
		return header + "\n" +
				monday + "\n" +
				tuesday + "\n" +
				wednesday + "\n" +
				thursday + "\n" +
				friday + "\n" +
				saturday + "\n" +
				sunday + "\n" +
				totals;
	}

}
