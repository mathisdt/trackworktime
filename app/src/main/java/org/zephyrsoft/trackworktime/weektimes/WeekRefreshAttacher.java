package org.zephyrsoft.trackworktime.weektimes;

import androidx.annotation.NonNull;

public interface WeekRefreshAttacher {
	void addObserver(@NonNull WeekRefreshHandler weekRefreshHandler);
	void removeObserver(@NonNull WeekRefreshHandler weekRefreshHandler);
}
