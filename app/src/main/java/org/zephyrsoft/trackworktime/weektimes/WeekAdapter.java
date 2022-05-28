/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License 3.0 as published by
 * the Free Software Foundation.
 *
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License 3.0 for more details.
 *
 * You should have received a copy of the GNU General Public License 3.0
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime.weektimes;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.content.Context;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekState;
import org.zephyrsoft.trackworktime.weektimes.WeekTimesView.OnDayClickListener;

public class WeekAdapter extends RecyclerView.Adapter<WeekTimesViewHolder> {
	
	private final WeekStateLoaderManager weekStateLoaderManager;
	private final LayoutParams LAYOUT_PARAMS = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
	private final OnDayClickListener onDayClickListener;
	private final OnClickListener onTopLeftClickListener;
	private final OnClickListener onClickListener;

	public WeekAdapter(@NonNull WeekStateLoaderManager weekStateLoaderManager,
			@Nullable OnDayClickListener onDayClickListener,
			@Nullable OnClickListener onTopLeftClickListener,
			@Nullable OnClickListener onClickListener) {
		this.weekStateLoaderManager = weekStateLoaderManager;
		this.onDayClickListener = onDayClickListener;
		this.onTopLeftClickListener = onTopLeftClickListener;
		this.onClickListener = onClickListener;
		setHasStableIds(true);
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
		weekTimesView.setTopLeftClickListener(onTopLeftClickListener);
		weekTimesView.setOnDayClickListener(onDayClickListener);		
		weekTimesView.setOnClickListener(onClickListener);
		return weekTimesView;
	}

	@Override
	public void onBindViewHolder(@NonNull WeekTimesViewHolder holder, int position) {
		Week week = WeekIndexConverter.getWeekForIndex(position);
		int requestId = position;
		// Cancel request before starting new one. It's possible same week is still being loaded,
		// but holder hasn't been recycled yet.
		weekStateLoaderManager.cancelRequest(requestId);
		LiveData<WeekState> weekState = weekStateLoaderManager.requestWeekState(week, requestId);
		holder.bind(weekState);
	}

	@Override
	public void onViewRecycled(@NonNull WeekTimesViewHolder holder) {
		super.onViewRecycled(holder);
		int position = holder.getAdapterPosition();
		if(position != NO_POSITION) {
			weekStateLoaderManager.cancelRequest(position);
		}
		holder.recycle();
	}

	@Override
	public int getItemCount() {
		return Integer.MAX_VALUE;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}
