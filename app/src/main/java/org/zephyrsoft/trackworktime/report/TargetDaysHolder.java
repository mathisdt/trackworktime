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

/**
 * Holds the data for reporting.
 */
public class TargetDaysHolder implements Comparable<TargetDaysHolder> {

	private String month;
	private String week;
	private String day;
	private String target;
	private Integer days;

	public TargetDaysHolder(String month, String week, String day, String target, Integer days) {
		this.month = month;
		this.week = week;
		this.day = day;
		this.target = target;
		this.days = days;
	}

	public static @NonNull
    TargetDaysHolder createForDay(String day, String target, Integer days) {
		return new TargetDaysHolder(null, null, day, target, days);
	}

	public static @NonNull
    TargetDaysHolder createForWeek(String week, String target, Integer days) {
		return new TargetDaysHolder(null, week, null, target, days);
	}

	public static @NonNull
    TargetDaysHolder createForMonth(String month, String target, Integer days) {
		return new TargetDaysHolder(month, null, null, target, days);
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

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public Integer getDays() {
		return days;
	}

	public void setDays(Integer days) {
		this.days = days;
	}

	@Override
	public int compareTo(TargetDaysHolder another) {
		if (another == null) {
			return 1;
		}
		return new CompareToBuilder()
			.append(getMonth(), another.getMonth())
			.append(getWeek(), another.getWeek())
			.append(getDay(), another.getDay())
			.append(getTarget(), another.getTarget())
			.toComparison();
	}

}
