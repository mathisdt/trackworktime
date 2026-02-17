package org.zephyrsoft.trackworktime.eventlist;

import android.content.Context;

import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class EventItemMapper {

	private final Locale locale;
	private final Function<Event, String> eventTaskName;
    private final Context context;

    public EventItemMapper(
		@NonNull Locale locale,
		@NonNull Function<Event, String> eventTaskName,
		@NonNull Context context
	) {
		this.locale = locale;
		this.eventTaskName = eventTaskName;
        this.context = context;
    }

	public List<BaseEventItem> map(List<Event> events) {
		List<BaseEventItem> items = new ArrayList<>(events.size());
		Event prev = null;
		for (Event event : events) {
			if (prev == null || !isOnSameDay(prev, event)) {
				items.add(newEventSeparatorItem(event));
			}
			items.add(newEventItem(event));
			prev = event;
		}
		return items;
	}

	private static boolean isOnSameDay(Event e1, Event e2) {
		return e1.getDateTime().toLocalDate().isEqual(e2.getDateTime().toLocalDate());
	}

	private EventItem newEventItem(Event event) {
		return new EventItem(event, locale, eventTaskName.apply(event), context);
	}

	private EventSeparatorItem newEventSeparatorItem(Event event) {
		String caption = DateTimeUtil.formatLocalizedDayAndDate(event.getDateTime(), locale);
		return new EventSeparatorItem(caption);
	}
}
