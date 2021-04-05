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

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekState;

public class WeekStateLoaderFactory {

	private final WeekStateCalculatorFactory weekStateCalculatorFactory;

	public WeekStateLoaderFactory(@NonNull WeekStateCalculatorFactory weekStateCalculatorFactory) {
		this.weekStateCalculatorFactory = weekStateCalculatorFactory;
	}

	public @NonNull WeekStateLoader create(@NonNull Week week,
			@NonNull Consumer<WeekState> onLoadedCallback) {
		WeekStateCalculator weekStateCalculator = weekStateCalculatorFactory.createForWeek(week);
		return new WeekStateLoader(weekStateCalculator, onLoadedCallback);
	}

}
