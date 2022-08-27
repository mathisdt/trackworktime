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

import android.app.Activity;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.timer.TimerManager;

public class WeekStateCalculatorFactory {

	private final @NonNull Activity activity;
	private final @NonNull DAO dao;
	private final @NonNull TimerManager timerManager;
	private final @NonNull SharedPreferences preferences;

	public WeekStateCalculatorFactory(@NonNull Activity activity, @NonNull DAO dao,
									  @NonNull TimerManager timerManager, @NonNull SharedPreferences preferences) {
		this.activity = activity;
		this.dao = dao;
		this.timerManager = timerManager;
		this.preferences = preferences;
	}

	public @NonNull WeekStateCalculator createForWeek(@NonNull Week week) {
		return new WeekStateCalculator(activity, dao, timerManager, preferences, week);
	}

}
