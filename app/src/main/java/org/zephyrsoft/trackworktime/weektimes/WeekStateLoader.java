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
package org.zephyrsoft.trackworktime.weektimes;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import org.zephyrsoft.trackworktime.model.WeekState;

public class WeekStateLoader extends AsyncTask<Void, Void, WeekState> {

	private final WeekStateCalculator weekStateCalculator;
	private final Consumer<WeekState> onWeekStateLoaded;

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
