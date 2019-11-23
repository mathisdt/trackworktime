package org.zephyrsoft.trackworktime.weektimes;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.timer.TimeCalculator;
import org.zephyrsoft.trackworktime.timer.TimerManager;

public class WeekStateCalculatorFactory {

	private final @NonNull Context context;
	private final @NonNull DAO dao;
	private final @NonNull TimerManager timerManager;
	private final @NonNull TimeCalculator timeCalculator;
	private final @NonNull SharedPreferences preferences;

	public WeekStateCalculatorFactory(@NonNull Context context, @NonNull DAO dao,
			@NonNull TimerManager timerManager, @NonNull TimeCalculator timeCalculator,
			@NonNull SharedPreferences preferences) {
		this.context = context;
		this.dao = dao;
		this.timerManager = timerManager;
		this.timeCalculator = timeCalculator;
		this.preferences = preferences;
	}

	public @NonNull WeekStateCalculator createForWeek(@NonNull Week week) {
		return new WeekStateCalculator(context, dao, timerManager, timeCalculator, preferences,
				week);
	}

}
