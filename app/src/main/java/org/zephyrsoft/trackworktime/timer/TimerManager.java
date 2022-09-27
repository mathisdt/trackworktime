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
package org.zephyrsoft.trackworktime.timer;

import static org.zephyrsoft.trackworktime.util.DateTimeUtil.truncateToMinute;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.location.TrackingMethod;
import org.zephyrsoft.trackworktime.model.CalcCacheEntry;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.FlexiReset;
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TimeInfo;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.util.BroadcastUtil;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.Updatable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Manages the time tracking.
 */
public class TimerManager {

	private final DAO dao;
	private final SharedPreferences preferences;
	private final Context context;
	private final List<Updatable> listeners = new ArrayList<>();

	/**
	 * Constructor
	 */
	public TimerManager(DAO dao, SharedPreferences preferences, Context context) {
		this.dao = dao;
		this.preferences = preferences;
		this.context = context;
	}

	public void addListener(Updatable listener) {
		listeners.add(listener);
	}

	public void removeListener(Updatable listener) {
		listeners.remove(listener);
	}

	public void notifyListeners() {
		Logger.debug("notifying {} listeners", listeners.size());
		for (Updatable listener : listeners) {
			try {
				if (listener != null) {
					listener.update();
				}
			} catch (Exception e) {
				Logger.debug(e, "error while notifying listener");
			}
		}
	}

	public ZoneId getHomeTimeZone() {
		String homeTimeZone = preferences.getString(Key.HOME_TIME_ZONE.getName(),  null);

		if (homeTimeZone == null) {
			// TODO start activity to select time zone?
			return ZoneId.systemDefault();
		} else {
			return ZoneId.of(homeTimeZone);
		}
	}

	public ZoneOffset getHomeTimeZoneOffset(LocalDateTime localDateTime) {
		return getHomeTimeZone().getRules().getOffset(localDateTime);
	}

	public FlexiReset getFlexiReset() {
		return FlexiReset.loadFromPreferences(preferences);
	}

