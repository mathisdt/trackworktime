package org.zephyrsoft.trackworktime.eventlist;

import androidx.annotation.NonNull;

public class EventSeparatorItem extends BaseEventItem {

	private final int id;
	private final String caption;

	@NonNull
	public String getTitle() {
		return caption;
	}

	@Override
	public int getId() {
		return id;
	}

	public EventSeparatorItem(@NonNull String caption) {
		this.id = caption.hashCode();
		this.caption = caption;
	}

	@Override
	public boolean isSameContentAs(@NonNull BaseEventItem other) {
		if (!(other instanceof EventSeparatorItem)) {
			return false;
		}
		return getId() == other.getId();
	}
}
