package org.zephyrsoft.trackworktime.model;

import androidx.annotation.NonNull;

public class WeekRowState {

	@NonNull public String date = "";
	@NonNull public String in = "";
	@NonNull public String out = "";
	@NonNull public String worked = "";
	@NonNull public String flexi = "";
	public boolean isHiglighted;

	@NonNull @Override public String toString() {
		return "values: " + date + ", " + in + ", " + out + ", " + worked + ", " + flexi
				+ ", highlighted: " + isHiglighted;
	}

}