	public boolean insertDefaultWorkTimes(LocalDate from, LocalDate to, Integer taskId, String text) {
		try {
			LocalDate running = from;

			while (!running.isAfter(to)) {
				// clock-in at start of day (00:00) in home time zone
				// TODO current time zone?

				DayOfWeek weekDay = running.getDayOfWeek();
				int workDuration = getNormalWorkDurationFor(weekDay);
				if (workDuration > 0) {
					OffsetDateTime clockInTime = running.atStartOfDay(getHomeTimeZone()).toOffsetDateTime();
					createEvent(clockInTime, taskId, TypeEnum.CLOCK_IN, text);

					OffsetDateTime clockOutTime = clockInTime.plusMinutes(workDuration);
					if (isAutoPauseApplicable(clockOutTime)) {
						long pauseDuration = getAutoPauseDuration();
						clockOutTime = clockOutTime.plusMinutes(pauseDuration);
					}
					createEvent(clockOutTime, null, TypeEnum.CLOCK_OUT, null);
				}

				running = running.plusDays(1);
			}
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	/**
	 * Checks the current state.
	 *
	 * @return {@code true} if currently clocked in, {@code false} otherwise
	 */
	public boolean isTracking() {
		Event latestEvent = dao.getLastEventUpTo(OffsetDateTime.now());
		return latestEvent != null && latestEvent.getType().equals(TypeEnum.CLOCK_IN.getValue());
	}

	/**
	 * @return the timestamp - or {@code null} if currently not clocked in
	 */
	public OffsetDateTime getLastClockIn() {
		Event latestEvent = dao.getLastEventUpTo(OffsetDateTime.now());
		return latestEvent != null && latestEvent.getType().equals(TypeEnum.CLOCK_IN.getValue())
			? truncateToMinute(latestEvent.getDateTime())
			: null;
	}

	/**
	 * Returns {@code true} if the options are set in a way that an event is in the defined time before/after an
	 * existing event (not counting CLOCK_OUT_NOW).
	 */
	public boolean isInIgnorePeriodForLocationBasedTracking() {
		OffsetDateTime now = OffsetDateTime.now();

		// get first event AFTER now, subtract the minutes to ignore before events and check if the result is BEFORE now
		Event firstAfterNow = dao.getFirstEventAfter(now);
		String ignoreBeforeString = preferences.getString(Key.LOCATION_BASED_TRACKING_IGNORE_BEFORE_EVENTS.getName(),
			"0");
		int ignoreBefore = 0;
		try {
			ignoreBefore = Integer.parseInt(ignoreBeforeString);
		} catch (NumberFormatException nfe) {
			Logger.warn("illegal value - ignore before events: {}", ignoreBeforeString);
		}
		if (firstAfterNow != null) {
			OffsetDateTime firstAfterNowTime = firstAfterNow.getDateTime();

			if (firstAfterNowTime.minusMinutes(ignoreBefore).isBefore(now)) {
				return true;
			}
		}
		// get the last event BEFORE now, add the minutes to ignore after events and check if the result is AFTER now
		Event lastBeforeNow = dao.getLastEventBefore(OffsetDateTime.now());
		String ignoreAfterString = preferences
			.getString(Key.LOCATION_BASED_TRACKING_IGNORE_AFTER_EVENTS.getName(), "0");
		int ignoreAfter = 0;
		try {
			ignoreAfter = Integer.parseInt(ignoreAfterString);
		} catch (NumberFormatException nfe) {
			Logger.warn("illegal value - ignore after events: {}", ignoreAfterString);
		}
		if (lastBeforeNow != null) {
			OffsetDateTime lastBeforeNowTime = lastBeforeNow.getDateTime();

			return lastBeforeNowTime.plusMinutes(ignoreAfter).isAfter(now);
		}

		return false;
	}

	/**
	 * Returns the currently active task or {@code null} if tracking is disabled at the moment.
	 */
	public Task getCurrentTask() {
		Event latestEvent = dao.getLastEventBefore(OffsetDateTime.now());
		if (latestEvent != null && latestEvent.getType().equals(TypeEnum.CLOCK_IN.getValue())) {
			return dao.getTask(latestEvent.getTask());
		} else {
			return null;
		}
	}

	/**
	 * Returns the default task or {@code null} if no task is configured.
	 */
	public Task getDefaultTask() {
		return dao.getDefaultTask();
	}

	/**
	 * Either starts tracking (from non-tracked time) or changes the task and/or text (from already tracked time).
	 * If the task is {@code null}, the default task is taken. If there is no default task, no task will be taken.
	 *
	 * @param minutesToPredate
	 *            how many minutes in the future should the event be
	 * @param selectedTask
	 *            the task for which the time shall be tracked
	 * @param text
	 *            free text to describe in detail what was done
	 */
	public void startTracking(int minutesToPredate, Task selectedTask, String text) {
		Task taskToLink = selectedTask;
		if (taskToLink == null) {
			taskToLink = dao.getDefaultTask();
		}
		createEvent(minutesToPredate, (taskToLink == null ? null : taskToLink.getId()), TypeEnum.CLOCK_IN, text);
		Basics.get(context).safeCheckExternalControls();
	}

	/**
	 * Stops tracking time.
	 *
	 * @param minutesToPredate
	 *            how many minutes in the future should the event be
	 */
	public void stopTracking(int minutesToPredate) {
		createEvent(minutesToPredate, null, TypeEnum.CLOCK_OUT, null);
		Basics.get(context).safeCheckExternalControls();
	}

	/**
	 * Is the event a clock-in event? {@code null} as argument will return {@code false}.
	 */
	public static boolean isClockInEvent(Event event) {
		return event != null && event.getType().equals(TypeEnum.CLOCK_IN.getValue());
	}

	/**
	 * Is the event a clock-out event? {@code null} as argument will return {@code false}.
	 */
	public static boolean isClockOutEvent(Event event) {
		return event != null
			&& (event.getType().equals(TypeEnum.CLOCK_OUT.getValue()) || event.getType().equals(
				TypeEnum.CLOCK_OUT_NOW.getValue()));
	}

	/**
	 * Calculate a time sum for a given period.
	 */
	public long calculateTimeSum(LocalDate date, PeriodEnum periodEnum) {
		// TODO restructure for clarity!
		// TODO improve performance
		if (periodEnum != PeriodEnum.ALL_TIME) {
			Logger.debug("calculating time sum for {} containing {}", periodEnum.name(), date);
		} else {
			Logger.debug("calculation time sum for all time");
		}

		TimeInfo infoStart;
		TimeInfo infoEnd;

		switch (periodEnum) {
			case DAY: {
				TimeCalculatorV2 timeCalc = new TimeCalculatorV2(dao, this, date, true);
				timeCalc.calculatePeriod(PeriodEnum.DAY, false);
				return timeCalc.getTimeWorked();
			}

			case WEEK: {
				TimeCalculatorV2 timeCalc = new TimeCalculatorV2(dao, this, date, true);
				timeCalc.calculatePeriod(PeriodEnum.WEEK, false);
				return timeCalc.getTimeWorked();
			}

			case MONTH:
				infoStart = getTimesAt(date.with(TemporalAdjusters.firstDayOfMonth()));

				LocalDate endDate = date.with(date.with(TemporalAdjusters.firstDayOfNextMonth()));
				if (endDate.isAfter(LocalDate.now())) {
					endDate = LocalDate.now().plusDays(1);
				}

				infoEnd = getTimesAt(endDate);
				break;
			case ALL_TIME:
				// FIXME better solution
				infoStart = getTimesAt(null);
				infoEnd = getTimesAt(LocalDate.now().plusDays(1));
				break;
			default:
				throw new IllegalArgumentException("unknown period type");
		}

		return (int) (infoEnd.getActual() - infoStart.getActual());
	}

	/**
	 * Get the remaining time for today (in minutes). Takes into account the target work time for
	 * the week and also if this is the last day in the working week.
	 */
	public Integer getMinutesRemaining() {
		boolean toZeroEveryDay = preferences.getBoolean(Key.FLEXI_TIME_TO_ZERO_ON_EVERY_DAY.getName(),
			false);
		OffsetDateTime dateTime = OffsetDateTime.now();
		DayOfWeek weekDay = dateTime.getDayOfWeek();
		if (isWorkDay(weekDay)) {
			int minutesRemaining = 0;
			Logger.debug("isAutoPauseEnabled={}", isAutoPauseEnabled());
			Logger.debug("isAutoPauseTheoreticallyApplicable={}", isAutoPauseTheoreticallyApplicable(dateTime));
			Logger.debug("isAutoPauseApplicable={}", isAutoPauseApplicable(dateTime));
			if (isAutoPauseEnabled() && isAutoPauseTheoreticallyApplicable(dateTime)
					&& !isAutoPauseApplicable(dateTime)) {
				// auto-pause is necessary, but was NOT already taken into account by calculatePeriod():
				Logger.debug("auto-pause is necessary, but was NOT already taken into account by calculateTimeSum()");
				minutesRemaining += getAutoPauseDuration();
			}

			if (!isFollowedByWorkDay(weekDay) || toZeroEveryDay) {
				// reach zero today
				TimeCalculatorV2 timeCalc = new TimeCalculatorV2(dao, this, dateTime.toLocalDate(), true);
				timeCalc.calculatePeriod(PeriodEnum.DAY, true);

				minutesRemaining += (int)-timeCalc.getBalance();
			} else {
				// not the last work day of the week, distribute remaining work time
				TimeCalculatorV2 timeCalc = new TimeCalculatorV2(dao, this, dateTime.toLocalDate(), true);
				timeCalc.calculatePeriod(PeriodEnum.WEEK, false);

				long remainingWeekPerDay = -timeCalc.getBalance() / (timeCalc.getFutureWorkDays() + 1);
				long remainingToday      = -timeCalc.getCurrentDayBalance() + minutesRemaining;

				minutesRemaining = (int)Math.min(remainingToday,remainingWeekPerDay);
			}

			Logger.debug("minutesRemaining={}", minutesRemaining);

			return minutesRemaining;
		} else {
			return null;
		}
	}

	/**
	 * Get the time info at the start of the specific day.
	 */
	public synchronized TimeInfo getTimesAt(LocalDate targetDate) {
		Logger.debug("Calculating times at {}", targetDate);

		TimeInfo ret = new TimeInfo();
		LocalDate startDate;
		CalcCacheEntry cache = null;

		Event event = dao.getFirstEvent();
		if (event == null) {
			return ret;
		}

		if (targetDate != null) {
			cache = dao.getCacheAt(targetDate);
		}

		if (cache == null) {
			Logger.debug("No cache for date {}", targetDate);

			// Start value assumed at start of week
			startDate =  DateTimeUtil.getWeekStart(event.getDateTime().toLocalDate());

			String startValueString = preferences.getString(Key.FLEXI_TIME_START_VALUE.getName(), "0:00");
			int startValue = parseHoursMinutesString(startValueString);

			ret.setActual(startValue);
		} else {
			startDate = cache.getDate();
			Logger.debug("Cache entry found for date {}: {}", startDate, cache.getWorked());

			ret.setActual(cache.getWorked());
			ret.setTarget(cache.getTarget());
		}

		if (targetDate == null || !startDate.isBefore(targetDate)) {
			return ret;
		}

		Logger.debug("Start sum: {}", formatTime(ret.getBalance()));

		long iter = ChronoUnit.DAYS.between(startDate, targetDate);
		Logger.debug("Date range to calculate: {} -> {}", startDate, targetDate);
		Logger.debug("Number of days to calculate: {}", iter);

		TimeCalculatorV2 timeCalc = new TimeCalculatorV2(dao, this, startDate,true);
		timeCalc.setStartSums(ret);

		// FIXME background task
		LocalDate currentDate = startDate;
		iter = 0;
		while (currentDate.isBefore(targetDate)) {
			timeCalc.calculateNextDay();

			Logger.debug("Sum at {}: {} = {} - {}", currentDate, timeCalc.getBalance(), timeCalc.getTotalTimeWorked(), timeCalc.getTotalTarget());


			currentDate = currentDate.plusDays(1);

			// save checkpoint in cache on Mondays and first day of month,
			// data from the **beginning** of the day (= 0:00)
			// currentDay is one ahead, so if it is today, we can already write the cache

			if (!currentDate.isAfter(LocalDate.now()) &&
					(currentDate.getDayOfWeek() == DayOfWeek.MONDAY || currentDate.getDayOfMonth() == 1)) {
				Logger.debug("Saving checkpoint for date: {}", currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

				CalcCacheEntry cacheEntry = new CalcCacheEntry(currentDate, timeCalc.getTotalTimeWorked(), timeCalc.getTotalTarget());
				Logger.debug("Data: {}", cacheEntry);

				dao.insertCache(cacheEntry);
			}

			iter++;
		}
		Logger.debug("Calculated {} days", iter);

		if (timeCalc.withFlexiTime()) {
			Logger.debug("Calculated flexi time:       {}", timeCalc.getBalance());
		}

		ret.setActual(timeCalc.getTotalTimeWorked());
		ret.setTarget(timeCalc.getTotalTarget());

		Logger.debug("DONE getTimesAt({}): actual={}, target={}",
				targetDate, ret.getActual(), ret.getTarget());
		Logger.debug("--");

		return ret;
	}

	/**
	 * Parse a value of hours and minutes (positive or negative).
	 */
	public static int parseHoursMinutesString(String hoursMinutes) {
		if (hoursMinutes != null) {
			String[] startValueArray = hoursMinutes.replaceAll("[- ]", "").split("[:.]");
			int hours = Integer.parseInt(startValueArray[0]);
			int minutes = startValueArray.length > 1 ? Integer.parseInt(startValueArray[1]) : 0;

			if (hoursMinutes.trim().startsWith("-")) {
				return -1 * (hours * 60 + minutes);
			} else {
				return hours * 60 + minutes;
			}
		}
		return 0;
	}

	/**
	 * Get the normal work time (in minutes) for a specific week day.
	 */
	public int getNormalWorkDurationFor(DayOfWeek weekDay) {
		if (isWorkDay(weekDay)) {
			String targetValueString = preferences.getString(Key.FLEXI_TIME_TARGET.getName(), "0:00");
			targetValueString = DateTimeUtil.refineHourMinute(targetValueString);
			int targetValue = parseHoursMinutesString(targetValueString);
			BigDecimal minutes = new BigDecimal(targetValue).divide(new BigDecimal(countWorkDays()),
				RoundingMode.HALF_UP);
			return minutes.intValue();
		} else {
			return 0;
		}
	}

	public int countWorkDays() {
		int ret = 0;
		for (DayOfWeek day : DayOfWeek.values()) {
			if (isWorkDay(day)) {
				ret++;
			}
		}
		return ret;
	}

	/**
	 * Is the next day after the given day marked as work day?
	 */
	private boolean isFollowedByWorkDay(DayOfWeek day) {
		DayOfWeek nextDay = day.plus(1);
		return isWorkDay(nextDay);
	}

	/**
	 * Is this a work day?
	 */
	public boolean isWorkDay(DayOfWeek weekDay) {
		Key key;
		switch (weekDay) {
			case MONDAY:
				key = Key.FLEXI_TIME_DAY_MONDAY;
				break;
			case TUESDAY:
				key = Key.FLEXI_TIME_DAY_TUESDAY;
				break;
			case WEDNESDAY:
				key = Key.FLEXI_TIME_DAY_WEDNESDAY;
				break;
			case THURSDAY:
				key = Key.FLEXI_TIME_DAY_THURSDAY;
				break;
			case FRIDAY:
				key = Key.FLEXI_TIME_DAY_FRIDAY;
				break;
			case SATURDAY:
				key = Key.FLEXI_TIME_DAY_SATURDAY;
				break;
			case SUNDAY:
				key = Key.FLEXI_TIME_DAY_SUNDAY;
				break;
			default:
				throw new IllegalArgumentException("unknown weekday");
		}
		return preferences.getBoolean(key.getName(), false);
	}

	/**
	 * Create a new event at current time + the given minute amount.
	 *
	 * @param minutesToPredate
	 *            how many minutes to add to "now"
	 * @param taskId
	 *            the task id (may be {@code null})
	 * @param type
	 *            the type
	 * @param text
	 *            the text (may be {@code null})
	 */
	public void createEvent(int minutesToPredate, Integer taskId, TypeEnum type, String text) {
		if (minutesToPredate < 0) {
			throw new IllegalArgumentException("no negative minute amount allowed");
		}
		ZonedDateTime targetTime = ZonedDateTime.now().plusMinutes(minutesToPredate);
		createEvent(targetTime.toOffsetDateTime(), taskId, type, text);
	}

	/**
	 * Create a new event at the given time.
	 *
	 * @param dateTime
	 *            the time for which the new event should be created
	 * @param taskId
	 *            the task id (may be {@code null})
	 * @param type
	 *            the type
	 * @param text
	 *            the text (may be {@code null})
	 */
	public void createEvent(OffsetDateTime dateTime, Integer taskId, TypeEnum type, String text) {
		createEvent(dateTime, taskId, type, text, false);
	}

	/**
	 * Create a new event at the given time.
	 *
	 * @param dateTime
	 *            the time for which the new event should be created
	 * @param taskId
	 *            the task id (may be {@code null})
	 * @param type
	 *            the type
	 * @param text
	 *            the text (may be {@code null})
	 * @param insertedByRestore
	 *            true if the event is inserted by a restore. In that case, auto pause and refresh of notifications are
	 *            suppressed.
	 */
	public void createEvent(OffsetDateTime dateTime, Integer taskId, TypeEnum type, String text, boolean insertedByRestore) {
		if (dateTime == null) {
			throw new IllegalArgumentException("date/time has to be given");
		}
		if (type == null) {
			throw new IllegalArgumentException("type has to be given");
		}

		if (!insertedByRestore && type == TypeEnum.CLOCK_OUT) {
			tryToInsertAutoPause(dateTime);
		}

		Event event = new Event(null, taskId, type.getValue(), dateTime, text);
		Logger.debug("TRACKING: {} @ {} taskId={} text={}", type.name(), dateTime, taskId, text);
		Event inserted = dao.insertEvent(event);
		dao.deleteCacheFrom(event.getDateTime().toLocalDate());

		if (!insertedByRestore) {
			Basics.get(context).safeCheckExternalControls();
		}
		notifyListeners();
		BroadcastUtil.sendEventBroadcast(inserted, context, BroadcastUtil.Action.CREATED);
	}

	// TODO General invalidate function (possibly with notification)
	public void invalidateCacheFrom(OffsetDateTime date) {
		// convert date to home time zone
		LocalDate dbDate = date.atZoneSameInstant(getHomeTimeZone()).toLocalDate();
		dao.deleteCacheFrom(dbDate);
	}

	public void invalidateCacheFrom(LocalDate date) {
		dao.deleteCacheFrom(date);
	}

	/**
	 * Create a new NON-PERSISTENT (!) event of the type CLOCK_OUT_NOW.
	 */
	public static Event createClockOutNowEvent() {
		return new Event(null, null, TypeEnum.CLOCK_OUT_NOW.getValue(), OffsetDateTime.now(), null);
	}

	private void tryToInsertAutoPause(OffsetDateTime dateTime) {
		if (isAutoPauseEnabled() && isAutoPauseApplicable(dateTime)) {
			// insert auto-pause events
			OffsetDateTime begin = dateTime.with(getAutoPauseBegin());
			OffsetDateTime end   = dateTime.with(getAutoPauseEnd());
			Logger.debug("inserting auto-pause, begin={}, end={}", begin, end);

			Event lastBeforePause = dao.getLastEventBefore(begin);
			createEvent(begin, null,TypeEnum.CLOCK_OUT, null);
			createEvent(end, (lastBeforePause == null ? null : lastBeforePause.getTask()), TypeEnum.CLOCK_IN,
				(lastBeforePause == null ? null : lastBeforePause.getText()));
		} else {
			Logger.debug("NOT inserting auto-pause");
		}
	}

	/**
	 * Determines if the auto-pause mechanism is enabled.
	 */
	public boolean isAutoPauseEnabled() {
		return preferences.getBoolean(Key.AUTO_PAUSE_ENABLED.getName(), false);
	}

	/**
	 * Determines if the auto-pause can be applied to the given day.
	 */
	public boolean isAutoPauseApplicable(OffsetDateTime dateTime) {
		OffsetDateTime end = dateTime.with(getAutoPauseEnd());
		// auto-pause is theoretically applicable
		return isAutoPauseTheoreticallyApplicable(dateTime)
			// given time is after auto-pause end, so auto-pause should really be applied
			&& dateTime.isAfter(end);
	}

	private boolean isAutoPauseTheoreticallyApplicable(OffsetDateTime date) {
		OffsetDateTime begin = date.with(getAutoPauseBegin());
		OffsetDateTime end   = date.with(getAutoPauseEnd());

		if (begin.isBefore(end)) {
			Event lastEventBeforeBegin = dao.getLastEventBefore(begin);
			Event lastEventBeforeEnd = dao.getLastEventBefore(end);
				// is clocked in before begin
			return lastEventBeforeBegin != null && lastEventBeforeBegin.getType().equals(TypeEnum.CLOCK_IN.getValue())
				// no event is in auto-pause interval
				&& lastEventBeforeBegin.getId().equals(lastEventBeforeEnd.getId());
		} else {
			// begin is equal to end or (even worse) begin is after end => no auto-pause
			return false;
		}
	}

	/**
	 * Calculates the begin of the auto-pause
	 */
	public LocalTime getAutoPauseBegin() {
		return DateTimeUtil.parseTime(getAutoPauseData(Key.AUTO_PAUSE_BEGIN.getName(), "23.59"));
	}

	/**
	 * Calculates the end of the auto-pause
	 */
	public LocalTime getAutoPauseEnd() {
		return DateTimeUtil.parseTime(getAutoPauseData(Key.AUTO_PAUSE_END.getName(), "00.00"));
	}

	/**
	 * Calculates the length of the auto-pause for the given day (in minutes).<br>
	 * Note: does *not* account for time changeovers
	 */
	public long getAutoPauseDuration() {
		return TimerManager.timeDiff(getAutoPauseBegin(), getAutoPauseEnd());
	}

	private String getAutoPauseData(String key, String defaultTime) {
		String ret = preferences.getString(key, defaultTime);
		ret = DateTimeUtil.refineTime(ret);
		return ret;
	}

	// ======== registration of automatic work time tracking methods ========

	public void activateTrackingMethod(TrackingMethod method) {
		Collection<TrackingMethod> activeMethods = readCurrentlyActiveTrackingMethods();
		if (!activeMethods.contains(method)) {
			activeMethods.add(method);
			writeCurrentlyActiveTrackingMethods(activeMethods);
		}
	}

	public void deactivateTrackingMethod(TrackingMethod method) {
		Collection<TrackingMethod> activeMethods = readCurrentlyActiveTrackingMethods();
		if (activeMethods.contains(method)) {
			activeMethods.remove(method);
			writeCurrentlyActiveTrackingMethods(activeMethods);
		}
	}

	private Collection<TrackingMethod> readCurrentlyActiveTrackingMethods() {
		String activeMethodsString = preferences.getString(context.getString(R.string.keyActiveMethods), "");
		String[] activeMethodsStrings = StringUtils.split(activeMethodsString, ',');
		Collection<TrackingMethod> result = new ArrayList<>(activeMethodsStrings.length);
		for (String activeMethodString : activeMethodsStrings) {
			result.add(TrackingMethod.valueOf(activeMethodString));
		}
		return result;
	}

	private void writeCurrentlyActiveTrackingMethods(Collection<TrackingMethod> methods) {
		StringBuilder result = new StringBuilder();
		for (TrackingMethod method : methods) {
			if (result.length() > 0) {
				result.append(",");
			}
			result.append(method.name());
		}
		final Editor editor = preferences.edit();
		editor.putString(context.getString(R.string.keyActiveMethods), result.toString());
		editor.commit();
	}

	public boolean clockInWithTrackingMethod(TrackingMethod method) {
		boolean currentlyClockedInWithMethod = getTrackingMethodClockInState(method);
		if (getTrackingMethodsGenerateEventsSeparately()) {
			Logger.debug("clocking in with method {} forcibly", method.name());
			return setTrackingMethodClockInStateForcibly(method, true);
		} else if (currentlyClockedInWithMethod) {
			Logger.debug("already clocked in with method {}", method.name());
			return false;
		} else {
			Logger.debug("clocking in with method {}", method.name());
			return setTrackingMethodClockInState(method, true);
		}
	}

	public boolean clockOutWithTrackingMethod(TrackingMethod method) {
		boolean currentlyClockedInWithMethod = getTrackingMethodClockInState(method);
		if (getTrackingMethodsGenerateEventsSeparately()) {
			Logger.debug("clocking out with method {} forcibly", method.name());
			return setTrackingMethodClockInStateForcibly(method, false);
		} else if (!currentlyClockedInWithMethod) {
			Logger.debug("not clocked in with method {}", method.name());
			return false;
		} else {
			Logger.debug("clocking out with method {}", method.name());
			return setTrackingMethodClockInState(method, false);
		}
	}

	private boolean getTrackingMethodClockInState(TrackingMethod method) {
		return preferences.getBoolean(context.getString(method.getPreferenceKeyId()), false);
	}

	private boolean getTrackingMethodsGenerateEventsSeparately() {
		return preferences
			.getBoolean(context.getString(R.string.keyEachTrackingMethodGeneratesEventsSeparately), false);
	}

	private boolean setTrackingMethodClockInState(TrackingMethod method, boolean state) {
		final Editor editor = preferences.edit();
		editor.putBoolean(context.getString(method.getPreferenceKeyId()), state);
		editor.commit();
		return createEventIfNecessary(method, state);
	}

	private boolean setTrackingMethodClockInStateForcibly(TrackingMethod method, boolean state) {
		final Editor editor = preferences.edit();
		editor.putBoolean(context.getString(method.getPreferenceKeyId()), state);
		editor.commit();
		return createEventForcibly(method, state);
	}

	private boolean createEventIfNecessary(TrackingMethod method, boolean state) {
		if (state) {
			// method is clocked in now - should we generate a clock-in event?
			if (!isClockedInWithAnyOtherTrackingMethod(method) && !isTracking()) {
				// we are not clocked in already by hand or by another method, so generate an event (first method
				// clocking in)
				startTracking(0, null, null);
				Logger.debug("method {}: started tracking", method);
				return true;
			} else {
				Logger.debug("method {}: NOT started tracking (was not first method or already clocked in manually)",
					method);
				return false;
			}
		} else {
			// method is clocked out now - should we generate a clock-out event?
			if (!isClockedInWithAnyOtherTrackingMethod(method) && isTracking()) {
				// we are not clocked in by hand or by another method, so generate an event (last method clocking out)
				stopTracking(0);
				Logger.debug("method {}: stopped tracking", method);
				return true;
			} else {
				Logger.debug("method {}: NOT stopped tracking (was not last method or already clocked out manually)",
					method);
				return false;
			}
		}
	}

	private boolean createEventForcibly(TrackingMethod method, boolean state) {
		if (state) {
			// method is clocked in now - should we generate a clock-in event?
			if (!isTracking()) {
				startTracking(0, null, null);
				Logger.debug("method {}: started tracking forcibly", method);
				return true;
			} else {
				Logger.debug("method {}: NOT started tracking forcibly (already clocked in)",
					method);
				return false;
			}
		} else {
			// method is clocked out now - should we generate a clock-out event?
			if (isTracking()) {
				stopTracking(0);
				Logger.debug("method {}: stopped tracking forcibly", method);
				return true;
			} else {
				Logger.debug("method {}: NOT stopped tracking forcibly (already clocked out)",
					method);
				return false;
			}
		}
	}

	private boolean isClockedInWithAnyOtherTrackingMethod(TrackingMethod methodToIgnore) {
		Collection<TrackingMethod> activeMethods = readCurrentlyActiveTrackingMethods();
		for (TrackingMethod method : activeMethods) {
			if (method.equals(methodToIgnore)) {
				continue;
			}
			if (isClockedInWithTrackingMethod(method)) {
				return true;
			}
		}
		return false;
	}

	private boolean isClockedInWithTrackingMethod(TrackingMethod method) {
		return getTrackingMethodClockInState(method);
	}

	public static String formatTime(long time) {
		String sgn = (time < 0) ? "-" : "";
		return String.format(Locale.US, "%s%02d:%02d", sgn, Math.abs(time / 60), Math.abs(time % 60));
	}

	public static String formatDecimal(long time) {
		double rounded = Math.round(time / 60.0 * 100.0) / 100.0;
		return Double.toString(rounded);
	}

	public static long timeDiff(OffsetDateTime startTime, OffsetDateTime endTime) {
		return ChronoUnit.MINUTES.between(startTime.truncatedTo(ChronoUnit.MINUTES), endTime.truncatedTo(ChronoUnit.MINUTES));
	}

	public static long timeDiff(LocalTime startTime, LocalTime endTime) {
		return ChronoUnit.MINUTES.between(startTime.truncatedTo(ChronoUnit.MINUTES), endTime.truncatedTo(ChronoUnit.MINUTES));
	}
}
