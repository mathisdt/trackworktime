package org.zephyrsoft.trackworktime.weektimes;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekState;

public class WeekStateLoaderFactory {

	private WeekStateCalculatorFactory weekStateCalculatorFactory;

	public WeekStateLoaderFactory(@NonNull WeekStateCalculatorFactory weekStateCalculatorFactory) {
		this.weekStateCalculatorFactory = weekStateCalculatorFactory;
	}

	public @NonNull WeekStateLoader create(@NonNull Week week,
			@NonNull Consumer<WeekState> onLoadedCallback) {
		WeekStateCalculator weekStateCalculator = weekStateCalculatorFactory.createForWeek(week);
		return new WeekStateLoader(weekStateCalculator, onLoadedCallback);
	}

}
