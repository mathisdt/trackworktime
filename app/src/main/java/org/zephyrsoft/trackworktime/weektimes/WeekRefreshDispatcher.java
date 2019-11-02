package org.zephyrsoft.trackworktime.weektimes;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class WeekRefreshDispatcher implements WeekRefreshAttacher {

	private final List<WeekRefreshHandler> weekRefreshHandlers = new ArrayList<>();

	@Override
	public void addObserver(@NonNull WeekRefreshHandler weekRefreshHandler) {
		weekRefreshHandlers.add(weekRefreshHandler);
	}

	@Override
	public void removeObserver(@NonNull WeekRefreshHandler weekRefreshHandler) {
		weekRefreshHandlers.remove(weekRefreshHandler);
	}

	public void dispatchRefresh() {
		for(WeekRefreshHandler o : weekRefreshHandlers) {
			o.onRefresh();
		}
	}

}
