package org.zephyrsoft.trackworktime.eventlist;

import androidx.annotation.NonNull;

public abstract class BaseEventItem {

	public abstract int getId();
	public abstract boolean isSameContentAs(@NonNull BaseEventItem other);

	public boolean isSameIdAs(@NonNull BaseEventItem other) {
		return getId() == other.getId();
	}
}
