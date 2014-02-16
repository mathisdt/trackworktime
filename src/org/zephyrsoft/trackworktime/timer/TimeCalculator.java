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

import java.util.List;

import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.DayLine;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.WeekDayEnum;
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

	public DayLine calulateOneDay(List<Event> eventsOfOneDay) {
		DayLine ret = new DayLine();
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

			ret.getTimeFlexi().addOrSubstract(amountWorked);
			// substract the "normal" work time for one day
			WeekDayEnum weekDay = WeekDayEnum.getByValue(timeOfFirstEvent.getWeekDay());
			int normalWorkTimeInMinutes = timerManager.getNormalWorkDurationFor(weekDay);
			ret.getTimeFlexi().substract(0, normalWorkTimeInMinutes);

		} else {
			if (TimerManager.isClockInEvent(lastEventBeforeToday)
				&& DateTimeUtil.isInPast(timeOfFirstEvent.getStartOfDay())) {
				// although there are no events on this day, the user is clocked in all day long - else there would be a
				// CLOCK_OUT_NOW event!
				ret.setTimeIn(timeOfFirstEvent.getStartOfDay());
				ret.setTimeOut(timeOfFirstEvent.getEndOfDay());
				ret.getTimeWorked().add(24, 0);
				ret.getTimeFlexi().add(24, 0);
				// substract the "normal" work time for one day
				WeekDayEnum weekDay = WeekDayEnum.getByValue(timeOfFirstEvent.getWeekDay());
				int normalWorkTimeInMinutes = timerManager.getNormalWorkDurationFor(weekDay);
				ret.getTimeFlexi().substract(0, normalWorkTimeInMinutes);
			} else {
				WeekDayEnum weekDay = WeekDayEnum.getByValue(timeOfFirstEvent.getWeekDay());
				if (timerManager.isWorkDay(weekDay)) {
					// substract the "normal" work time for one day
					int normalWorkTimeInMinutes = timerManager.getNormalWorkDurationFor(weekDay);
					ret.getTimeFlexi().substract(0, normalWorkTimeInMinutes);
				}
			}
		}

		return ret;
	}

}
