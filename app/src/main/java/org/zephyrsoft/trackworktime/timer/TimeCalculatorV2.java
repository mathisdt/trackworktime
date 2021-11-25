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

import org.apache.commons.lang3.NotImplementedException;
import org.pmw.tinylog.Logger;
import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.FlexiReset;
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.model.Target;
import org.zephyrsoft.trackworktime.model.TargetEnum;
import org.zephyrsoft.trackworktime.model.TimeInfo;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.util.List;

/**
 * Calculates the the work times for a sequence of days
 */
public class TimeCalculatorV2 {

	/**
	 * One day's worth of interpreted events.
	 */
	public static class DayInfo {
		public static final int TYPE_REGULAR_FREE = 0;
		public static final int TYPE_FREE = 1;
		public static final int TYPE_REGULAR_WORK = 2;
		public static final int TYPE_SPECIAL_GRANT = 3;

		private int dayType;

		private OffsetDateTime timeIn = null;
		private OffsetDateTime timeOut = null;
		private long timeWorked = 0;
		private Long timeBalance = null;

		private LocalDate date = null;
		private boolean today = false;
		private boolean withEvents = false;

		public LocalDateTime getTimeIn() {
			if (timeIn != null) {
				return timeIn.toLocalDateTime();
			} else {
				return null;
			}
		}

		public LocalDateTime getTimeOut() {
			if (timeOut != null) {
				return timeOut.toLocalDateTime();
			} else {
				return null;
			}
		}

		public long getTimeWorked() {
			return timeWorked;
		}

		public Long getTimeFlexi() {
			return timeBalance;
		}

		public boolean isToday() {
			return this.today;
		}

		public LocalDate getDate() {
			return this.date;
		}

		public boolean isWorkDay() {
			return this.dayType > TYPE_FREE;
		}

		public int getType() {
			return this.dayType;
		}

		public boolean containsEvents() {
			return this.withEvents;
		}
	}

	private final DAO dao;
	private final TimerManager timerManager;
	private final boolean handleFlexiTime;
	private final ZoneId zoneId;

	private LocalDate startDate;
	private Event lastEventBeforeDay;
	private LocalDate currentDate;
	private int dayType;

	private FlexiReset flexiReset;
	private LocalDate nextFlexiReset;

	private long actualStart = 0;
	private long actual = 0;
	private long targetStart = 0;
	private long target = 0;


	// status of current day
	private boolean isInFuture;
	private boolean currentDayHasEvents = false;
	private OffsetDateTime timeIn;
	private OffsetDateTime timeOut;
	private long currentDayActual = 0;
	private long currentDayTarget = 0;
	private long currentBalance = 0;
	private int futureWorkDays = -1;

	public TimeCalculatorV2(DAO dao, TimerManager timerManager, LocalDate startDate, boolean handleFlexiTime) {
		this.dao = dao;
		this.timerManager = timerManager;

		this.handleFlexiTime = handleFlexiTime;
		if (handleFlexiTime) {
			this.flexiReset = timerManager.getFlexiReset();
		} else {
			this.flexiReset = FlexiReset.NONE;
		}

		this.zoneId = timerManager.getHomeTimeZone();

		setStartDate(startDate);
	}

	private void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
		this.currentDate = startDate.minusDays(1);

		// get last event before start
		this.lastEventBeforeDay =
				dao.getLastEventBefore(this.startDate.atStartOfDay(zoneId).toOffsetDateTime());

