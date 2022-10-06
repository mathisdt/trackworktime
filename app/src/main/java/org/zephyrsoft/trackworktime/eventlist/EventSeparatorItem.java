package org.zephyrsoft.trackworktime.eventlist;

import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.model.EventSeparator;

public class EventSeparatorItem extends BaseEventItem {

	private final int id;

	@NonNull
	public String getTitle() {
		return getEvent().toString();
	}

	@Override
	public int getId() {
		return id;
	}

	public EventSeparatorItem(EventSeparator event) {
		super(event);
		this.id = event.hashCode();
	}

	@NonNull
	@Override
	public EventSeparator getEvent() {
		return (EventSeparator) super.getEvent();
	}
}
