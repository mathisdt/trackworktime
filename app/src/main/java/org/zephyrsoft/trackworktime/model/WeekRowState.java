package org.zephyrsoft.trackworktime.model;

import androidx.annotation.NonNull;

public class WeekRowState {

	@NonNull public String date, in, out, worked, flexi;
	{
		date = in = out = worked = flexi = "";
	}

	public boolean isHiglighted;

}