		// get next flexi reset
		if (flexiReset != FlexiReset.NONE) {
			nextFlexiReset = flexiReset.getNextResetDate(currentDate);
		}
	}

	public void setStartSums(long actual, long target) {
		if (this.actual != 0 || this.target != 0) {
			throw new UnsupportedOperationException("Cannot change sums after calculation.");
		}

		Logger.debug("Setting start sums: {} / {}", actual, target);

		this.actualStart = actual;
		this.targetStart = target;
		this.actual = actualStart;
		this.target = targetStart;

		this.currentBalance = actual - target;
	}

	public void setStartSums(TimeInfo info) {
		setStartSums(info.getActual(), info.getTarget());
	}

	/**
	 * Calculate the worked time from the list of events
	 *
	 * @param events List of events
	 * @return work time in Minutes
	 */
	private long calculateWorkTime(List<Event> events) {
		long workedTime = 0;

		if ((events != null && !events.isEmpty())) {

			OffsetDateTime clockedInSince = null;
			Event firstClockInEvent = null;
			Event effectiveClockOutEvent = null;


			if (lastEventBeforeDay != null) {

				if (TimerManager.isClockInEvent(lastEventBeforeDay)
						&& !TimerManager.isClockInEvent(events.get(0))) {

					// clocked in since beginning of day
					clockedInSince = currentDate.atStartOfDay(zoneId).toOffsetDateTime();
					firstClockInEvent = lastEventBeforeDay;

					timeIn = clockedInSince;
				}
			}

			for (Event event : events) {
				OffsetDateTime eventTime = event.getDateTime();

				// clock-in event while not clocked in? => remember time
				if (clockedInSince == null && TimerManager.isClockInEvent(event)) {
					clockedInSince = eventTime;

					if (firstClockInEvent == null) {
						firstClockInEvent = event;
					}
				}

				// clock-out event while clocked-in? => add time since last clock-in to result
				if (clockedInSince != null && TimerManager.isClockOutEvent(event)) {

					workedTime += TimerManager.timeDiff(clockedInSince, eventTime);

					clockedInSince = null;
					effectiveClockOutEvent = event;
				}
			}

			if (firstClockInEvent != null && timeIn == null) {
				timeIn = firstClockInEvent.getDateTime();
			} else {
				// clocked in before the begin of day or no clock-in event this day
			}

			if (clockedInSince != null) {
				// still clocked-in at the end of the day
				timeOut = currentDate.atTime(LocalTime.MAX).atZone(zoneId).toOffsetDateTime();

				workedTime += TimerManager.timeDiff(clockedInSince, timeOut);
			} else if (effectiveClockOutEvent != null) {

				// effectiveClockOutEvent is null if there are only clock out events
				timeOut = effectiveClockOutEvent.getDateTime();
			}

			// update last event
			lastEventBeforeDay = events.get(events.size() - 1);

		} else if (TimerManager.isClockInEvent(lastEventBeforeDay)) {
			// although there are no events on this day, the user is clocked in all day long -
			// else there would be a CLOCK_OUT_NOW event!
			timeIn = currentDate.atTime(LocalTime.MIN).atZone(zoneId).toOffsetDateTime();
			timeOut = currentDate.atTime(LocalTime.MAX).atZone(zoneId).toOffsetDateTime();

			workedTime += 24 * 60;
		}

		Logger.debug("Time worked: {}", workedTime);

		return workedTime;
	}

	private long calculateTargetTime(Target specialTarget, TargetEnum targetEnum) {
		long targetTime = 0;

		if (targetEnum == TargetEnum.DAY_SET) {
			// overwrite target work time

			if (specialTarget.getValue() == 0) {
				if (dayType == DayInfo.TYPE_REGULAR_WORK) {
					dayType = DayInfo.TYPE_FREE;
				}

				targetTime = 0;
			} else {
				targetTime = specialTarget.getValue();
			}

		} else if (dayType == DayInfo.TYPE_REGULAR_WORK) {
			targetTime = timerManager.getNormalWorkDurationFor(currentDate.getDayOfWeek());
		}

		return targetTime;
	}

	/**
	 * Calculate the time sum, flexi value and in/out times for one day.
	 */
	public void calculateNextDay() {
		// reset
		timeIn = null;
		timeOut = null;

		// move to next day
		currentDate = currentDate.plusDays(1);


		ZonedDateTime now = ZonedDateTime.now(zoneId);

		boolean isToday = currentDate.isEqual(now.toLocalDate());
		isInFuture = currentDate.isAfter(now.toLocalDate());


		// calculate work time if there are events
		long workedTime = 0;
		List<Event> events;

		if (!isToday) {
			Logger.debug("Fetching events for day: {}", currentDate);
			events = dao.getEventsOnDay(currentDate.atStartOfDay(zoneId));

		} else {
			Logger.debug("Fetching events for today");
			events = dao.getEventsOnDayUpTo(now);

			currentDayHasEvents = (events != null && !events.isEmpty());

			// currently clocked in
			if ((currentDayHasEvents && TimerManager.isClockInEvent(events.get(events.size() - 1)))
					|| (!currentDayHasEvents && TimerManager.isClockInEvent(lastEventBeforeDay))) {

				// handle autopause
				// try to substract the auto-pause for today because it might be not counted in the database yet
				OffsetDateTime eventTime = now.toOffsetDateTime();

				if (timerManager.isAutoPauseEnabled() && timerManager.isAutoPauseApplicable(eventTime)) {
					OffsetDateTime autoPauseBegin = eventTime.with(timerManager.getAutoPauseBegin());
					OffsetDateTime autoPauseEnd = eventTime.with(timerManager.getAutoPauseEnd());

					workedTime -= TimerManager.timeDiff(autoPauseBegin, autoPauseEnd);
				}


				// if there is no future clock-out event, add clock-out-now
				Event event = dao.getFirstEventAfterWithType(now,TypeEnum.CLOCK_OUT);
				if (event == null) {
					events.add(TimerManager.createClockOutNowEvent());
				}
			}

			// add remaining events for today
			List<Event> eventsAfter = dao.getEventsOnDayAfter(now);
			events.addAll(eventsAfter);
		}

		currentDayHasEvents = (events != null && !events.isEmpty());

		workedTime += calculateWorkTime(events);

		// always show real worked time
		currentDayActual = workedTime;


		// default day types
		DayOfWeek weekDay = currentDate.getDayOfWeek();

		if (timerManager.isWorkDay(weekDay)) {
			dayType = DayInfo.TYPE_REGULAR_WORK;
		} else {
			dayType = DayInfo.TYPE_REGULAR_FREE;
		}


		if (handleFlexiTime) {
			// handle flexi reset
			if (nextFlexiReset != null && nextFlexiReset.isEqual(currentDate)) {
				target = actual;	// reset target work time

				nextFlexiReset = flexiReset.getNextResetDate(currentDate);
			}

			// get special target
			Target specialTarget = dao.getDayTarget(currentDate);
			TargetEnum targetEnum = specialTarget != null ? TargetEnum.byValue(specialTarget.getType()) : null;

			// get target work time
			currentDayTarget = calculateTargetTime(specialTarget, targetEnum);

			if (targetEnum != null) {
				switch (targetEnum) {
					case DAY_IGNORE:
						dayType = DayInfo.TYPE_FREE;
						break;
					case DAY_SET:
						if (specialTarget.getValue() == 0) {
							dayType = DayInfo.TYPE_FREE;
							break;
						} else {
							dayType = DayInfo.TYPE_SPECIAL_GRANT;
							break;
						}
					case DAY_GRANT:
						dayType = DayInfo.TYPE_SPECIAL_GRANT;
						break;
				}
			}

			// handle special case
			if (!isToday && targetEnum == TargetEnum.DAY_GRANT) {
				if (currentDayTarget == 0) {
					Logger.error("Target work time granted on free day!");
				} else {
					if (workedTime < currentDayTarget) {
						workedTime = currentDayTarget;
					}
				}
			}

			// update total
			actual += workedTime;
			target += currentDayTarget;


			// keep balance unchanged if DAY_IGNORE
			if (targetEnum != TargetEnum.DAY_IGNORE) {
				currentBalance = actual - target;
			}
		} else {
			// update total
			actual += workedTime;
		}
	}

	public DayInfo getNextDayInfo() {
		calculateNextDay();

		DayInfo ret = new DayInfo();
		ret.dayType = dayType;
		ret.date = currentDate;
		ret.today = currentDate.isEqual(LocalDate.now());
		ret.withEvents = currentDayHasEvents;
		ret.timeIn = timeIn;
		ret.timeOut = timeOut;
		ret.timeWorked = currentDayActual;
		ret.timeBalance = currentBalance;

		return ret;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getCurrentDate() {
		return currentDate;
	}

	public long getTargetSum() {
		return target - targetStart;
	}

	public long getTotalTarget() {
		return target;
	}

	public long getTimeWorked() {
		return actual - actualStart;
	}

	public long getTotalTimeWorked() {
		return actual;
	}

	public int getFutureWorkDays() {
		if (futureWorkDays == -1) {
			throw new NotImplementedException("Not implemented for general use.");
		}

		return futureWorkDays;
	}

	public boolean withFlexiTime() {
		return handleFlexiTime && startDate.isBefore(LocalDate.now());
	}

	public long getBalance() {
		return actual - target;
	}

	public long getCurrentDayTarget() {
		return currentDayTarget;
	}

	public int getCurrentDayBalance() {
		if (futureWorkDays == -1) {
			throw new NotImplementedException("Not implemented for general use.");
		}
		return (int)(currentDayActual - currentDayTarget);
	}


	/** Helper functions **/
	public void calculatePeriod(PeriodEnum period, boolean includeFlexiTime) {
		if (!this.currentDate.isBefore(this.startDate)) {
			throw new UnsupportedOperationException("Time calculator cannot be reused.");
		}

		int numDays;
		long currentDayActual = 0;
		long currentDayTarget = 0;

		switch (period) {
			case DAY:
				numDays = 1;
				break;

			case WEEK:
				// move start date to beginning of week
				setStartDate(DateTimeUtil.getWeekStart(startDate));
				numDays = 7;
				break;

			default:
				throw new UnsupportedOperationException("Use cache for longer periods.");
		}

		if (includeFlexiTime) {
			setStartSums(timerManager.getTimesAt(startDate));
		}

		futureWorkDays = 0;

		for (int i = 0; i < numDays; i++) {
			calculateNextDay();

			if (isInFuture) {
				if (dayType == DayInfo.TYPE_REGULAR_WORK) {
					futureWorkDays++;
				}
			} else {
				currentDayActual = this.actual;
				currentDayTarget = this.target;
			}
		}

		// restore value of "today"
		this.currentDayActual = currentDayActual;
		this.currentDayTarget = currentDayTarget;
	}
}
