package org.zephyrsoft.trackworktime.model;

import androidx.annotation.NonNull;

public class WeekRowState {

	@NonNull private String label = "";
	@NonNull private String in = "";
	@NonNull private String out = "";
	@NonNull private String worked = "";
	@NonNull private String flexi = "";
	private boolean isHiglighted;

	@NonNull public String getLabel() {
		return label;
	}

	public void setLabel(@NonNull String label) {
		this.label = label;
	}

	@NonNull public String getIn() {
		return in;
	}

	public void setIn(@NonNull String in) {
		this.in = in;
	}

	@NonNull public String getOut() {
		return out;
	}

	public void setOut(@NonNull String out) {
		this.out = out;
	}

	@NonNull public String getWorked() {
		return worked;
	}

	public void setWorked(@NonNull String worked) {
		this.worked = worked;
	}

	@NonNull public String getFlexi() {
		return flexi;
	}

	public void setFlexi(@NonNull String flexi) {
		this.flexi = flexi;
	}

	public boolean isHiglighted() {
		return isHiglighted;
	}

	public void setHiglighted(boolean higlighted) {
		isHiglighted = higlighted;
	}

	@NonNull @Override public String toString() {
		return "values: " + getLabel() + ", " + getIn() + ", " + getOut() + ", " + getWorked() + ", " + getFlexi()
				+ ", highlighted: " + isHiglighted();
	}

}
