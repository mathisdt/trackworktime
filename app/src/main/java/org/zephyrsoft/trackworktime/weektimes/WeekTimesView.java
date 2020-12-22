package org.zephyrsoft.trackworktime.weektimes;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

import org.pmw.tinylog.Logger;
import org.threeten.bp.DayOfWeek;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.databinding.WeekTableBinding;
import org.zephyrsoft.trackworktime.model.WeekState;
import org.zephyrsoft.trackworktime.model.WeekState.DayRowState;
import org.zephyrsoft.trackworktime.model.WeekState.SummaryRowState;

public class WeekTimesView extends LinearLayout {
	
	private WeekState weekState;
	
	private TableLayout weekTable = null;
	private WeekTableBinding binding;

	public WeekTimesView(@NonNull Context context) {
		super(context);
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
		
		binding.targetLabel.setOnClickListener(v -> {
			if (onDayClickListener != null) {
				onDayClickListener.onClick(v, null);
			}
		});
	}
	
	private void setWeekRow(DayRowState dayRowState, TableRow tableRow, DayOfWeek day) {
		TextView textView;
		
		textView = getTableCell(tableRow, 0);
		textView.setText(dayRowState.label);
		textView.setTextColor(dayRowState.labelHighlighted ? 
				Color.GREEN : getResources().getColor(R.color.light_gray));
		textView.setOnClickListener(v -> {
			if (onDayClickListener != null) {
				onDayClickListener.onClick(v, day);
			}
		});
		
		getTableCell(tableRow, 1).setText(dayRowState.in);
		getTableCell(tableRow, 2).setText(dayRowState.out);
		
		textView = getTableCell(tableRow, 3);
		textView.setText(dayRowState.worked);
		textView.setTextColor(dayRowState.workedHighlighted ?
				Color.GREEN : getResources().getColor(R.color.light_gray));
		
		getTableCell(tableRow, 4).setText(dayRowState.flexi);
	}
	
	private void setSummaryRow(SummaryRowState summaryRowState, TableRow tableRow) {
		getTableCell(tableRow, 0).setText(summaryRowState.label);
		getTableCell(tableRow, 1).setText(summaryRowState.worked);
		getTableCell(tableRow, 2).setText(summaryRowState.flexi);
	}

	private void setRowHighlighting(DayRowState dayRowState, TableRow tableRow,  boolean isLight) {
		int unhighlightedDrawable = isLight ? R.drawable.table_row : 0;
		
		tableRow.setBackgroundResource(dayRowState.highlighted
				? R.drawable.table_row_highlighting : unhighlightedDrawable);
	}	
	
	private TextView getTableCell(TableRow tableRow, int index) {
		return (TextView) tableRow.getChildAt(index);
	}
}
