package org.zephyrsoft.trackworktime.timer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Unit;
import org.zephyrsoft.trackworktime.report.TaskAndHint;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TimeCalculatorTest {

    private final ZonedDateTime now = ZonedDateTime.now();

    private final Task task1 = new Task(0, "development", 1, 1, 0);
    private final Task task2 = new Task(1, "review", 1, 2, 0);

    private final String hint1 = "ticket-123";
    private final String hint2 = "ticket-456";

    private List<Event> events = new ArrayList<>();

    private TimeCalculator timeCalculator;

    @Before
    public void setup() {
        DAO dao = mock(DAO.class);
        TimerManager timerManager = mock(TimerManager.class);

        doReturn(ZoneId.systemDefault()).when(timerManager).getHomeTimeZone();

        doReturn(List.of(
            eventAt(now.minusYears(1)),
            eventAt(now.minusMonths(1)),
            eventAt(now.minusWeeks(1)),
            eventAt(now.minusDays(1)),
            eventAt(now)
        )).when(dao).getAllEvents();

        doReturn(task1).when(dao).getTask(task1.getId());
        doReturn(task2).when(dao).getTask(task2.getId());

        // Event list for 10 days
        for(int i = 10; i > 5; i--) {
            events.add(eventAt(now.minusDays(i).plusHours(1), task1, i % 2 == 0 ? hint1 : hint2));
            events.add(eventAt(now.minusDays(i).plusHours(2), task1, i % 2 == 0 ? hint1 : ""));
            events.add(eventAt(now.minusDays(i).plusHours(3), task1, i % 2 == 0 ? hint2 : hint1));
            events.add(eventAt(now.minusDays(i).plusHours(4), task1, i % 2 == 0 ? "" : hint2));
            events.add(clockOutEvent(now.minusDays(i).plusHours(5)));
        }
        for(int i = 5; i > 0; i--) {
            events.add(eventAt(now.minusDays(i).plusHours(1), task2, i % 2 == 0 ? hint1 : hint2));
            events.add(eventAt(now.minusDays(i).plusHours(2), task2, i % 2 == 0 ? hint1 : ""));
            events.add(eventAt(now.minusDays(i).plusHours(3), task2, i % 2 == 0 ? hint2 : hint1));
            events.add(eventAt(now.minusDays(i).plusHours(4), task2, i % 2 == 0 ? "" : hint2));
            events.add(clockOutEvent(now.minusDays(i).plusHours(5)));
        }

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

        assertRange(now.minusYears(1).with(LocalTime.MIN),
            now.with(LocalTime.MAX),
            Range.ALL_DATA,
            null);
    }

    @Test
    public void calculateSumsPerTaskAndHint() {
        assertSumsPerTaskAndHintMap(Map.of(),
                now.minusDays(10), now, null);
        assertSumsPerTaskAndHintMap(Map.of(),
                now.minusDays(10), now, List.of());

        assertSumsPerTaskAndHintMap(Map.of(
                        taskAndHint(task2, hint2), timeSum(2),
                        taskAndHint(task2, ""), timeSum(1),
                        taskAndHint(task2, hint1), timeSum(1)),
                now.minusDays(1), now, events);
        assertSumsPerTaskAndHintMap(Map.of(
                        taskAndHint(task1, hint1), timeSum(2),
                        taskAndHint(task1, hint2), timeSum(1),
                        taskAndHint(task1, ""), timeSum(1)),
                now.minusDays(10), now.minusDays(9), events);

        assertSumsPerTaskAndHintMap(Map.of(
                        taskAndHint(task2, hint2), timeSum(8),
                        taskAndHint(task2, ""), timeSum(5),
                        taskAndHint(task2, hint1), timeSum(7)),
                now.minusDays(5), now, events);
        assertSumsPerTaskAndHintMap(Map.of(
                        taskAndHint(task1, hint1), timeSum(8),
                        taskAndHint(task1, hint2), timeSum(7),
                        taskAndHint(task1, ""), timeSum(5)),
                now.minusDays(10), now.minusDays(5), events);

        assertSumsPerTaskAndHintMap(Map.of(
                        taskAndHint(task1, hint2), timeSum(3),
                        taskAndHint(task1, ""), timeSum(2),
                        taskAndHint(task1, hint1), timeSum(3),
                        taskAndHint(task2, hint2), timeSum(3),
                        taskAndHint(task2, ""), timeSum(2),
                        taskAndHint(task2, hint1), timeSum(3)),
                now.minusDays(7), now.minusDays(3), events);
        assertSumsPerTaskAndHintMap(Map.of(
                        taskAndHint(task1, hint1), timeSum(8),
                        taskAndHint(task1, hint2), timeSum(7),
                        taskAndHint(task1, ""), timeSum(5),
                        taskAndHint(task2, hint2), timeSum(8),
                        taskAndHint(task2, ""), timeSum(5),
                        taskAndHint(task2, hint1), timeSum(7)),
                now.minusDays(10), now, events);
    }

    private void assertRange(ZonedDateTime expectedStart, ZonedDateTime expectedEnd,
                                    Range range, Unit unit) {
        ZonedDateTime[] beginAndEnd = timeCalculator.calculateBeginAndEnd(range, unit);
        assertEquals("begin of " + range + " " + unit + " wrong",
            expectedStart, beginAndEnd[0]);
        assertEquals("end of " + range + " " + unit + " wrong",
            expectedEnd, beginAndEnd[1]);
    }

    private void assertSumsPerTaskAndHintMap(Map<TaskAndHint, TimeSum> expected, ZonedDateTime begin, ZonedDateTime end, List<Event> events) {
        Map<TaskAndHint, TimeSum> actual = timeCalculator.calculateSumsPerTaskAndHint(
                begin.toOffsetDateTime(),
                begin.toOffsetDateTime(),
                eventsInRange(events, begin, end));
        assertEquals(expected.size(), expected.size());
        for(Map.Entry<TaskAndHint, TimeSum> entry : expected.entrySet()) {
            assertTrue(expected.keySet().contains(entry.getKey()));
            assertEquals(actual.get(entry.getKey()).getAsMinutes(), entry.getValue().getAsMinutes());
        }
    }

    private static Event eventAt(ZonedDateTime offsetDateTime) {
        Event e = new Event();
        e.setDateTime(offsetDateTime.toOffsetDateTime());
        return e;
    }

    private static Event eventAt(ZonedDateTime offsetDateTime, Task task, String hint) {
        Event e = new Event();
        e.setDateTime(offsetDateTime.toOffsetDateTime());
        e.setType(TypeEnum.CLOCK_IN.getValue());
        e.setTask(task.getId());
        e.setText(hint);
        return e;
    }

    private static List<Event> eventsInRange(List<Event> events, ZonedDateTime from, ZonedDateTime to) {
        List<Event> res = new ArrayList<>();
        if (events != null && !events.isEmpty()) {
            for (Event e : events) {
                if (from.toOffsetDateTime().isBefore(e.getTime())
                        && to.toOffsetDateTime().isAfter(e.getTime())) {
                    res.add(e);
                }
            }
        }
        return res;
    }

    private static Event clockOutEvent(ZonedDateTime time) {
        Event e = new Event();
        e.setType(TypeEnum.CLOCK_OUT.getValue());
        e.setDateTime(time.toOffsetDateTime());
        return e;
    }

    private static TaskAndHint taskAndHint(Task task, String hint) {
        return  new TaskAndHint(hint, task);
    }

    private static TimeSum timeSum(int hours) {
        return timeSum(hours, 0);
    }

    private static TimeSum timeSum(int hours, int minutes) {
        TimeSum t = new TimeSum();
        t.set(hours, minutes);
        return t;
    }
}
