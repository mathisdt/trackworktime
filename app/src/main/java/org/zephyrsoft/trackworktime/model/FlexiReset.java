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
	HALF_YEARLY(6, Unit.MONTH, "half-yearly"),
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
			case MONTH: return isResetDayForMonth(day);
			default: throw new UnsupportedOperationException(intervalUnit.toString());
		}
	}

	private boolean isResetDayForDay(DateTime day) {
		return getCountSinceLastResetDay(day) == 0;
	}

	private boolean isResetDayForWeek(DateTime day) {
		boolean isFirstDay = day.getWeekDay() == 2;
		boolean isCorrectWeek = getCountSinceLastResetWeek(day) == 0;
		return isFirstDay && isCorrectWeek;
	}

	private boolean isResetDayForMonth(DateTime day) {
		boolean isFirstDay = day.getStartOfMonth().isSameDayAs(day);
		boolean isCorrectMonth = getCountSinceLastResetMonth(day) == 0;
		return isFirstDay && isCorrectMonth;
	}

	private int getCountSinceLastResetDay(DateTime atDay) {
		int zeroBasedDayIndex = atDay.getDayOfYear() - 1;
		return zeroBasedDayIndex % intervalSize;
	}

	private int getCountSinceLastResetWeek(DateTime atDay) {
		int zeroBasedDayIndex = atDay.getDayOfYear() - 1;
		return zeroBasedDayIndex % intervalSize;
	}

	private int getCountSinceLastResetMonth(DateTime atDay) {
		int zeroBasedMonthIndex = atDay.getMonth() - 1;
		return zeroBasedMonthIndex % intervalSize;
	}

	public static FlexiReset loadFromPreferences(SharedPreferences preferences) {
		String key = Key.FLEXI_TIME_RESET_INTERVAL.getName();
		String defaultValue = FlexiReset.NONE.name();
		String string = preferences.getString(key, defaultValue);
		return valueOf(string);
	}

}
