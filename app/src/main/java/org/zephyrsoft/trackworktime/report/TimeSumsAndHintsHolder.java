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
package org.zephyrsoft.trackworktime.report;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.zephyrsoft.trackworktime.model.TimeSum;

/**
 * Holds the data for reporting.
 */
public class TimeSumsAndHintsHolder implements Comparable<TimeSumsAndHintsHolder> {

	private String month;
	private String week;
	private String day;
	private String task;
	private String text;
	private TimeSum spent;

	public TimeSumsAndHintsHolder(String month, String week, String day, String task, String text, TimeSum spent) {
		this.month = month;
		this.week = week;
		this.day = day;
		this.task = task;
		this.text = text;
		this.spent = spent;
	}

	public static @NonNull TimeSumsAndHintsHolder createForDay(String day, String task, String text, TimeSum spent) {
		return new TimeSumsAndHintsHolder(null, null, day, task, text, spent);
	}

	public static @NonNull TimeSumsAndHintsHolder createForWeek(String week, String task, String text, TimeSum spent) {
		return new TimeSumsAndHintsHolder(null, week, null, task, text, spent);
	}

	public static @NonNull TimeSumsAndHintsHolder createForMonth(String month, String task, String text, TimeSum spent) {
		return new TimeSumsAndHintsHolder(month, null, null, task, text, spent);
	}

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public String getWeek() {
		return week;
	}

	public void setWeek(String week) {
		this.week = week;
	}

	public String getMonth() {
		return month;
	}

	public void setMonth(String month) {
		this.month = month;
	}

	public String getTask() {
		return task;
	}

	public void setTask(String task) {
		this.task = task;
	}

	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public TimeSum getSpent() {
		return spent;
	}

	public void setSpent(TimeSum spent) {
		this.spent = spent;
	}

	@Override
	public int compareTo(TimeSumsAndHintsHolder another) {
		if (another == null) {
			return 1;
		}
		return new CompareToBuilder()
			.append(getMonth(), another.getMonth())
			.append(getWeek(), another.getWeek())
			.append(getDay(), another.getDay())
			.append(getTask(), another.getTask())
			.append(getText(), another.getText())
			.toComparison();
	}

}
