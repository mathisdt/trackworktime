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

import org.pmw.tinylog.Logger;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.Locale;
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
		loadWeek(weekState, preferences.getBoolean(Key.DECIMAL_TIME_SUMS.getName(), false));
		return weekState;
	}

	private void loadWeek(WeekState weekState, boolean decimalAmounts) {
		long startTime = System.nanoTime();
		
		LocalDate startDate = week.getStart();

		try {
			TimeCalculatorV2 timeCalc = new TimeCalculatorV2(dao, timerManager, startDate, handleFlexiTime);
			timeCalc.setStartSums(timerManager.getTimesAt(startDate));
		
			weekState.topLeftCorner = context.getString(R.string.weekNumber, startDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));

			DateTimeUtil.LocalizedDayAndShortDateFormatter formatter = new DateTimeUtil.LocalizedDayAndShortDateFormatter(context);
			for (DayOfWeek day : DayOfWeek.values()) {
				setRowValues(timeCalc.getNextDayInfo(), weekState.getRowForDay(day), formatter, decimalAmounts);
			}
		
			setSummaryRow(weekState.totals, timeCalc, decimalAmounts);
			
		} catch (Exception e) {
			Logger.debug(e, "could not calculate week");
		}

		// measure elapsed time
		long durationInMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
		Logger.debug("Calculated week in {} ms", durationInMillis);
	}

	private void setRowValues(DayInfo dayInfo, DayRowState dayRowState, DateTimeUtil.LocalizedDayAndShortDateFormatter formatter, boolean decimalAmounts) {
		dayRowState.highlighted = dayInfo.isToday();
		dayRowState.label = formatter.format(dayInfo.getDate());
		
		dayRowState.in = formatTime(dayInfo.getTimeIn(), formatter.getLocale());

		if (isCurrentMinute(dayInfo.getTimeOut()) && timerManager.isTracking()) {
			dayRowState.out = getString(R.string.now);
		} else {
			dayRowState.out = formatTime(dayInfo.getTimeOut(), formatter.getLocale());
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
			if (decimalAmounts) {
				dayRowState.workedDecimal = formatDecimal(dayInfo.getTimeWorked(), null);
			}
		} else {
			dayRowState.worked = formatSum(dayInfo.getTimeWorked(), "");
			if (decimalAmounts) {
				dayRowState.workedDecimal = formatDecimal(dayInfo.getTimeWorked(), "");
			}
		}


		boolean showFlexiTime = dayInfo.getTimeFlexi() != null;
		boolean freeWithoutEvents = !dayInfo.isWorkDay() && !dayInfo.containsEvents();

		if (!showFlexiTime || freeWithoutEvents || !handleFlexiTime) {
			dayRowState.flexi = "";
			if (decimalAmounts) {
				dayRowState.flexiDecimal = "";
			}
		} else if (isTodayOrEarlier && dayInfo.isWorkDay()) {
			dayRowState.flexi = formatSum(dayInfo.getTimeFlexi(), null);
			if (decimalAmounts) {
				dayRowState.flexiDecimal = formatDecimal(dayInfo.getTimeFlexi(), null);
			}
		} else if (dayInfo.containsEvents()) {
			dayRowState.flexi = formatSum(dayInfo.getTimeFlexi(), "");
			if (decimalAmounts) {
				dayRowState.flexiDecimal = formatDecimal(dayInfo.getTimeFlexi(), "");
			}
		} else {
			dayRowState.flexi = "";
			if (decimalAmounts) {
				dayRowState.flexiDecimal = "";
			}
		}
	}

	private void setSummaryRow(SummaryRowState summaryRow, TimeCalculatorV2 timeCalc, boolean decimalAmounts) {
		summaryRow.label = getString(R.string.total);
		summaryRow.worked = TimerManager.formatTime(timeCalc.getTimeWorked());
		if (decimalAmounts) {
			summaryRow.workedDecimal = TimerManager.formatDecimal(timeCalc.getTimeWorked());
		}

		if (timeCalc.withFlexiTime()) {
			summaryRow.flexi = TimerManager.formatTime(timeCalc.getBalance());
			if (decimalAmounts) {
				summaryRow.flexiDecimal = TimerManager.formatDecimal(timeCalc.getBalance());
			}
		} else {
			summaryRow.flexi = "";
			if (decimalAmounts) {
				summaryRow.flexiDecimal = "";
			}
		}
	}

	private boolean isCurrentMinute(LocalDateTime dateTime) {
		if (dateTime == null) {
			return false;
		}

		LocalDateTime now = LocalDateTime.now();
		return dateTime.truncatedTo(ChronoUnit.MINUTES).isEqual(now.truncatedTo(ChronoUnit.MINUTES));
	}

	private String formatTime(LocalDateTime time, Locale locale) {
		return time == null ? "" : DateTimeUtil.formatLocalizedTime(time, locale);
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
