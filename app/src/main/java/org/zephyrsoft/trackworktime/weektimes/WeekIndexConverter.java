package org.zephyrsoft.trackworktime.weektimes;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekPlaceholder;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.util.TimeZone;

import hirondelle.date4j.DateTime;

/**
 * Converts week-index to {@link Week} and vice versa, where week-index is number of weeks after epoch,
 * starting with 0.
 *
 * E.g. index of 0 means 1st week after epoch.
 */
public class WeekIndexConverter {

	private final DAO dao;
	private final DateTime epochDate;

	public WeekIndexConverter(@NonNull DAO dao, @NonNull TimeZone timeZone) {
		this.dao = dao;
		epochDate = DateTime.forInstant(0, timeZone);
		verifyValues();
	}

	private void verifyValues() {
		if(dao == null || epochDate == null) {
			throw new IllegalStateException("Invalid class state, dao " + dao + ", epochDate "
					+ epochDate);
		}
	}

	public @NonNull Week getWeekForIndex(@IntRange(from=0) int weekIndex) {
		checkWeekIndex(weekIndex);
		DateTime weekOnIndex = getDateForIndex(weekIndex);
		return getWeekForDate(weekOnIndex);
	}

	private void checkWeekIndex(int weekIndex) {
		if(weekIndex < 0) {
			throw new IllegalArgumentException("Week index should be positive");
		}
	}

	private DateTime getDateForIndex(int weekIndex) {
		return DateTimeUtil.plusWeeks(epochDate, weekIndex);
	}

	private Week getWeekForDate(DateTime dateTime) {
		DateTime weekStart = DateTimeUtil.getWeekStart(dateTime);
		Week week = dao.getWeek(DateTimeUtil.dateTimeToString(weekStart));
		if (week == null) {
			week = new WeekPlaceholder(DateTimeUtil.dateTimeToString(weekStart));
		}
		return week;
	}

	public @IntRange(from=0) int getIndexForWeek(@NonNull Week week) {
		checkWeek(week);
		DateTime weekDate = getDateForWeek(week);
		return weekDate.getWeekIndex(epochDate);
	}

	private void checkWeek(Week week) {
		if(week == null || week.getStart() == null || week.getStart().isEmpty()) {
			throw new IllegalArgumentException("Invalid Week " + week);
		}
	}

	private DateTime getDateForWeek(Week week) {
		String start = week.getStart();
		return DateTimeUtil.stringToDateTime(start);
	}

}
