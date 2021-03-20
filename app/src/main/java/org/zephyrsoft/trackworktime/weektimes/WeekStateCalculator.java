package org.zephyrsoft.trackworktime.weektimes;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.pmw.tinylog.Logger;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.IsoFields;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekState;
import org.zephyrsoft.trackworktime.model.WeekState.DayRowState;
import org.zephyrsoft.trackworktime.model.WeekState.SummaryRowState;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.timer.TimeCalculator;
import org.zephyrsoft.trackworktime.timer.TimeCalculatorV2;
import org.zephyrsoft.trackworktime.timer.TimeCalculatorV2.DayInfo;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WeekStateCalculator {

	private final DateTimeFormatter SHORT_DATE;

	private final Context context;
	private final DAO dao;
	private final TimerManager timerManager;
	private final TimeCalculator timeCalculator;
	private final Week week;
	
	private final boolean handleFlexiTime;

	public WeekStateCalculator(Context context, DAO dao, TimerManager timerManager,
		   TimeCalculator timeCalculator, SharedPreferences preferences, Week week) {
		this.context = context;
		this.dao = dao;
		this.timerManager = timerManager;
		this.timeCalculator = timeCalculator;
		this.week = week;
		
		this.handleFlexiTime = preferences.getBoolean(Key.ENABLE_FLEXI_TIME.getName(), false);

		SHORT_DATE = DateTimeFormatter.ofPattern(getString(R.string.shortDate), Locale.getDefault());
	}

	public @NonNull WeekState calculateWeekState() {
		WeekState weekState = new WeekState();
		loadWeek(weekState);
		return weekState;
	}

	private void loadWeek(WeekState weekState) {
		long startTime = System.nanoTime();
		
		LocalDate startDate = week.getStart();

		try {
			TimeCalculatorV2 timeCalc = new TimeCalculatorV2(dao, timerManager, startDate, handleFlexiTime);
			timeCalc.setStartSums(timerManager.getTimesAt(startDate));
		
			weekState.topLeftCorner = "W " + startDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

			for (DayOfWeek day : DayOfWeek.values()) {
				setRowValues(timeCalc.getNextDayInfo(), weekState.getRowForDay(day));
			}
		
			setSummaryRow(weekState.totals, timeCalc);
			
		} catch (Exception e) {
			Logger.debug(e, "could not calculate week");
		}

		// measure elapsed time
		long durationInMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
		Logger.debug("Calculated week in {} ms", durationInMillis);
	}

	private void setRowValues(DayInfo dayInfo, DayRowState weekRowState) {
		weekRowState.highlighted = dayInfo.isToday();
		weekRowState.label = dayInfo.getDate().format(SHORT_DATE);
		
		weekRowState.in = formatTime(dayInfo.getTimeIn());

		if (isCurrentMinute(dayInfo.getTimeOut()) && timerManager.isTracking()) {
			weekRowState.out = getString(R.string.now);
		} else {
			weekRowState.out = formatTime(dayInfo.getTimeOut());
		}
		
		if (handleFlexiTime) {
			weekRowState.labelHighlighted = !dayInfo.isWorkDay();
			weekRowState.workedHighlighted = (dayInfo.getType() == DayInfo.TYPE_SPECIAL_GRANT);
		}

		boolean isTodayOrEarlier = dayInfo.getDate().atStartOfDay().isBefore(LocalDateTime.now());

		if (isTodayOrEarlier && dayInfo.isWorkDay()) {
			weekRowState.worked = formatSum(dayInfo.getTimeWorked(), null);
		} else {
			weekRowState.worked = formatSum(dayInfo.getTimeWorked(), "");
		}


		boolean showFlexiTime = dayInfo.getTimeFlexi() != null;
		boolean freeWithoutEvents = !dayInfo.isWorkDay() && !dayInfo.containsEvents();

		if (!showFlexiTime || freeWithoutEvents || !handleFlexiTime) {
			weekRowState.flexi = "";
		} else if (isTodayOrEarlier && dayInfo.isWorkDay()) {
			weekRowState.flexi = formatSum(dayInfo.getTimeFlexi(), null);
		} else if (dayInfo.containsEvents()) {
			weekRowState.flexi = formatSum(dayInfo.getTimeFlexi(), "");
		} else {
			weekRowState.flexi = "";
		}
	}

	private void setSummaryRow(SummaryRowState summaryRow, TimeCalculatorV2 timeCalc) {
		summaryRow.label = getString(R.string.total);
		summaryRow.worked = TimerManager.formatTime(timeCalc.getTimeWorked());

		if (timeCalc.withFlexiTime()) {
			summaryRow.flexi = TimerManager.formatTime(timeCalc.getBalance());
		} else {
			summaryRow.flexi = "";
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
		return time == null ? "" : DateTimeUtil.dateTimeToHourMinuteString(time);
	}

	private String formatSum(Long sum, String valueForZero) {
		if (sum != null && sum == 0 && valueForZero != null) {
			return valueForZero;
		}
		return sum == null ? "" : TimerManager.formatTime(sum);
	}

	private String getString(@StringRes int id) {
		return context.getString(id);
	}

}
