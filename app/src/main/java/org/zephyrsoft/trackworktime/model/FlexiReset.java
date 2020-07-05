package org.zephyrsoft.trackworktime.model;

import android.content.SharedPreferences;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.Constants;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

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

	public @NonNull DateTime calcLastResetDayFromDay(@NonNull DateTime fromDay) {
		switch(intervalUnit) {
			case NULL: return Constants.EPOCH;
			case DAY: return calcLastResetDayForDay(fromDay);
			case WEEK: return calcLastResetDayForWeek(fromDay);
			case MONTH: return calcLastResetDayForMonth(fromDay);
			default: throw new UnsupportedOperationException(intervalUnit.toString());
		}
	}

	private DateTime calcLastResetDayForDay(DateTime fromDay) {
		int daysDelta = getCountSinceLastResetDay(fromDay);
		return fromDay.minusDays(daysDelta).getStartOfDay();
	}

	private int getCountSinceLastResetDay(DateTime atDay) {
		int zeroBasedDayIndex = atDay.getDayOfYear() - 1;
		return zeroBasedDayIndex % intervalSize;
	}

	private DateTime calcLastResetDayForWeek(DateTime fromDay) {
		int weekDelta = getCountSinceLastResetWeek(fromDay);
		DateTime resetWeek = fromDay.minusDays(weekDelta * 7);
		return DateTimeUtil.getWeekStart(resetWeek);
	}

	private int getCountSinceLastResetWeek(DateTime atDay) {
		int zeroBasedDayIndex = atDay.getDayOfYear() - 1;
		return zeroBasedDayIndex % intervalSize;
	}

	private DateTime calcLastResetDayForMonth(DateTime fromDay) {
		int monthDelta = getCountSinceLastResetMonth(fromDay);
		DateTime resetMonth = DateTimeUtil.minusMonths(fromDay, monthDelta);
		return resetMonth.getStartOfMonth();
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
