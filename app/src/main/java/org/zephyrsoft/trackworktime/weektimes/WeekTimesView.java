package org.zephyrsoft.trackworktime.weektimes;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.model.WeekRowState;
import org.zephyrsoft.trackworktime.model.WeekState;

import java.util.Objects;

public class WeekTimesView extends LinearLayout {

	private WeekState weekState;

	private TableRow titleRow = null;
	private TextView topLeftCorner = null;
	private TextView inLabel = null;
	private TextView outLabel = null;
	private TextView workedLabel = null;
	private TextView flexiLabel = null;

	private TableRow mondayRow = null;
	private TextView mondayLabel = null;
	private TextView mondayIn = null;
	private TextView mondayOut = null;
	private TextView mondayWorked = null;
	private TextView mondayFlexi = null;

	private TableRow tuesdayRow = null;
	private TextView tuesdayLabel = null;
	private TextView tuesdayIn = null;
	private TextView tuesdayOut = null;
	private TextView tuesdayWorked = null;
	private TextView tuesdayFlexi = null;

	private TableRow wednesdayRow = null;
	private TextView wednesdayLabel = null;
	private TextView wednesdayIn = null;
	private TextView wednesdayOut = null;
	private TextView wednesdayWorked = null;
	private TextView wednesdayFlexi = null;

	private TableRow thursdayRow = null;
	private TextView thursdayLabel = null;
	private TextView thursdayIn = null;
	private TextView thursdayOut = null;
	private TextView thursdayWorked = null;
	private TextView thursdayFlexi = null;

	private TableRow fridayRow = null;
	private TextView fridayLabel = null;
	private TextView fridayIn = null;
	private TextView fridayOut = null;
	private TextView fridayWorked = null;
	private TextView fridayFlexi = null;

	private TableRow saturdayRow = null;
	private TextView saturdayLabel = null;
	private TextView saturdayIn = null;
	private TextView saturdayOut = null;
	private TextView saturdayWorked = null;
	private TextView saturdayFlexi = null;

	private TableRow sundayRow = null;
	private TextView sundayLabel = null;
	private TextView sundayIn = null;
	private TextView sundayOut = null;
	private TextView sundayWorked = null;
	private TextView sundayFlexi = null;

	private TableRow totalRow = null;
	private TextView totalLabel = null;
	private TextView totalIn = null;
	private TextView totalOut = null;
	private TextView totalWorked = null;
	private TextView totalFlexi = null;

	public WeekTimesView(@NonNull Context context) {
		super(context);
		startLayoutLoading();
	}

	private void startLayoutLoading() {
		AsyncLayoutInflater asyncInflater = new AsyncLayoutInflater(getContext());
		asyncInflater.inflate(R.layout.week_table, this, (view, resId, parent) -> {
			Objects.requireNonNull(parent); // Non-null parent passed in #asyncInflater
			parent.addView(view);
			onViewReady();
		});
	}

	private void onViewReady() {
		findAllViewsById();
		if(isDataSet()) {
			loadWeekState();
		}
	}

	private boolean isDataSet() {
		return weekState != null;
	}

