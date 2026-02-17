package org.zephyrsoft.trackworktime.eventlist;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class EventItem extends BaseEventItem {

	private final int id;
	private final Event event;
	private final String type;
	private final String time;
	private final String task;

	@NonNull
	public Event getEvent() {
		return event;
	}

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

	@Override
	public boolean isSameContentAs(@NonNull BaseEventItem other) {
		if (!(other instanceof EventItem)) {
			return false;
		}
		Event otherEvent = ((EventItem) other).getEvent();
		return getEvent().equals(otherEvent);
	}

	public EventItem(@NonNull Event event, @NonNull Locale locale, @NonNull String task, Context context) {
		this.event = event;
		this.id = Optional.ofNullable(event.getId()).orElse(System.identityHashCode(event));
		this.type = translateType(event.getTypeEnum(), context);
		this.time = formatTime(event.getTime(), locale);
		this.task = task;
	}

	private String translateType(TypeEnum eventType, Context context) {
		return switch (event.getTypeEnum()) {
			case CLOCK_IN -> {
				try {
					yield context.getString(R.string.in);
				} catch (Resources.NotFoundException nfe) {
					// the current language doesn't have a translated name for "IN"
					yield "IN";
				}
			}
			case CLOCK_OUT -> {
				try {
					yield context.getString(R.string.out);
				} catch (Resources.NotFoundException nfe) {
					// the current language doesn't have a translated name for "OUT"
					yield "OUT";
				}
			}
			case CLOCK_OUT_NOW -> {
				try {
					yield context.getString(R.string.out);
				} catch (Resources.NotFoundException nfe) {
					// the current language doesn't have a translated name for "OUT"
					yield "OUT";
				}
			}
		};
	}

	private String formatTime(OffsetDateTime time, Locale locale) {
		return DateTimeUtil.formatLocalizedTime(time, locale);
	}
}
