/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime.timer;

import org.pmw.tinylog.Logger;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZonedDateTime;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Range;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.Unit;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.threeten.bp.temporal.ChronoUnit.DAYS;
import static org.threeten.bp.temporal.ChronoUnit.MINUTES;
import static org.threeten.bp.temporal.TemporalAdjusters.firstDayOfMonth;
import static org.threeten.bp.temporal.TemporalAdjusters.firstDayOfYear;
import static org.threeten.bp.temporal.TemporalAdjusters.lastDayOfMonth;

/**
 * Calculates the actual work times from events.
 *
 * @author Mathis Dirksen-Thedens
 */
public class TimeCalculator {

	private final DAO dao;
	private final TimerManager timerManager;

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

	private static void countTime(Map<Task, TimeSum> mapForCounting, Task task, OffsetDateTime from, OffsetDateTime to) {
		// fetch sum up to now
		TimeSum sumForTask = mapForCounting.get(task);
		if (sumForTask == null) {
			sumForTask = new TimeSum();
			mapForCounting.put(task, sumForTask);
		}
		// add new times to sum
		long minutesWorked = MINUTES.between(from, to);
		if (minutesWorked > Integer.MAX_VALUE - 60) {
			// this is extremely unlikely, someone would have to work 4084 years without pause...
			int correctedMinutesWorked = Integer.MAX_VALUE - 60;
			Logger.warn("could not handle {} minutes, number is too high - taking {} instead",
				minutesWorked, correctedMinutesWorked);
		}
		sumForTask.add(0, (int) minutesWorked);
	}

	public ZonedDateTime[] calculateBeginAndEnd(Range range, Unit unit) {
		ZonedDateTime now =  ZonedDateTime.now(timerManager.getHomeTimeZone());
		ZonedDateTime beginOfTimeFrame;
		ZonedDateTime endOfTimeFrame;

		long daysInLastUnit;
		switch (unit) {
			case WEEK:
				beginOfTimeFrame = DateTimeUtil.getWeekStart(now);
				endOfTimeFrame = beginOfTimeFrame.plusDays(7);
				daysInLastUnit = 7;
				break;
			case MONTH:
				beginOfTimeFrame = now.with(firstDayOfMonth());
				endOfTimeFrame = now.with(lastDayOfMonth());

				ZonedDateTime lastMonthBegin = beginOfTimeFrame.minusMonths(1);
				daysInLastUnit = DAYS.between(lastMonthBegin, beginOfTimeFrame);
				break;
			case YEAR:
				beginOfTimeFrame = now.with(firstDayOfYear());
				endOfTimeFrame = beginOfTimeFrame.plusYears(1);

				ZonedDateTime lastYearBegin = beginOfTimeFrame.minusYears(1);
				daysInLastUnit = DAYS.between(lastYearBegin, beginOfTimeFrame);
				break;
			default:
				throw new IllegalArgumentException("unknown unit");
		}

		// time frame is expected to align with day boundaries
		beginOfTimeFrame = beginOfTimeFrame.with(LocalTime.MIN);
		endOfTimeFrame = endOfTimeFrame.with(LocalTime.MAX);

		switch (range) {
			case CURRENT:
				// nothing to do
				break;
			case LAST_AND_CURRENT:
				beginOfTimeFrame = beginOfTimeFrame.minusDays(daysInLastUnit);
				break;
			case LAST:
				endOfTimeFrame = beginOfTimeFrame;
				beginOfTimeFrame = beginOfTimeFrame.minusDays(daysInLastUnit);
				break;
			case ALL_DATA:
				List<Event> allEvents = dao.getAllEvents();
				if (allEvents.isEmpty()) {
					// FIXME check twice
					beginOfTimeFrame = now.with(LocalTime.MIN);
					endOfTimeFrame = now.with(LocalTime.MAX);
				} else {
					// FIXME check twice
					beginOfTimeFrame = allEvents.get(0).getDateTime().atZoneSameInstant(timerManager.getHomeTimeZone()).with(LocalTime.MIN);
					endOfTimeFrame = allEvents.get(allEvents.size() - 1).getDateTime().atZoneSameInstant(timerManager.getHomeTimeZone()).plusDays(1).with(LocalTime.MAX);
				}
				break;
			default:
				throw new IllegalArgumentException("unknown range");
		}
		return new ZonedDateTime[]{beginOfTimeFrame, endOfTimeFrame};
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
