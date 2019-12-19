package org.zephyrsoft.trackworktime.weektimes;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.DayLine;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekDayEnum;
import org.zephyrsoft.trackworktime.model.WeekPlaceholder;
import org.zephyrsoft.trackworktime.model.WeekRowState;
import org.zephyrsoft.trackworktime.model.WeekState;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.timer.TimeCalculator;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.util.List;

import hirondelle.date4j.DateTime;

public class WeekStateCalculator {

	private final Context context;
	private final DAO dao;
	private final TimerManager timerManager;
	private final SharedPreferences preferences;
	private final TimeCalculator timeCalculator;
	private final Week week;
	private final DateTime monday, tuesday, wednesday, thursday, friday, saturday, sunday;

	public WeekStateCalculator(@NonNull Context context, @NonNull DAO dao,
			@NonNull TimerManager timerManager, @NonNull TimeCalculator timeCalculator,
			@NonNull SharedPreferences preferences, @NonNull Week week) {
		this.context = context;
		this.dao = dao;
		this.timerManager = timerManager;
		this.timeCalculator = timeCalculator;
		this.preferences = preferences;
		this.week = week;

		monday = DateTimeUtil.stringToDateTime(week.getStart());
		tuesday = monday.plusDays(1);
		wednesday = tuesday.plusDays(1);
		thursday = wednesday.plusDays(1);
		friday = thursday.plusDays(1);
		saturday = friday.plusDays(1);
		sunday = saturday.plusDays(1);
	}

	public @NonNull WeekState calculateWeekState() {
		WeekState weekState = new WeekState();
		loadWeek(weekState);
		return weekState;
	}

	private void loadWeek(WeekState weekState) {
		setDates(weekState);
		setDays(weekState.header);
		setRowHighlighting(weekState);
		setTimes(weekState);
	}

	private void setDates(@NonNull WeekState weekState) {
		int weekIndex = thursday.getWeekIndex(DateTimeUtil.getBeginOfFirstWeekFor(thursday.getYear()));
		weekState.header.setDate("W " + weekIndex);

		weekState.monday.setDate(getString(R.string.monday) + getString(R.string.onespace)
				+ monday.format(getString(R.string.shortDateFormat)));

		weekState.tuesday.setDate(getString(R.string.tuesday) + getString(R.string.onespace)
				+ tuesday.format(getString(R.string.shortDateFormat)));

		weekState.wednesday.setDate(getString(R.string.wednesday) + getString(R.string.onespace)
				+ wednesday.format(getString(R.string.shortDateFormat)));

		weekState.thursday.setDate(getString(R.string.thursday) + getString(R.string.onespace)
				+ thursday.format(getString(R.string.shortDateFormat)));

		weekState.friday.setDate(getString(R.string.friday) + getString(R.string.onespace)
				+ friday.format(getString(R.string.shortDateFormat)));

		weekState.saturday.setDate(getString(R.string.saturday) + getString(R.string.onespace)
				+ saturday.format(getString(R.string.shortDateFormat)));

		weekState.sunday.setDate(getString(R.string.sunday) + getString(R.string.onespace)
				+ sunday.format(getString(R.string.shortDateFormat)));
	}

	private void setDays(@NonNull WeekRowState weekRowHeaderState) {
		weekRowHeaderState.setIn(getString(R.string.in));
		weekRowHeaderState.setOut(getString(R.string.out));
		weekRowHeaderState.setWorked(getString(R.string.worked));
		weekRowHeaderState.setFlexi(getString(R.string.flexi));
	}

	private void setRowHighlighting(@NonNull WeekState weekState) {
		DateTime today = DateTimeUtil.getCurrentDateTime();
		weekState.monday.setHiglighted(today.isSameDayAs(monday));
		weekState.tuesday.setHiglighted(today.isSameDayAs(tuesday));
		weekState.wednesday.setHiglighted(today.isSameDayAs(wednesday));
		weekState.thursday.setHiglighted(today.isSameDayAs(thursday));
		weekState.friday.setHiglighted(today.isSameDayAs(friday));
		weekState.saturday.setHiglighted(today.isSameDayAs(saturday));
		weekState.sunday.setHiglighted(today.isSameDayAs(sunday));
	}

	private void setTimes(@NonNull WeekState weekState) {
		TimeSum flexiBalance = null;
		boolean hasRealData = !(week instanceof WeekPlaceholder);
		if (hasRealData && preferences.getBoolean(Key.ENABLE_FLEXI_TIME.getName(), false)) {
			flexiBalance = timerManager.getFlexiBalanceAtWeekStart(
					DateTimeUtil.stringToDateTime(week.getStart()));
		}
		boolean earlierEventsExist = (dao.getLastEventBefore(monday.getStartOfDay()) != null);
		boolean showFlexiTimes = hasRealData || earlierEventsExist;

		List<Event> events = fetchEventsForDay(monday);
		flexiBalance = setTimesForSingleDay(monday, events, flexiBalance, weekState.monday,
				showFlexiTimes);

		events = fetchEventsForDay(tuesday);
		flexiBalance = setTimesForSingleDay(tuesday, events, flexiBalance, weekState.tuesday,
				showFlexiTimes);

		events = fetchEventsForDay(wednesday);
		flexiBalance = setTimesForSingleDay(wednesday, events, flexiBalance, weekState.wednesday,
				showFlexiTimes);

		events = fetchEventsForDay(thursday);
		flexiBalance = setTimesForSingleDay(thursday, events, flexiBalance, weekState.thursday,
				showFlexiTimes);

		events = fetchEventsForDay(friday);
		flexiBalance = setTimesForSingleDay(friday, events, flexiBalance, weekState.friday,
				showFlexiTimes);

		events = fetchEventsForDay(saturday);
		flexiBalance = setTimesForSingleDay(saturday, events, flexiBalance, weekState.saturday,
				showFlexiTimes);

		events = fetchEventsForDay(sunday);
		flexiBalance = setTimesForSingleDay(sunday, events, flexiBalance, weekState.sunday,
				showFlexiTimes);

		DateTime weekStart = DateTimeUtil.getWeekStart(DateTimeUtil.stringToDateTime(week.getStart()));
		TimeSum amountWorked = timerManager.calculateTimeSum(weekStart, PeriodEnum.WEEK);
		boolean showFlexi = showFlexiTimes && DateTimeUtil.isInPast(monday.getStartOfDay());
		setSummaryLine(weekState.totals, amountWorked, flexiBalance, showFlexi);
	}

