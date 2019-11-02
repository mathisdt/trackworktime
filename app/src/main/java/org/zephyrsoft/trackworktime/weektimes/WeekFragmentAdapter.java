package org.zephyrsoft.trackworktime.weektimes;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.util.WeekFragment;

public class WeekFragmentAdapter extends FragmentStateAdapter {

	private final WeekIndexConverter weekIndexConverter;

	public WeekFragmentAdapter(@NonNull FragmentManager fragmentManager,
			@NonNull Lifecycle lifecycle, @NonNull WeekIndexConverter weekIndexConverter) {
		super(fragmentManager, lifecycle);
		this.weekIndexConverter = weekIndexConverter;
	}

	@Override @NonNull
	public Fragment createFragment(int position) {
		Week week = weekIndexConverter.getWeekForIndex(position);
		return WeekFragment.newInstance(week);
	}

	@Override
	public int getItemCount() {
		// Should be fine until next apocalypse
		return Integer.MAX_VALUE;
	}

}
