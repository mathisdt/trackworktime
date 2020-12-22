package org.zephyrsoft.trackworktime.weektimes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import org.zephyrsoft.trackworktime.model.WeekState;

import java.util.Objects;

public class WeekTimesViewHolder extends RecyclerView.ViewHolder implements Observer<WeekState> {

	private final WeekTimesView weekTimesView;
	private @Nullable LiveData<WeekState> weekStateLiveData;

	public WeekTimesViewHolder(@NonNull WeekTimesView weekTimesView) {
		super(weekTimesView);
		this.weekTimesView = weekTimesView;
	}

	public void bind(@NonNull LiveData<WeekState> weekStateLiveData) {
		this.weekStateLiveData = weekStateLiveData;
		weekStateLiveData.observeForever(this);
	}

	@Override
	public void onChanged(WeekState weekState) {
		Objects.requireNonNull(weekState);
		weekTimesView.setWeekState(weekState);
	}

	public void recycle() {
		removeObserver();
		weekTimesView.clearWeekState();
	}

	private void removeObserver() {
		if(weekStateLiveData != null) {
			weekStateLiveData.removeObserver(this);
			weekStateLiveData = null;
		}
	}

}
