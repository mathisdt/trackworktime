package org.zephyrsoft.trackworktime.weektimes;

import android.content.Context;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.zephyrsoft.trackworktime.model.Week;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class WeekAdapter extends RecyclerView.Adapter<WeekTimesViewHolder> {

	private final WeekIndexConverter weekIndexConverter;
	private final WeekStateLoaderFactory weekStateLoaderFactory;
	private final LayoutParams LAYOUT_PARAMS = new LayoutParams(MATCH_PARENT, MATCH_PARENT);

	public WeekAdapter(@NonNull WeekIndexConverter weekIndexConverter,
			@NonNull WeekStateLoaderFactory weekStateLoaderFactory) {
		this.weekIndexConverter = weekIndexConverter;
		this.weekStateLoaderFactory = weekStateLoaderFactory;
	}

	@NonNull @Override
	public WeekTimesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context context = parent.getContext();
		WeekTimesView weekTimesView = createView(context);
		return new WeekTimesViewHolder(weekTimesView, weekStateLoaderFactory);
	}

	private WeekTimesView createView(Context context) {
		WeekTimesView weekTimesView = new WeekTimesView(context);
		weekTimesView.setLayoutParams(LAYOUT_PARAMS);
		return weekTimesView;
	}

	@Override public void onBindViewHolder(@NonNull WeekTimesViewHolder holder, int position) {
		Week week = weekIndexConverter.getWeekForIndex(position);
		holder.bind(week);
	}

	@Override public void onViewRecycled(@NonNull WeekTimesViewHolder holder) {
		super.onViewRecycled(holder);
		holder.recycle();
	}

	@Override public int getItemCount() {
		return Integer.MAX_VALUE;
	}

}
