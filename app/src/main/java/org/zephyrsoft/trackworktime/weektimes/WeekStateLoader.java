package org.zephyrsoft.trackworktime.weektimes;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import org.zephyrsoft.trackworktime.model.WeekState;

public class WeekStateLoader extends AsyncTask<Void, Void, WeekState> {

	private final WeekStateCalculator weekStateCalculator;
	private Consumer<WeekState> onWeekStateLoaded;

	public WeekStateLoader(@NonNull WeekStateCalculator weekStateCalculator,
			@NonNull Consumer<WeekState> onWeekStateLoaded) {
		this.weekStateCalculator = weekStateCalculator;
		this.onWeekStateLoaded = onWeekStateLoaded;
	}

	@Override protected WeekState doInBackground(Void... voids) {
		return weekStateCalculator.calculateWeekState();
	}

	@Override protected void onPostExecute(WeekState weekState) {
		super.onPostExecute(weekState);
		onWeekStateLoaded.accept(weekState);
	}

}
