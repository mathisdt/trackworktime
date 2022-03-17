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

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

import org.pmw.tinylog.Logger;
import org.threeten.bp.DayOfWeek;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.databinding.WeekTableBinding;
import org.zephyrsoft.trackworktime.model.WeekState;
import org.zephyrsoft.trackworktime.model.WeekState.DayRowState;
import org.zephyrsoft.trackworktime.model.WeekState.SummaryRowState;
import org.zephyrsoft.trackworktime.options.Key;

public class WeekTimesView extends LinearLayout {
	
	private WeekState weekState;
	
	private TableLayout weekTable = null;
	private WeekTableBinding binding;
	private SharedPreferences preferences;

	public WeekTimesView(@NonNull Context context) {
		super(context);
		preferences = Basics.getOrCreateInstance(context).getPreferences();
		startLayoutLoading();
	}

	private void startLayoutLoading() {
		new AsyncLayoutInflater(getContext()).inflate(R.layout.week_table,this, ((view, resid, parent) -> {
			binding = WeekTableBinding.bind(view);
			parent.addView(binding.getRoot());
			onViewReady();
		}));
	}

	private void onViewReady() {
		weekTable = binding.weekTable;
		
		if(isDataSet()) {
			loadWeekState();
		}
	}

	/** Interface for callback when clicking on day **/
	public interface OnDayClickListener {
		void onClick(View v, DayOfWeek day);
	}
	private OnDayClickListener onDayClickListener;
	public void setOnDayClickListener(OnDayClickListener onDayClickListener) {
		this.onDayClickListener = onDayClickListener;
	}
	

	private boolean isDataSet() {
		return weekState != null;
	}

	public void clearWeekState() {
		setWeekState(new WeekState());
	}

	public void setWeekState(@NonNull WeekState weekState) {
		this.weekState = weekState;
		if(isViewReady()) {
			loadWeekState();
		}
	}

	private boolean isViewReady() {
		return weekTable != null;
	}

	private void loadWeekState() {
		if(!isDataSet()) {
			Logger.warn("Loading weekState when data was not set");
			return;
		}
		
		binding.topLeftCorner.setText(weekState.topLeftCorner);

		for (DayOfWeek day : DayOfWeek.values()) {
			DayRowState currentWeekRow = weekState.getRowForDay(day);

			TableRow tableRow = (TableRow) weekTable.getChildAt(day.getValue());
			setWeekRow(currentWeekRow, tableRow, day);
			setRowHighlighting(currentWeekRow, tableRow, day.getValue() % 2 == 1);
		}
		
		setSummaryRow(weekState.totals, (TableRow) weekTable.getChildAt(8));
	}
	
	private void setWeekRow(DayRowState dayRowState, TableRow tableRow, DayOfWeek day) {
		TextView textView;
		
		textView = getTableCell(tableRow, 0);
		textView.setText(dayRowState.label);
		setColorAccording(dayRowState.labelHighlighted, textView);
		textView.setOnClickListener(v -> {
			if (onDayClickListener != null) {
				onDayClickListener.onClick(v, day);
			}
		});
		
		getTableCell(tableRow, 1).setText(dayRowState.in);
		getTableCell(tableRow, 2).setText(dayRowState.out);
		
		textView = getCombinedTableCell1(tableRow, 3);
		textView.setText(dayRowState.worked);

		getCombinedTableCell1(tableRow, 4).setText(dayRowState.flexi);

		TextView cell23 = getCombinedTableCell2(tableRow, 3);
		TextView cell24 = getCombinedTableCell2(tableRow, 4);
		if (preferences.getBoolean(Key.DECIMAL_TIME_SUMS.getName(), false)) {
			cell23.setVisibility(VISIBLE);
			cell24.setVisibility(VISIBLE);
			cell23.setText(dayRowState.workedDecimal);
			cell24.setText(dayRowState.flexiDecimal);
		} else {
			cell23.setVisibility(GONE);
			cell24.setVisibility(GONE);
		}
	}

	private void setColorAccording(WeekState.HighlightType type, TextView textView) {
		switch (type) {
			case NONE:
				textView.setTextColor(getResources().getColor(R.color.date_regular_work));
				break;
			case REGULAR_FREE:
				textView.setTextColor(getResources().getColor(R.color.date_regular_free));
				break;
			case FREE:
				textView.setTextColor(getResources().getColor(R.color.date_free));
				break;
			case CHANGED_TARGET_TIME:
				textView.setTextColor(getResources().getColor(R.color.date_target_changed));
				break;
			default:
				throw new IllegalStateException("unknown highlight type " + type);
		}
	}
	
	private void setSummaryRow(SummaryRowState summaryRowState, TableRow tableRow) {
		getTableCell(tableRow, 0).setText(summaryRowState.label);
		getCombinedTableCell1(tableRow, 1).setText(summaryRowState.worked);
		getCombinedTableCell1(tableRow, 2).setText(summaryRowState.flexi);

		TextView cell21 = getCombinedTableCell2(tableRow, 1);
		TextView cell22 = getCombinedTableCell2(tableRow, 2);
		if (preferences.getBoolean(Key.DECIMAL_TIME_SUMS.getName(), false)) {
			cell21.setVisibility(VISIBLE);
			cell22.setVisibility(VISIBLE);
			cell21.setText(summaryRowState.workedDecimal);
			cell22.setText(summaryRowState.flexiDecimal);
		} else {
			cell21.setVisibility(GONE);
			cell22.setVisibility(GONE);
		}
	}

	private void setRowHighlighting(DayRowState dayRowState, TableRow tableRow,  boolean isLight) {
		int unhighlightedDrawable = isLight ? R.drawable.table_row : 0;
		
		tableRow.setBackgroundResource(dayRowState.highlighted
				? R.drawable.table_row_highlighting : unhighlightedDrawable);
	}	
	
	private TextView getCombinedTableCell1(TableRow tableRow, int index) {
		return (TextView) ((LinearLayout) tableRow.getChildAt(index)).getChildAt(0);
	}
	private TextView getCombinedTableCell2(TableRow tableRow, int index) {
		return (TextView) ((LinearLayout) tableRow.getChildAt(index)).getChildAt(1);
	}

	private TextView getTableCell(TableRow tableRow, int index) {
		return (TextView) tableRow.getChildAt(index);
	}
}