	private List<Event> fetchEventsForDay(DateTime day) {
		Logger.debug("fetchEventsForDay: {}", DateTimeUtil.dateTimeToDateString(day));
		List<Event> ret = dao.getEventsOnDay(day);
		DateTime now = DateTimeUtil.getCurrentDateTime();
		Event lastEventBeforeNow = dao.getLastEventBefore(now);
		if (day.isSameDayAs(now) && TimerManager.isClockInEvent(lastEventBeforeNow)) {
			// currently clocked in: add clock-out event "NOW"
			ret.add(timerManager.createClockOutNowEvent());
		}
		return ret;
	}

	private TimeSum setTimesForSingleDay(DateTime day, List<Event> events, TimeSum flexiBalanceAtDayStart,
			WeekRowState weekRowState, boolean showFlexiTimes) {

		DayLine dayLine = timeCalculator.calulateOneDay(day, events);

		WeekDayEnum weekDay = WeekDayEnum.getByValue(day.getWeekDay());
		boolean isWorkDay = timerManager.isWorkDay(weekDay);
		boolean isTodayOrEarlier = DateTimeUtil.isInPast(day.getStartOfDay());
		boolean containsEventsForDay = containsEventsForDay(events, day);
		boolean weekEndWithoutEvents = !isWorkDay && !containsEventsForDay;
		// correct result by previous flexi time sum
		dayLine.getTimeFlexi().addOrSubstract(flexiBalanceAtDayStart);

		weekRowState.setIn(formatTime(dayLine.getTimeIn()));

		final String out;
		if (isCurrentMinute(dayLine.getTimeOut()) && timerManager.isTracking()) {
			out = "NOW";
		} else {
			out = formatTime(dayLine.getTimeOut());
		}
		weekRowState.setOut(out);

		final String worked;
		if (weekEndWithoutEvents) {
			worked = "";
		} else if (isWorkDay && isTodayOrEarlier) {
			worked = formatSum(dayLine.getTimeWorked(), null);
		} else {
			worked = formatSum(dayLine.getTimeWorked(), "");
		}
		weekRowState.setWorked(worked);

		final String flexi;
		if (!showFlexiTimes || weekEndWithoutEvents || !preferences.getBoolean(Key.ENABLE_FLEXI_TIME.getName(),
				false)) {
			flexi = "";
		} else if (isWorkDay && isTodayOrEarlier) {
			flexi = formatSum(dayLine.getTimeFlexi(), null);
		} else if (containsEventsForDay) {
			flexi = formatSum(dayLine.getTimeFlexi(), "");
		} else {
			flexi = "";
		}
		weekRowState.setFlexi(flexi);

		return dayLine.getTimeFlexi();
	}

	private void setSummaryLine(WeekRowState weekRowState, TimeSum amountWorked, TimeSum flexiBalance,
			boolean showFlexiTimes) {
		weekRowState.setDate(getString(R.string.total));

		weekRowState.setWorked(amountWorked.toString());

		boolean showFlexi = flexiBalance != null && showFlexiTimes;
		weekRowState.setFlexi(showFlexi ? flexiBalance.toString() : "");
	}

	private boolean containsEventsForDay(List<Event> events, DateTime day) {
		for (Event event : events) {
			if (DateTimeUtil.stringToDateTime(event.getTime()).isSameDayAs(day)) {
				return true;
			}
		}
		return false;
	}

	private boolean isCurrentMinute(DateTime dateTime) {
		if (dateTime == null) {
			return false;
		}
		DateTime now = DateTimeUtil.getCurrentDateTime();
		return now.getYear().equals(dateTime.getYear())
				&& now.getMonth().equals(dateTime.getMonth())
				&& now.getDay().equals(dateTime.getDay())
				&& now.getHour().equals(dateTime.getHour())
				&& now.getMinute().equals(dateTime.getMinute());
	}

	private String formatTime(DateTime time) {
		return time == null ? "" : DateTimeUtil.dateTimeToHourMinuteString(time);
	}

	private String formatSum(TimeSum sum, String valueForZero) {
		if (sum != null && sum.getAsMinutes() == 0 && valueForZero != null) {
			return valueForZero;
		}
		return sum == null ? "" : sum.toString();
	}

	private String getString(@StringRes int id) {
		return context.getString(id);
	}

}
