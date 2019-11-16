package org.zephyrsoft.trackworktime.weektimes;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekRowState;
import org.zephyrsoft.trackworktime.model.WeekState;
import org.zephyrsoft.trackworktime.timer.TimeCalculator;
import org.zephyrsoft.trackworktime.timer.TimerManager;

/**
 * Controller for work times view
 */
public class WeekFragment extends Fragment implements WeekRefreshHandler {

	private static final String KEY_WEEK = "key_week";

	private TableLayout weekTable = null;

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

	private Week currentlyShownWeek;
	private SharedPreferences preferences;
	private DAO dao = null;
	private TimerManager timerManager = null;
	private TimeCalculator timeCalculator = null;
	private WeekCallback weekCallback;
	private WeekRefreshAttacher weekRefreshAttacher;

	private WeekStateLoader weekStateLoader;
	private WeekStateCalculator weekStateCalculator;

	@Override
	public void onRefresh() {
		refreshView();
	}

	public interface WeekCallback {
		void onWeekTableClick(@NonNull Week week);
	}

	public static WeekFragment newInstance(@NonNull Week week) {
		WeekFragment fragment = new WeekFragment();
		Bundle args = new Bundle();
		args.putParcelable(KEY_WEEK, week);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadArgs();
		Context context = requireContext().getApplicationContext();
		Basics basics = Basics.getOrCreateInstance(context);
		// Create new DAO, since it's not completely thread safe
		dao = new DAO(getContext());
		preferences = basics.getPreferences();
		timerManager = basics.getTimerManager();
		timeCalculator = basics.getTimeCalculator();

		weekStateCalculator = new WeekStateCalculator(getContext(), dao,
				timerManager, timeCalculator, preferences, currentlyShownWeek);
	}

	private void loadArgs() {
		Bundle args = getArguments();
		if(args == null) {
			throw new IllegalStateException("Fragment has no arguments");
		}

		currentlyShownWeek = getArguments().getParcelable(KEY_WEEK);
		if(currentlyShownWeek == null) {
			throw new IllegalArgumentException("Fragment week argument was null");
		}
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		if(!(context instanceof WeekCallback)) {
			throw new RuntimeException("Parent fragment context should implement "
					+ WeekCallback.class.getSimpleName());
		}
		weekCallback = (WeekCallback)context;

		if(!(context instanceof WeekRefreshAttacher)) {
			throw new RuntimeException("Parent fragment context should implement "
					+ WeekRefreshAttacher.class.getSimpleName());
		}
		weekRefreshAttacher = (WeekRefreshAttacher)context;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		weekCallback = null;
		weekRefreshAttacher = null;
	}

	@Override @Nullable
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.week, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		findAllViewsById(view);

		weekTable.setOnClickListener(v -> {
			if(currentlyShownWeek != null && weekCallback != null) {
				weekCallback.onWeekTableClick(currentlyShownWeek);
			}
		});
	}

	private void findAllViewsById(View view) {
		weekTable = view.findViewById(R.id.week_table);
		titleRow = view.findViewById(R.id.titleRow);
		topLeftCorner = view.findViewById(R.id.topLeftCorner);
		inLabel = view.findViewById(R.id.inLabel);
		outLabel = view.findViewById(R.id.outLabel);
		workedLabel = view.findViewById(R.id.workedLabel);
		flexiLabel = view.findViewById(R.id.flexiLabel);
		mondayRow = view.findViewById(R.id.mondayRow);
		mondayLabel = view.findViewById(R.id.mondayLabel);
		mondayIn = view.findViewById(R.id.mondayIn);
		mondayOut = view.findViewById(R.id.mondayOut);
		mondayWorked = view.findViewById(R.id.mondayWorked);
		mondayFlexi = view.findViewById(R.id.mondayFlexi);
		tuesdayRow = view.findViewById(R.id.tuesdayRow);
		tuesdayLabel = view.findViewById(R.id.tuesdayLabel);
		tuesdayIn = view.findViewById(R.id.tuesdayIn);
		tuesdayOut = view.findViewById(R.id.tuesdayOut);
		tuesdayWorked = view.findViewById(R.id.tuesdayWorked);
		tuesdayFlexi = view.findViewById(R.id.tuesdayFlexi);
		wednesdayRow = view.findViewById(R.id.wednesdayRow);
		wednesdayLabel = view.findViewById(R.id.wednesdayLabel);
		wednesdayIn = view.findViewById(R.id.wednesdayIn);
		wednesdayOut = view.findViewById(R.id.wednesdayOut);
		wednesdayWorked = view.findViewById(R.id.wednesdayWorked);
		wednesdayFlexi = view.findViewById(R.id.wednesdayFlexi);
		thursdayRow = view.findViewById(R.id.thursdayRow);
		thursdayLabel = view.findViewById(R.id.thursdayLabel);
		thursdayIn = view.findViewById(R.id.thursdayIn);
		thursdayOut = view.findViewById(R.id.thursdayOut);
		thursdayWorked = view.findViewById(R.id.thursdayWorked);
		thursdayFlexi = view.findViewById(R.id.thursdayFlexi);
		fridayRow = view.findViewById(R.id.fridayRow);
		fridayLabel = view.findViewById(R.id.fridayLabel);
		fridayIn = view.findViewById(R.id.fridayIn);
		fridayOut = view.findViewById(R.id.fridayOut);
		fridayWorked = view.findViewById(R.id.fridayWorked);
		fridayFlexi = view.findViewById(R.id.fridayFlexi);
		saturdayRow = view.findViewById(R.id.saturdayRow);
		saturdayLabel = view.findViewById(R.id.saturdayLabel);
		saturdayIn = view.findViewById(R.id.saturdayIn);
		saturdayOut = view.findViewById(R.id.saturdayOut);
		saturdayWorked = view.findViewById(R.id.saturdayWorked);
		saturdayFlexi = view.findViewById(R.id.saturdayFlexi);
		sundayRow = view.findViewById(R.id.sundayRow);
		sundayLabel = view.findViewById(R.id.sundayLabel);
		sundayIn = view.findViewById(R.id.sundayIn);
		sundayOut = view.findViewById(R.id.sundayOut);
		sundayWorked = view.findViewById(R.id.sundayWorked);
		sundayFlexi = view.findViewById(R.id.sundayFlexi);
		totalRow = view.findViewById(R.id.totalRow);
		totalLabel = view.findViewById(R.id.totalLabel);
		totalIn = view.findViewById(R.id.totalIn);
		totalOut = view.findViewById(R.id.totalOut);
		totalWorked = view.findViewById(R.id.totalWorked);
		totalFlexi = view.findViewById(R.id.totalFlexi);
	}

	public void refreshView() {
		if(currentlyShownWeek == null ||
				(weekStateLoader != null && weekStateLoader.getStatus() != AsyncTask.Status.FINISHED)) {
			return;
		}

		startWeekLoader();
	}

	private void startWeekLoader() {
		weekStateLoader = new WeekStateLoader(weekStateCalculator, this::showWeekData);
		weekStateLoader.execute();
	}

	private void showWeekData(WeekState weekState) {
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

	@Override
	public void onStart() {
		super.onStart();
		refreshView();
		// Note: Attaching fragment on start will keep it "ready" when view pager swipes on to it
		weekRefreshAttacher.addObserver(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		weekRefreshAttacher.removeObserver(this);
		stopWeekLoader();
		dao.close();
	}

	private void stopWeekLoader() {
		if(weekStateLoader != null)
			weekStateLoader.cancel(true);
	}

}
