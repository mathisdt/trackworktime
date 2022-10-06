package org.zephyrsoft.trackworktime.eventlist;

import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.EventSeparator;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

public class EventItemMapper {

	private final Locale locale;
	private final Function<Event, String> eventTaskName;

	public EventItemMapper(
			@NonNull Locale locale,
			@NonNull Function<Event, String> eventTaskName,
			@NonNull Predicate<Event> isEventSelected
	) {
		this.locale = locale;
		this.eventTaskName = eventTaskName;
	}

	public BaseEventItem map(Event event) {
		return event instanceof EventSeparator
				? new EventSeparatorItem((EventSeparator) event)
				: new EventItem(event, locale, eventTaskName.apply(event));
	}

}
