package org.zephyrsoft.trackworktime.model;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.acra.util.Predicate;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import hirondelle.date4j.DateTime;

public enum FlexiReset {
	NONE(Unit.NULL, "none", dateTime -> false),
	DAILY(Unit.DAY, "daily", dateTime -> true),
	WEEKLY(Unit.WEEK, "weekly", dateTime -> DateTimeUtil.getWeekStart(dateTime).isSameDayAs(dateTime)),
	MONTHLY(Unit.MONTH, "monthly", dateTime -> dateTime.getStartOfMonth().isSameDayAs(dateTime)),
	YEARLY(Unit.YEAR, "yearly", dateTime -> dateTime.getDayOfYear()==1);

	private final Unit interval;
	private final String friendlyName;
	private final Predicate<DateTime> resetDayEvaluator;

	FlexiReset(@NonNull Unit unit, @NonNull String friendlyName,
			@NonNull Predicate<DateTime> resetDayEvaluator) {
		this.interval = unit;
		this.friendlyName = friendlyName;
		this.resetDayEvaluator = resetDayEvaluator;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public boolean isResetDay(DateTime day) {
		return resetDayEvaluator.apply(day);
	}

	public String getIntervalPreferenceValue() {
		return interval.getName();
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
			if(flexiReset.interval.equals(unit)) {
				return flexiReset;
			}
		}
		throw new IllegalArgumentException("Not defined for provided interval unit: " + unit);
	}

}
