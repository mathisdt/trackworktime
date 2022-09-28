package org.zephyrsoft.trackworktime.timer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfYear;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

import org.junit.Before;
import org.junit.Test;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Range;
import org.zephyrsoft.trackworktime.model.Unit;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class TimeCalculatorTest {

    private final ZonedDateTime now = ZonedDateTime.now();
    private final ZonedDateTime oneYearBack = now.minusYears(1);

    private TimeCalculator timeCalculator;

    @Before
    public void setup() {
        DAO dao = mock(DAO.class);
        TimerManager timerManager = mock(TimerManager.class);

        doReturn(ZoneId.systemDefault()).when(timerManager).getHomeTimeZone();

        doReturn(List.of(
            eventAt(oneYearBack),
            eventAt(now.minusMonths(1)),
            eventAt(now.minusWeeks(1)),
            eventAt(now.minusDays(1)),
            eventAt(now)
        )).when(dao).getAllEvents();

        timeCalculator = new TimeCalculator(dao, timerManager);
    }

    @Test
    public void calculateBeginAndEnd() {
        assertRange(now.with(LocalTime.MIN).with(previousOrSame(DayOfWeek.MONDAY)),
            now.with(LocalTime.MAX).with(nextOrSame(DayOfWeek.SUNDAY)),
            Range.CURRENT,
            Unit.WEEK);
        assertRange(now.with(LocalTime.MIN).with(firstDayOfMonth()),
            now.with(LocalTime.MAX).with(lastDayOfMonth()),
            Range.CURRENT,
            Unit.MONTH);
        assertRange(now.with(LocalTime.MIN).with(firstDayOfYear()),
            now.with(LocalTime.MAX).with(lastDayOfYear()),
            Range.CURRENT,
            Unit.YEAR);

        assertRange(now.minusDays(7).with(LocalTime.MIN).with(previousOrSame(DayOfWeek.MONDAY)),
            now.with(LocalTime.MAX).with(nextOrSame(DayOfWeek.SUNDAY)),
            Range.LAST_AND_CURRENT,
            Unit.WEEK);
        assertRange(now.minusMonths(1).with(LocalTime.MIN).with(firstDayOfMonth()),
            now.with(LocalTime.MAX).with(lastDayOfMonth()),
            Range.LAST_AND_CURRENT,
            Unit.MONTH);
        assertRange(now.minusYears(1).with(LocalTime.MIN).with(firstDayOfYear()),
            now.with(LocalTime.MAX).with(lastDayOfYear()),
            Range.LAST_AND_CURRENT,
            Unit.YEAR);

        assertRange(now.minusDays(7).with(LocalTime.MIN).with(previousOrSame(DayOfWeek.MONDAY)),
            now.minusDays(7).with(LocalTime.MAX).with(nextOrSame(DayOfWeek.SUNDAY)),
            Range.LAST,
            Unit.WEEK);
        assertRange(now.minusMonths(1).with(LocalTime.MIN).with(firstDayOfMonth()),
            now.minusMonths(1).with(LocalTime.MAX).with(lastDayOfMonth()),
            Range.LAST,
            Unit.MONTH);
        assertRange(now.minusYears(1).with(LocalTime.MIN).with(firstDayOfYear()),
            now.minusYears(1).with(LocalTime.MAX).with(lastDayOfYear()),
            Range.LAST,
            Unit.YEAR);

        assertRange(oneYearBack.with(LocalTime.MIN),
            now.with(LocalTime.MAX),
            Range.ALL_DATA,
            null);
    }

    private void assertRange(ZonedDateTime expectedStart, ZonedDateTime expectedEnd,
                                    Range range, Unit unit) {
        ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(range, unit);
        assertEquals("begin of " + range + " " + unit + " wrong",
            expectedStart, beginAndEnd[0]);
        assertEquals("end of " + range + " " + unit + " wrong",
            expectedEnd, beginAndEnd[1]);
    }

    private static Event eventAt(ZonedDateTime offsetDateTime) {
        Event e = new Event();
        e.setDateTime(offsetDateTime.toOffsetDateTime());
        return e;
    }
}
