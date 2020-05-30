package org.zephyrsoft.trackworktime.model;

import android.content.SharedPreferences;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.options.Key;

import hirondelle.date4j.DateTime;

public enum FlexiReset {
	NONE(1, Unit.NULL, "none"),
	DAILY(1, Unit.DAY, "daily"),
	WEEKLY(1, Unit.WEEK, "weekly"),
	MONTHLY(1, Unit.MONTH, "monthly"),
	QUARTERLY(3, Unit.MONTH, "quarterly"),
	YEARLY(12, Unit.MONTH, "yearly");

	private final int intervalSize;
	private final Unit intervalUnit;
	private final String friendlyName;

	FlexiReset(@IntRange(from=1) int intervalSize, @NonNull Unit intervalUnit,
			@NonNull String friendlyName) {
		this.intervalSize = intervalSize;
		this.intervalUnit = intervalUnit;
		this.friendlyName = friendlyName;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public boolean isResetDay(DateTime day) {
		switch(intervalUnit) {
			case NULL: return false;
			case DAY: return isResetDayForDay(day);
			case WEEK: return isResetDayForWeek(day);
			case MONTH: return isResetDayMonth(day);
			default: throw new UnsupportedOperationException(intervalUnit.toString());
		}
	}

	public boolean isResetDayForDay(DateTime day) {
		int zeroBasedDayIndex = day.getDayOfYear() - 1;
		return zeroBasedDayIndex % intervalSize == 0;
	}

	public boolean isResetDayForWeek(DateTime day) {
		boolean isFirstDay = day.getWeekDay() == 2;
		int zeroBasedWeekIndex = day.getWeekIndex() - 1;
		boolean isCorrectWeek = zeroBasedWeekIndex % intervalSize == 0;
		return isFirstDay && isCorrectWeek;
	}

	public boolean isResetDayMonth(DateTime day) {
		boolean isFirstDay = day.getStartOfMonth().isSameDayAs(day);
		int zeroBasedMonthIndex = day.getMonth() - 1;
		boolean isCorrectMonth = zeroBasedMonthIndex % intervalSize == 0;
		return isFirstDay && isCorrectMonth;
	}

	public String getIntervalPreferenceValue() {
		return intervalUnit.getName();
	}

	public static FlexiReset loadFromPreferences(SharedPreferences preferences) {
		Unit unit = loadUnitFromPreferences(preferences);
		return FlexiReset.getByUnit(unit);
	}

	private static Unit loadUnitFromPreferences(SharedPreferences preferences) {
		String key = Key.FLEXI_TIME_RESET_INTERVAL.getName();
		String defaultValue = FlexiReset.NONE.getIntervalPreferenceValue();
		String string = preferences.getString(key, defaultValue);
		return Unit.getByName(string);
	}

	public static FlexiReset getByUnit(@NonNull Unit unit) {
		for(FlexiReset flexiReset : values()) {
			if(flexiReset.intervalUnit.equals(unit)) {
				return flexiReset;
			}
		}
		throw new IllegalArgumentException("Not defined for provided interval unit: " + unit);
	}

}
