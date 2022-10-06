package org.zephyrsoft.trackworktime.eventlist;

import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;

public class EventItem extends BaseEventItem {

	private final int id;
	private final String type;
	private final String time;
	private final String task;

	@NonNull
	public String getType() {
		return type;
	}

	@NonNull
	public String getTask() {
		return task;
	}

	@NonNull
	public String getTime() {
		return time;
	}

	@Override
	public int getId() {
		return this.id;
	}

	public EventItem(@NonNull Event event, @NonNull Locale locale, @NonNull String task) {
		super(event);
		this.id = Optional.ofNullable(event.getId()).orElse(System.identityHashCode(event));
		this.type = formatType(event.getTypeEnum());
		this.time = formatTime(event.getTime(), locale);
		this.task = task;
	}

	private String formatTime(OffsetDateTime time, Locale locale) {
		return DateTimeUtil.formatLocalizedTime(time, locale);
	}

	private String formatType(TypeEnum type) {
		switch (type) {
			case CLOCK_IN:
				return "IN";
			case CLOCK_OUT:
				return "OUT";
			default:
				throw new IllegalStateException("unrecognized event type");
		}
	}
}
