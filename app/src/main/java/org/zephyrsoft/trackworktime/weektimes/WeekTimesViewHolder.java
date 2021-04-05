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
