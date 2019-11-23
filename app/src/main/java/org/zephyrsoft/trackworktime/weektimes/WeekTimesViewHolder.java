package org.zephyrsoft.trackworktime.weektimes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.zephyrsoft.trackworktime.model.Week;

public class WeekTimesViewHolder extends RecyclerView.ViewHolder {

	private final WeekTimesView weekTimesView;
	private final WeekStateLoaderFactory weekStateLoaderFactory;
	private @Nullable WeekStateLoader weekStateLoader;

	public WeekTimesViewHolder(@NonNull WeekTimesView weekTimesView,
			@NonNull WeekStateLoaderFactory weekStateLoaderFactory) {
		super(weekTimesView);
		this.weekTimesView = weekTimesView;
		this.weekStateLoaderFactory = weekStateLoaderFactory;
	}

	public void bind(@NonNull Week week) {
		startWeekStateLoading(week);
	}

	private void startWeekStateLoading(@NonNull Week week) {
		weekStateLoader = weekStateLoaderFactory.create(week, weekTimesView::setWeekState);
		weekStateLoader.execute();
	}

	public void recycle() {
		stopWeekStateLoading();
	}

	private void stopWeekStateLoading() {
		if(weekStateLoader == null) {
			return;
		}
		weekStateLoader.cancel(true);
		weekStateLoader = null;
	}

}
