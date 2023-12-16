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

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Range;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.report.TaskAndHint;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.Unit;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Calculates the actual work times from events.
 */
public class TimeCalculator {

	private final DAO dao;
	private final TimerManager timerManager;

	private static class RangeAndUnit {
		private final Range range;
		private final Unit unit;

		public RangeAndUnit(Range range, Unit unit) {
			this.range = range;
			this.unit = unit;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			RangeAndUnit that = (RangeAndUnit) o;
			return range == that.range && unit == that.unit;
		}

		@Override
		public int hashCode() {
			return Objects.hash(range, unit);
		}
	}

	private static final Map<RangeAndUnit, Function<ZonedDateTime, ZonedDateTime[]>> TIMESPAN_FUNCTIONS = new HashMap<>();

	static {
		TIMESPAN_FUNCTIONS.put(new RangeAndUnit(Range.CURRENT, Unit.WEEK), reference ->
			new ZonedDateTime[]{reference.with(LocalTime.MIN).with(previousOrSame(DayOfWeek.MONDAY)),
				reference.with(LocalTime.MAX).with(nextOrSame(DayOfWeek.SUNDAY))});
		TIMESPAN_FUNCTIONS.put(new RangeAndUnit(Range.CURRENT, Unit.MONTH), reference ->
			new ZonedDateTime[]{reference.with(LocalTime.MIN).with(firstDayOfMonth()),
				reference.with(LocalTime.MAX).with(lastDayOfMonth())});
		TIMESPAN_FUNCTIONS.put(new RangeAndUnit(Range.CURRENT, Unit.YEAR), reference ->
			new ZonedDateTime[]{reference.with(LocalTime.MIN).with(firstDayOfYear()),
				reference.with(LocalTime.MAX).with(lastDayOfYear())});

		TIMESPAN_FUNCTIONS.put(new RangeAndUnit(Range.LAST_AND_CURRENT, Unit.WEEK), reference ->
			new ZonedDateTime[]{reference.minusDays(7).with(LocalTime.MIN).with(previousOrSame(DayOfWeek.MONDAY)),
				reference.with(LocalTime.MAX).with(nextOrSame(DayOfWeek.SUNDAY))});
		TIMESPAN_FUNCTIONS.put(new RangeAndUnit(Range.LAST_AND_CURRENT, Unit.MONTH), reference ->
			new ZonedDateTime[]{reference.minusMonths(1).with(LocalTime.MIN).with(firstDayOfMonth()),
				reference.with(LocalTime.MAX).with(lastDayOfMonth())});
		TIMESPAN_FUNCTIONS.put(new RangeAndUnit(Range.LAST_AND_CURRENT, Unit.YEAR), reference ->
			new ZonedDateTime[]{reference.minusYears(1).with(LocalTime.MIN).with(firstDayOfYear()),
				reference.with(LocalTime.MAX).with(lastDayOfYear())});

		TIMESPAN_FUNCTIONS.put(new RangeAndUnit(Range.LAST, Unit.WEEK), reference ->
			new ZonedDateTime[]{reference.minusDays(7).with(LocalTime.MIN).with(previousOrSame(DayOfWeek.MONDAY)),
				reference.minusDays(7).with(LocalTime.MAX).with(nextOrSame(DayOfWeek.SUNDAY))});
		TIMESPAN_FUNCTIONS.put(new RangeAndUnit(Range.LAST, Unit.MONTH), reference ->
			new ZonedDateTime[]{reference.minusMonths(1).with(LocalTime.MIN).with(firstDayOfMonth()),
				reference.minusMonths(1).with(LocalTime.MAX).with(lastDayOfMonth())});
		TIMESPAN_FUNCTIONS.put(new RangeAndUnit(Range.LAST, Unit.YEAR), reference ->
			new ZonedDateTime[]{reference.minusYears(1).with(LocalTime.MIN).with(firstDayOfYear()),
				reference.minusYears(1).with(LocalTime.MAX).with(lastDayOfYear())});
	}

	public TimeCalculator(DAO dao, TimerManager timerManager) {
		this.dao = dao;
		this.timerManager = timerManager;
	}

	/**
	 * Calculate the time sums per task in a given time range.
	 */
	public Map<Task, TimeSum> calculateSums(OffsetDateTime beginOfPeriod, OffsetDateTime endOfPeriod, List<Event> events) {
		Map<Task, TimeSum> ret = new HashMap<>();
		if (events == null || events.isEmpty()) {
			return ret;
		}

		OffsetDateTime timeOfFirstEvent = events.get(0).getDateTime();
		Event lastEventBefore = dao.getLastEventBefore(timeOfFirstEvent);

		OffsetDateTime clockedInSince = null;
		Task currentTask = null;

		if (TimerManager.isClockInEvent(lastEventBefore)) {
			// clocked in since begin of period
			clockedInSince = beginOfPeriod;
			currentTask = lastEventBefore.getTask() != null ? dao.getTask(lastEventBefore.getTask()) : null;
		}

		for (Event event : events) {
			OffsetDateTime eventTime = event.getDateTime();
			if (clockedInSince != null) {
				countTime(ret, currentTask, clockedInSince, eventTime);
			}
			if (TimerManager.isClockInEvent(event)) {
				clockedInSince = eventTime;
				currentTask = event.getTask() != null ? dao.getTask(event.getTask()) : null;
			} else {
				clockedInSince = null;
				currentTask = null;
			}
		}

		if (clockedInSince != null) {
			countTime(ret, currentTask, clockedInSince, endOfPeriod);
		}

		return ret;
	}

