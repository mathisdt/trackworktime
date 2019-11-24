package org.zephyrsoft.trackworktime.weektimes;

import android.content.Context;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekState;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

public class WeekAdapter extends RecyclerView.Adapter<WeekTimesViewHolder> {

	private final WeekIndexConverter weekIndexConverter;
	private final WeekStateLoaderManager weekStateLoaderManager;
	private final LayoutParams LAYOUT_PARAMS = new LayoutParams(MATCH_PARENT, MATCH_PARENT);

	public WeekAdapter(@NonNull WeekIndexConverter weekIndexConverter,
			@NonNull WeekStateLoaderManager weekStateLoaderManager) {
		this.weekIndexConverter = weekIndexConverter;
		this.weekStateLoaderManager = weekStateLoaderManager;
	}

	@NonNull @Override
	public WeekTimesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context context = parent.getContext();
		WeekTimesView weekTimesView = createView(context);
		return new WeekTimesViewHolder(weekTimesView);
	}

	private WeekTimesView createView(Context context) {
		WeekTimesView weekTimesView = new WeekTimesView(context);
		weekTimesView.setLayoutParams(LAYOUT_PARAMS);
		return weekTimesView;
	}

	@Override public void onBindViewHolder(@NonNull WeekTimesViewHolder holder, int position) {
		Week week = weekIndexConverter.getWeekForIndex(position);
		int requestId = position;
		LiveData<WeekState> weekState = weekStateLoaderManager.requestWeekState(week, requestId);
		holder.bind(weekState);
	}

	@Override public void onViewRecycled(@NonNull WeekTimesViewHolder holder) {
		super.onViewRecycled(holder);
		int position = holder.getAdapterPosition();
		if(position != NO_POSITION) {
			weekStateLoaderManager.cancelRequest(position);
		}
		holder.recycle();
	}

	@Override public int getItemCount() {
		return Integer.MAX_VALUE;
	}

}
