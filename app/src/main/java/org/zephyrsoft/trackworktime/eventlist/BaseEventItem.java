package org.zephyrsoft.trackworktime.eventlist;

import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.model.Event;

public abstract class BaseEventItem {

	private final Event event;

	@NonNull
	public Event getEvent() {
		return event;
	}

	public abstract int getId();

	protected BaseEventItem(@NonNull Event event) {
		this.event = event;
	}

	public boolean isSameIdAs(@NonNull BaseEventItem other) {
		return getId() == other.getId();
	}

	public boolean isSameContentAs(@NonNull BaseEventItem other) {
		return getEvent().equals(other.getEvent());
	}
}
