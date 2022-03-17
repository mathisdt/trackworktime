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

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.IsoFields;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekState;
import org.zephyrsoft.trackworktime.model.WeekState.DayRowState;
import org.zephyrsoft.trackworktime.model.WeekState.SummaryRowState;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.timer.TimeCalculatorV2;
import org.zephyrsoft.trackworktime.timer.TimeCalculatorV2.DayInfo;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.util.concurrent.TimeUnit;

public class WeekStateCalculator {

	private final Context context;
	private final DAO dao;
	private final TimerManager timerManager;
	private final SharedPreferences preferences;
	private final Week week;
	
	private final boolean handleFlexiTime;

	public WeekStateCalculator(Context context, DAO dao, TimerManager timerManager,
		   SharedPreferences preferences, Week week) {
		this.context = context;
		this.dao = dao;
		this.timerManager = timerManager;
		this.preferences = preferences;
		this.week = week;
		
		this.handleFlexiTime = preferences.getBoolean(Key.ENABLE_FLEXI_TIME.getName(), false);
	}

	public @NonNull WeekState calculateWeekState() {
		WeekState weekState = new WeekState();
		boolean decimalTimeSums = preferences.getBoolean(Key.DECIMAL_TIME_SUMS.getName(), false);
		loadWeek(weekState, decimalTimeSums);
		return weekState;
	}

	private void loadWeek(WeekState weekState, boolean decimalTimeSums) {
		long startTime = System.nanoTime();
		
		LocalDate startDate = week.getStart();

		try {
			TimeCalculatorV2 timeCalc = new TimeCalculatorV2(dao, timerManager, startDate, handleFlexiTime);
			timeCalc.setStartSums(timerManager.getTimesAt(startDate));
		
			weekState.topLeftCorner = "W " + startDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

			for (DayOfWeek day : DayOfWeek.values()) {
				setRowValues(timeCalc.getNextDayInfo(), weekState.getRowForDay(day), decimalTimeSums);
			}
		
			setSummaryRow(weekState.totals, timeCalc, decimalTimeSums);
			
		} catch (Exception e) {
			Logger.debug(e, "could not calculate week");
		}

		// measure elapsed time
		long durationInMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
		Logger.debug("Calculated week in {} ms", durationInMillis);
	}

	private void setRowValues(DayInfo dayInfo, DayRowState dayRowState, boolean decimalTimeSums) {
		dayRowState.highlighted = dayInfo.isToday();
		dayRowState.label = DateTimeUtil.formatLocalizedDayAndShortDate(dayInfo.getDate());
		
		dayRowState.in = formatTime(dayInfo.getTimeIn());

		if (isCurrentMinute(dayInfo.getTimeOut()) && timerManager.isTracking()) {
			dayRowState.out = getString(R.string.now);
		} else {
			dayRowState.out = formatTime(dayInfo.getTimeOut());
		}
		
		if (handleFlexiTime) {
			switch (dayInfo.getType()) {
				case DayInfo.TYPE_REGULAR_FREE:
					dayRowState.labelHighlighted = WeekState.HighlightType.REGULAR_FREE;
					break;
				case DayInfo.TYPE_FREE:
					dayRowState.labelHighlighted = WeekState.HighlightType.FREE;
					break;
				case DayInfo.TYPE_REGULAR_WORK:
					// no highlighting
					break;
				case DayInfo.TYPE_SPECIAL_GRANT:
					dayRowState.labelHighlighted = WeekState.HighlightType.CHANGED_TARGET_TIME;
					break;
				default:
					throw new IllegalStateException("unknown DayInfo type " + dayInfo.getType());
			}
		}

		boolean isTodayOrEarlier = dayInfo.getDate().atStartOfDay().isBefore(LocalDateTime.now());

		if (isTodayOrEarlier && dayInfo.isWorkDay()) {
			dayRowState.worked = formatSum(dayInfo.getTimeWorked(), null);
			String workedDecimal = formatDecimal(dayInfo.getTimeWorked(), null);
			dayRowState.workedDecimal = StringUtils.isNotBlank(workedDecimal)
				? "(" + workedDecimal + ")"
				: null;
		} else {
			dayRowState.worked = formatSum(dayInfo.getTimeWorked(), "");
			String workedDecimal = formatDecimal(dayInfo.getTimeWorked(), "");
			dayRowState.workedDecimal = StringUtils.isNotBlank(workedDecimal)
				? "(" + workedDecimal + ")"
				: "";
		}


		boolean showFlexiTime = dayInfo.getTimeFlexi() != null;
		boolean freeWithoutEvents = !dayInfo.isWorkDay() && !dayInfo.containsEvents();

		if (!showFlexiTime || freeWithoutEvents || !handleFlexiTime) {
			dayRowState.flexi = "";
			dayRowState.flexiDecimal = "";
		} else if (isTodayOrEarlier && dayInfo.isWorkDay()) {
			dayRowState.flexi = formatSum(dayInfo.getTimeFlexi(), null);
			dayRowState.flexiDecimal = "(" + formatDecimal(dayInfo.getTimeFlexi(), null) + ")";
		} else if (dayInfo.containsEvents()) {
			dayRowState.flexi = formatSum(dayInfo.getTimeFlexi(), "");
			dayRowState.flexiDecimal = "(" + formatDecimal(dayInfo.getTimeFlexi(), "") + ")";
		} else {
			dayRowState.flexi = "";
			dayRowState.flexiDecimal = "";
		}
	}

	private void setSummaryRow(SummaryRowState summaryRow, TimeCalculatorV2 timeCalc, boolean decimalTimeSums) {
		summaryRow.label = getString(R.string.total);
		summaryRow.worked = TimerManager.formatTime(timeCalc.getTimeWorked());
		summaryRow.workedDecimal = "(" + TimerManager.formatDecimal(timeCalc.getTimeWorked()) + ")";

		if (timeCalc.withFlexiTime()) {
			summaryRow.flexi = TimerManager.formatTime(timeCalc.getBalance());
			summaryRow.flexiDecimal = "(" + TimerManager.formatDecimal(timeCalc.getBalance()) + ")";
		} else {
			summaryRow.flexi = "";
			summaryRow.flexiDecimal = "";
		}
	}

	private boolean isCurrentMinute(LocalDateTime dateTime) {
		if (dateTime == null) {
			return false;
		}

		LocalDateTime now = LocalDateTime.now();
		return dateTime.truncatedTo(ChronoUnit.MINUTES).isEqual(now.truncatedTo(ChronoUnit.MINUTES));
	}

	private String formatTime(LocalDateTime time) {
		return time == null ? "" : DateTimeUtil.formatLocalizedTime(time);
	}

	private String formatSum(Long sum, String valueForZero) {
		if (sum != null && sum == 0 && valueForZero != null) {
			return valueForZero;
		}
		return sum == null ? "" : TimerManager.formatTime(sum);
	}

	private String formatDecimal(Long sum, String valueForZero) {
		if (sum != null && sum == 0 && valueForZero != null) {
			return valueForZero;
		}
		return sum == null ? "" : TimerManager.formatDecimal(sum);
	}

	private String getString(@StringRes int id) {
		return context.getString(id);
	}

}