	/**
	 * Calculate the time sums per task and hint in a given time range.
	 */
	public Map<TaskAndHint, TimeSum> calculateSumsPerTaskAndHint(OffsetDateTime beginOfPeriod, OffsetDateTime endOfPeriod, List<Event> events) {
		Map<TaskAndHint, TimeSum> ret = new HashMap<>();
		if (events == null || events.isEmpty()) {
			return ret;
		}

		OffsetDateTime timeOfFirstEvent = events.get(0).getDateTime();
		Event lastEventBefore = dao.getLastEventBefore(timeOfFirstEvent);

		OffsetDateTime clockedInSince = null;
		TaskAndHint currentTaskAndHint = null;

		if (TimerManager.isClockInEvent(lastEventBefore)) {
			// clocked in since begin of period
			clockedInSince = beginOfPeriod;
			currentTaskAndHint = lastEventBefore.getTask() != null ? new TaskAndHint(lastEventBefore.getText(), dao.getTask(lastEventBefore.getTask())) : null;
		}

		for (Event event : events) {
			OffsetDateTime eventTime = event.getDateTime();
			if (clockedInSince != null) {
				countTime(ret, currentTaskAndHint, clockedInSince, eventTime);
			}
			if (TimerManager.isClockInEvent(event)) {
				clockedInSince = eventTime;
				currentTaskAndHint = event.getTask() != null ? new TaskAndHint(event.getText(), dao.getTask(event.getTask())) : null;
			} else {
				clockedInSince = null;
				currentTaskAndHint = null;
			}
		}

		if (clockedInSince != null) {
			countTime(ret, currentTaskAndHint, clockedInSince, endOfPeriod);
		}

		return ret;
	}

	private static void countTime(Map<Task, TimeSum> mapForCounting, Task task, OffsetDateTime from, OffsetDateTime to) {
		// fetch sum up to now
		TimeSum sumForTask = mapForCounting.get(task);
		if (sumForTask == null) {
			sumForTask = new TimeSum();
			mapForCounting.put(task, sumForTask);
		}
		// add new times to sum
		long minutesWorked = ChronoUnit.MINUTES.between(from, to);
		if (minutesWorked > Integer.MAX_VALUE - 60) {
			// this is extremely unlikely, someone would have to work 4084 years without pause...
			int correctedMinutesWorked = Integer.MAX_VALUE - 60;
			Logger.warn("could not handle {} minutes, number is too high - taking {} instead",
				minutesWorked, correctedMinutesWorked);
		}
		sumForTask.add(0, (int) minutesWorked);
	}

	private static void countTime(Map<TaskAndHint, TimeSum> mapForCounting, TaskAndHint taskAndHint, OffsetDateTime from, OffsetDateTime to) {
		// fetch sum up to now
		TimeSum sumForTask = mapForCounting.get(taskAndHint);
		if (sumForTask == null) {
			sumForTask = new TimeSum();
			mapForCounting.put(taskAndHint, sumForTask);
		}
		// add new times to sum
		long minutesWorked = ChronoUnit.MINUTES.between(from, to);
		if (minutesWorked > Integer.MAX_VALUE - 60) {
			// this is extremely unlikely, someone would have to work 4084 years without pause...
			int correctedMinutesWorked = Integer.MAX_VALUE - 60;
			Logger.warn("could not handle {} minutes, number is too high - taking {} instead",
					minutesWorked, correctedMinutesWorked);
		}
		sumForTask.add(0, (int) minutesWorked);
	}

	public ZonedDateTime[] calculateBeginAndEnd(Range range, Unit unit) {
		ZonedDateTime now = ZonedDateTime.now(timerManager.getHomeTimeZone());

		if (range == Range.ALL_DATA) {
				List<Event> allEvents = dao.getAllEvents();
			if (allEvents.isEmpty()) {
				return new ZonedDateTime[]{now.with(LocalTime.MIN), now.with(LocalTime.MAX)};
			} else {
				return new ZonedDateTime[]{allEvents.get(0).getDateTime().atZoneSameInstant(timerManager.getHomeTimeZone()).with(LocalTime.MIN),
					allEvents.get(allEvents.size() - 1).getDateTime().atZoneSameInstant(timerManager.getHomeTimeZone()).with(LocalTime.MAX)};
			}
		} else {
			Function<ZonedDateTime, ZonedDateTime[]> timespanFunction = TIMESPAN_FUNCTIONS.get(new RangeAndUnit(range, unit));
			if (timespanFunction == null) {
				throw new IllegalArgumentException("unknown combination of range and unit: " + range + " / " + unit);
			}
			return timespanFunction.apply(now);
		}
	}

	/**
	 * Includes the parameter "from" as this also is a range start (although it is not necessarily the start of a
	 * complete range).
	 */
	public List<ZonedDateTime> calculateRangeBeginnings(Unit unit, ZonedDateTime from, ZonedDateTime to) {
		List<ZonedDateTime> ret = new ArrayList<>();
		ret.add(from);

		ZonedDateTime current;
		switch (unit) {
			case DAY:
				current = from.plusDays(1);

				while (current.isBefore(to)) {
					ret.add(current);
					current = current.plusDays(1);
				}
				break;
			case WEEK:
				current = DateTimeUtil.getWeekStart(from).plusDays(7);

				while (current.isBefore(to)) {
					ret.add(current);
					current = current.plusDays(7);
				}
				break;
			case MONTH:
				current = from.withDayOfMonth(1).plusMonths(1);

				while (current.isBefore(to)) {
					ret.add(current);
					current = current.plusMonths(1);
				}
				break;
			case YEAR:
				current = ZonedDateTime.of(LocalDate.of(from.getYear()+1,1,1), LocalTime.MIDNIGHT, from.getZone());

				while (current.isBefore(to)) {
					ret.add(current);
					current = current.plusYears(1);
				}
				break;
			default:
				throw new IllegalArgumentException("unknown unit");
		}

		return ret;
	}

}
