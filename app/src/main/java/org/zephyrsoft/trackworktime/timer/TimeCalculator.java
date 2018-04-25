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

import hirondelle.date4j.DateTime;
import hirondelle.date4j.DateTime.DayOverflow;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.DayLine;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.model.Range;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.Unit;
import org.zephyrsoft.trackworktime.model.WeekDayEnum;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

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
	public Map<Task, TimeSum> calculateSums(DateTime beginOfPeriod, DateTime endOfPeriod, List<Event> events) {
		Map<Task, TimeSum> ret = new HashMap<Task, TimeSum>();
		if (events == null || events.isEmpty()) {
			return ret;
		}

		DateTime timeOfFirstEvent = DateTimeUtil.stringToDateTime(events.get(0).getTime());
		Event lastEventBefore = dao.getLastEventBefore(timeOfFirstEvent);

		DateTime clockedInSince = null;
		Task currentTask = null;

		if (TimerManager.isClockInEvent(lastEventBefore)) {
			// clocked in since begin of period
			clockedInSince = beginOfPeriod;
			currentTask = lastEventBefore.getTask() != null ? dao.getTask(lastEventBefore.getTask()) : null;
		}

		for (Event event : events) {
			DateTime eventTime = DateTimeUtil.stringToDateTime(event.getTime());
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

	private static void countTime(Map<Task, TimeSum> mapForCounting, Task task, DateTime from, DateTime to) {
		// fetch sum up to now
		TimeSum sumForTask = mapForCounting.get(task);
		if (sumForTask == null) {
			sumForTask = new TimeSum();
			mapForCounting.put(task, sumForTask);
		}
		// add new times to sum
		long minutesWorked = from.numSecondsFrom(to) / 60;
		if (minutesWorked > Integer.MAX_VALUE - 60) {
			// this is extremely unlikely, someone would have to work 4084 years without pause...
			int correctedMinutesWorked = Integer.MAX_VALUE - 60;
			Logger.warn("could not handle {} minutes, number is too high - taking {} instead",
				minutesWorked, correctedMinutesWorked);
		}
		sumForTask.add(0, (int) minutesWorked);
	}

	/**
	 * Calculate the time sum, flexi value and in/out times for one day.
	 */
	public DayLine calulateOneDay(DateTime day, List<Event> eventsOfOneDay) {
		DayLine ret = new DayLine();

		WeekDayEnum weekDay = WeekDayEnum.getByValue(day.getWeekDay());
		if (Basics.getInstance().getPreferences().getBoolean(Key.ENABLE_FLEXI_TIME.getName(), false)
			&& timerManager.isWorkDay(weekDay)) {
			// substract the "normal" work time for one day
			int normalWorkTimeInMinutes = timerManager.getNormalWorkDurationFor(weekDay);
			ret.getTimeFlexi().substract(0, normalWorkTimeInMinutes);
		}

		if (eventsOfOneDay == null || eventsOfOneDay.isEmpty()) {
			return ret;
		}

		DateTime timeOfFirstEvent = DateTimeUtil.stringToDateTime(eventsOfOneDay.get(0).getTime());
		Event lastEventBeforeToday = dao.getLastEventBefore(timeOfFirstEvent);
		DateTime lastEventBeforeTodayTime = (lastEventBeforeToday != null ? DateTimeUtil
			.stringToDateTime(lastEventBeforeToday.getTime()) : null);
		if (!eventsOfOneDay.isEmpty()) {
			// take special care of the event type (CLOCK_IN vs. CLOCK_OUT/CLOCK_OUT_NOW)
			Event firstClockInEvent = null;
			for (Event event : eventsOfOneDay) {
				if (TimerManager.isClockInEvent(event)) {
					firstClockInEvent = event;
					break;
				}
			}
			Event effectiveClockOutEvent = null;
			for (int i = eventsOfOneDay.size() - 1; i >= 0; i--) {
				Event event = eventsOfOneDay.get(i);
				if (TimerManager.isClockOutEvent(event)) {
					effectiveClockOutEvent = event;
				}
				if (TimerManager.isClockInEvent(event)) {
					break;
				}
			}

			if (TimerManager.isClockInEvent(lastEventBeforeToday) && lastEventBeforeTodayTime != null
				&& DateTimeUtil.isInPast(lastEventBeforeTodayTime)
				&& !TimerManager.isClockInEvent(eventsOfOneDay.get(0))) {
				// clocked in since begin of day
				ret.setTimeIn(timeOfFirstEvent.getStartOfDay());
			} else if (firstClockInEvent != null) {
				ret.setTimeIn(DateTimeUtil.stringToDateTime(firstClockInEvent.getTime()));
			} else {
				// apparently not clocked in before begin of day and no clock-in event
			}

			if (effectiveClockOutEvent != null) {
				ret.setTimeOut(DateTimeUtil.stringToDateTime(effectiveClockOutEvent.getTime()));
			} else {
				ret.setTimeOut(timeOfFirstEvent.getEndOfDay());
			}

			TimeSum amountWorked = timerManager.calculateTimeSum(timeOfFirstEvent, PeriodEnum.DAY);
			ret.setTimeWorked(amountWorked);
		} else if (TimerManager.isClockInEvent(lastEventBeforeToday)
			&& DateTimeUtil.isInPast(timeOfFirstEvent.getStartOfDay())) {
			// although there are no events on this day, the user is clocked in all day long - else there would be a
			// CLOCK_OUT_NOW event!
			ret.setTimeIn(timeOfFirstEvent.getStartOfDay());
			ret.setTimeOut(timeOfFirstEvent.getEndOfDay());
			ret.getTimeWorked().add(24, 0);
		}

		ret.getTimeFlexi().addOrSubstract(ret.getTimeWorked());

		return ret;
	}

	public DateTime[] calculateBeginAndEnd(Range range, Unit unit) {
		DateTime now = DateTimeUtil.getCurrentDateTime();
		DateTime beginOfTimeFrame;
		DateTime endOfTimeFrame;

		int daysInLastUnit;
		switch (unit) {
			case WEEK:
				beginOfTimeFrame = DateTimeUtil.getWeekStart(now);
				endOfTimeFrame = beginOfTimeFrame.plusDays(7);
				daysInLastUnit = 7;
				break;
			case MONTH:
				beginOfTimeFrame = now.getStartOfMonth();
				endOfTimeFrame = now.getEndOfMonth();
				DateTime lastMonthBegin = beginOfTimeFrame.minus(0, 1, 0, 0, 0, 0, 0, DayOverflow.Spillover);
				daysInLastUnit = lastMonthBegin.numDaysFrom(beginOfTimeFrame);
				break;
			case YEAR:
				beginOfTimeFrame = new DateTime(now.getYear(), 1, 1, 0, 0, 0, 0);
				endOfTimeFrame = beginOfTimeFrame.plus(1, 0, 0, 0, 0, 0, 0, DayOverflow.Spillover);
				DateTime lastYearBegin = beginOfTimeFrame.minus(1, 0, 0, 0, 0, 0, 0, DayOverflow.Spillover);
				daysInLastUnit = lastYearBegin.numDaysFrom(beginOfTimeFrame);
				break;
			default:
				throw new IllegalArgumentException("unknown unit");
		}

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
			default:
				throw new IllegalArgumentException("unknown range");
		}
		return new DateTime[] { beginOfTimeFrame, endOfTimeFrame };
	}

	/**
	 * Includes the parameter "from" as this also is a range start (although it is not necessarily the start of a
	 * complete range).
	 */
	public List<DateTime> calculateRangeBeginnings(Unit unit, DateTime from, DateTime to) {
		List<DateTime> ret = new LinkedList<DateTime>();
		ret.add(from);

		DateTime current = null;
		switch (unit) {
			case WEEK:
				current = DateTimeUtil.getWeekStart(from).plusDays(7);
				while (current.lt(to)) {
					ret.add(current);
					current = current.plusDays(7);
				}
				break;
			case MONTH:
				current = from.getStartOfMonth().plus(0, 1, 0, 0, 0, 0, 0, DayOverflow.Spillover);
				while (current.lt(to)) {
					ret.add(current);
					current = current.plus(0, 1, 0, 0, 0, 0, 0, DayOverflow.Spillover);
				}
				break;
			case YEAR:
				current = new DateTime(from.getYear() + 1, 1, 1, 0, 0, 0, 0);
				while (current.lt(to)) {
					ret.add(current);
					current = current.plus(1, 0, 0, 0, 0, 0, 0, DayOverflow.Spillover);
				}
				break;
			default:
				throw new IllegalArgumentException("unknown unit");
		}

		return ret;
	}

}