	private void findAllViewsById() {
		titleRow = findViewById(R.id.titleRow);
		topLeftCorner = findViewById(R.id.topLeftCorner);
		inLabel = findViewById(R.id.inLabel);
		outLabel = findViewById(R.id.outLabel);
		workedLabel = findViewById(R.id.workedLabel);
		flexiLabel = findViewById(R.id.flexiLabel);
		mondayRow = findViewById(R.id.mondayRow);
		mondayLabel = findViewById(R.id.mondayLabel);
		mondayIn = findViewById(R.id.mondayIn);
		mondayOut = findViewById(R.id.mondayOut);
		mondayWorked = findViewById(R.id.mondayWorked);
		mondayFlexi = findViewById(R.id.mondayFlexi);
		tuesdayRow = findViewById(R.id.tuesdayRow);
		tuesdayLabel = findViewById(R.id.tuesdayLabel);
		tuesdayIn = findViewById(R.id.tuesdayIn);
		tuesdayOut = findViewById(R.id.tuesdayOut);
		tuesdayWorked = findViewById(R.id.tuesdayWorked);
		tuesdayFlexi = findViewById(R.id.tuesdayFlexi);
		wednesdayRow = findViewById(R.id.wednesdayRow);
		wednesdayLabel = findViewById(R.id.wednesdayLabel);
		wednesdayIn = findViewById(R.id.wednesdayIn);
		wednesdayOut = findViewById(R.id.wednesdayOut);
		wednesdayWorked = findViewById(R.id.wednesdayWorked);
		wednesdayFlexi = findViewById(R.id.wednesdayFlexi);
		thursdayRow = findViewById(R.id.thursdayRow);
		thursdayLabel = findViewById(R.id.thursdayLabel);
		thursdayIn = findViewById(R.id.thursdayIn);
		thursdayOut = findViewById(R.id.thursdayOut);
		thursdayWorked = findViewById(R.id.thursdayWorked);
		thursdayFlexi = findViewById(R.id.thursdayFlexi);
		fridayRow = findViewById(R.id.fridayRow);
		fridayLabel = findViewById(R.id.fridayLabel);
		fridayIn = findViewById(R.id.fridayIn);
		fridayOut = findViewById(R.id.fridayOut);
		fridayWorked = findViewById(R.id.fridayWorked);
		fridayFlexi = findViewById(R.id.fridayFlexi);
		saturdayRow = findViewById(R.id.saturdayRow);
		saturdayLabel = findViewById(R.id.saturdayLabel);
		saturdayIn = findViewById(R.id.saturdayIn);
		saturdayOut = findViewById(R.id.saturdayOut);
		saturdayWorked = findViewById(R.id.saturdayWorked);
		saturdayFlexi = findViewById(R.id.saturdayFlexi);
		sundayRow = findViewById(R.id.sundayRow);
		sundayLabel = findViewById(R.id.sundayLabel);
		sundayIn = findViewById(R.id.sundayIn);
		sundayOut = findViewById(R.id.sundayOut);
		sundayWorked = findViewById(R.id.sundayWorked);
		sundayFlexi = findViewById(R.id.sundayFlexi);
		totalRow = findViewById(R.id.totalRow);
		totalLabel = findViewById(R.id.totalLabel);
		totalIn = findViewById(R.id.totalIn);
		totalOut = findViewById(R.id.totalOut);
		totalWorked = findViewById(R.id.totalWorked);
		totalFlexi = findViewById(R.id.totalFlexi);
	}

	public void setWeekState(@NonNull WeekState weekState) {
		this.weekState = weekState;
		if(isViewReady()) {
			loadWeekState();
		}
	}

	private boolean isViewReady() {
		return inLabel != null;
	}

	private void loadWeekState() {
		if(!isDataSet()) {
			Logger.warn("Loading weekState when data was not set");
			return;
		}

		// TODO: Generalize...

		showWeekRow(weekState.header, topLeftCorner, inLabel, outLabel, workedLabel,
				flexiLabel);

		showWeekRow(weekState.monday, mondayLabel, mondayIn, mondayOut, mondayWorked,
				mondayFlexi);
		refreshRowHighlighting(weekState.monday, mondayRow, true);

		showWeekRow(weekState.tuesday, tuesdayLabel, tuesdayIn, tuesdayOut, tuesdayWorked,
				tuesdayFlexi);
		refreshRowHighlighting(weekState.tuesday, tuesdayRow, false);

		showWeekRow(weekState.wednesday, wednesdayLabel, wednesdayIn, wednesdayOut, wednesdayWorked,
				wednesdayFlexi);
		refreshRowHighlighting(weekState.wednesday, wednesdayRow, true);

		showWeekRow(weekState.thursday, thursdayLabel, thursdayIn, thursdayOut, thursdayWorked,
				thursdayFlexi);
		refreshRowHighlighting(weekState.thursday, thursdayRow, false);

		showWeekRow(weekState.friday, fridayLabel, fridayIn, fridayOut, fridayWorked,
				fridayFlexi);
		refreshRowHighlighting(weekState.friday, fridayRow, true);

		showWeekRow(weekState.saturday, saturdayLabel, saturdayIn, saturdayOut, saturdayWorked,
				saturdayFlexi);
		refreshRowHighlighting(weekState.saturday, saturdayRow, false);

		showWeekRow(weekState.sunday, sundayLabel, sundayIn, sundayOut, sundayWorked,
				sundayFlexi);
		refreshRowHighlighting(weekState.sunday, sundayRow, true);

		showWeekRow(weekState.totals, totalLabel, totalIn, totalOut, totalWorked,
				totalFlexi);
	}

	private void showWeekRow(WeekRowState weekRowState, TextView label, TextView in, TextView out,
			TextView worked, TextView flexi) {
		label.setText(weekRowState.date);
		in.setText(weekRowState.in);
		out.setText(weekRowState.out);
		worked.setText(weekRowState.worked);
		flexi.setText(weekRowState.flexi);
	}

	private void refreshRowHighlighting(WeekRowState weekRowState, TableRow tableRow,
			boolean isLight) {
		int notHighlightedDrawable = isLight ? R.drawable.table_row : 0;
		tableRow.setBackgroundResource(weekRowState.isHiglighted
				? R.drawable.table_row_highlighting
				: notHighlightedDrawable);
	}

}