package org.zephyrsoft.trackworktime.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.DayLine;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekDayEnum;
import org.zephyrsoft.trackworktime.model.WeekPlaceholder;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.timer.TimeCalculator;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.weektimes.WeekRefreshAttacher;
import org.zephyrsoft.trackworktime.weektimes.WeekRefreshHandler;

import java.util.List;

import hirondelle.date4j.DateTime;

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

	private Button previousWeekButton = null;
	private Button nextWeekButton = null;
	private Button todayButton = null;

	private Week currentlyShownWeek;
	private SharedPreferences preferences;
	private DAO dao = null;
	private TimerManager timerManager = null;
	private TimeCalculator timeCalculator = null;
	private WeekCallback weekCallback;
	private WeekRefreshAttacher weekRefreshAttacher;

	@Override
	public void onRefresh() {
		refreshView();
	}

	public interface WeekCallback {
		void onWeekTableClick();
	}

	public @Nullable Week getWeek() {
		return currentlyShownWeek;
	}

	public void setWeek(@NonNull Week week) {
		currentlyShownWeek = week;
		refreshView();
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
		dao = basics.getDao();
		preferences = basics.getPreferences();
		timerManager = basics.getTimerManager();
		timeCalculator = basics.getTimeCalculator();
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
		if(!(context instanceof WeekCallback))
			throw new RuntimeException("Parent fragment context should implement "
					+ WeekCallback.class.getSimpleName());
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
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		findAllViewsById(view);

		previousWeekButton.setOnClickListener(v -> changeDisplayedWeek(-1));

		nextWeekButton.setOnClickListener(v -> changeDisplayedWeek(1));

		todayButton.setOnClickListener(v -> {
			final String todaysWeekStart = DateTimeUtil.getWeekStartAsString(DateTimeUtil.getCurrentDateTime());
			Week todaysWeek = dao.getWeek(todaysWeekStart);
			if (todaysWeek == null) {
				todaysWeek = new WeekPlaceholder(todaysWeekStart);
			}
			currentlyShownWeek = todaysWeek;
			refreshView();
		});

		weekTable.setOnClickListener(v -> {
			if(weekCallback != null)
				weekCallback.onWeekTableClick();
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
		todayButton = view.findViewById(R.id.todayButton);
		previousWeekButton = view.findViewById(R.id.previous);
		nextWeekButton = view.findViewById(R.id.next);
		todayButton = view.findViewById(R.id.todayButton);
	}

	/**
	 * @param interval
	 *            position of the week relative to the currently displayed week, e.g. -2 for two weeks before the
	 *            currently displayed week
	 */
	private void changeDisplayedWeek(int interval) {
		if (interval == 0) {
			return;
		}

		DateTime targetWeekStart = DateTimeUtil.stringToDateTime(currentlyShownWeek.getStart()).plusDays(interval * 7);
		Week targetWeek = dao.getWeek(DateTimeUtil.dateTimeToString(targetWeekStart));
		if (targetWeek == null) {
			// don't insert a new week into the DB but only use a placeholder
			targetWeek = new WeekPlaceholder(DateTimeUtil.dateTimeToString(targetWeekStart));
		}

		// display a Toast indicating the change interval (helps the user for more than one week difference)
		if (Math.abs(interval) > 1) {
			CharSequence backwardOrForward = interval < 0 ? getText(R.string.backward) : getText(R.string.forward);
			Toast.makeText(getContext(), backwardOrForward + " " + Math.abs(interval) + " " + getText(R.string.weeks),
					Toast.LENGTH_SHORT).show();
		}

		currentlyShownWeek = targetWeek;
		refreshView();
	}

	public void refreshView() {
		if (currentlyShownWeek == null) {
			return;
		}

		DateTime monday = DateTimeUtil.stringToDateTime(currentlyShownWeek.getStart());
		DateTime tuesday = monday.plusDays(1);
		DateTime wednesday = tuesday.plusDays(1);
		DateTime thursday = wednesday.plusDays(1);
		DateTime friday = thursday.plusDays(1);
		DateTime saturday = friday.plusDays(1);
		DateTime sunday = saturday.plusDays(1);
		// set dates
		showActualDates(monday, tuesday, wednesday, thursday, friday, saturday, sunday);
		// highlight current day (if it is visible)
		// and reset the highlighting for the other days
		refreshRowHighlighting(monday, tuesday, wednesday, thursday, friday, saturday, sunday);
		// display times
		showTimes(monday, tuesday, wednesday, thursday, friday, saturday, sunday);
	}

	private void showActualDates(DateTime monday, DateTime tuesday, DateTime wednesday, DateTime thursday,
			DateTime friday, DateTime saturday, DateTime sunday) {
		topLeftCorner.setText("W " + thursday.getWeekIndex(DateTimeUtil.getBeginOfFirstWeekFor(thursday.getYear())));
		mondayLabel.setText(getString(R.string.monday) + getString(R.string.onespace)
				+ monday.format(getString(R.string.shortDateFormat)));
		tuesdayLabel.setText(getString(R.string.tuesday) + getString(R.string.onespace)
				+ tuesday.format(getString(R.string.shortDateFormat)));
		wednesdayLabel.setText(getString(R.string.wednesday) + getString(R.string.onespace)
				+ wednesday.format(getString(R.string.shortDateFormat)));
		thursdayLabel.setText(getString(R.string.thursday) + getString(R.string.onespace)
				+ thursday.format(getString(R.string.shortDateFormat)));
		fridayLabel.setText(getString(R.string.friday) + getString(R.string.onespace)
				+ friday.format(getString(R.string.shortDateFormat)));
		saturdayLabel.setText(getString(R.string.saturday) + getString(R.string.onespace)
				+ saturday.format(getString(R.string.shortDateFormat)));
		sundayLabel.setText(getString(R.string.sunday) + getString(R.string.onespace)
				+ sunday.format(getString(R.string.shortDateFormat)));
	}

	private void refreshRowHighlighting(DateTime monday, DateTime tuesday, DateTime wednesday, DateTime thursday,
		DateTime friday, DateTime saturday, DateTime sunday) {
		DateTime today = DateTimeUtil.getCurrentDateTime();
		mondayRow.setBackgroundResource(today.isSameDayAs(monday) ? R.drawable.table_row_highlighting
				: R.drawable.table_row);
		tuesdayRow.setBackgroundResource(today.isSameDayAs(tuesday) ? R.drawable.table_row_highlighting : 0);
		wednesdayRow.setBackgroundResource(today.isSameDayAs(wednesday) ? R.drawable.table_row_highlighting
				: R.drawable.table_row);
		thursdayRow.setBackgroundResource(today.isSameDayAs(thursday) ? R.drawable.table_row_highlighting : 0);
		fridayRow.setBackgroundResource(today.isSameDayAs(friday) ? R.drawable.table_row_highlighting
				: R.drawable.table_row);
		saturdayRow.setBackgroundResource(today.isSameDayAs(saturday) ? R.drawable.table_row_highlighting : 0);
		sundayRow.setBackgroundResource(today.isSameDayAs(sunday) ? R.drawable.table_row_highlighting
				: R.drawable.table_row);
	}

	private void showTimes(DateTime monday, DateTime tuesday, DateTime wednesday, DateTime thursday, DateTime friday,
			DateTime saturday, DateTime sunday) {
		if (currentlyShownWeek != null) {
			TimeSum flexiBalance = null;
			boolean hasRealData = !(currentlyShownWeek instanceof WeekPlaceholder);
			if (hasRealData && preferences.getBoolean(Key.ENABLE_FLEXI_TIME.getName(), false)) {
				flexiBalance = timerManager.getFlexiBalanceAtWeekStart(DateTimeUtil.stringToDateTime(currentlyShownWeek
						.getStart()));
			}
			boolean earlierEventsExist = (dao.getLastEventBefore(monday.getStartOfDay()) != null);
			boolean showFlexiTimes = hasRealData || earlierEventsExist;
			List<Event> events = fetchEventsForDay(monday);
			flexiBalance = showTimesForSingleDay(monday, events, flexiBalance, mondayIn, mondayOut, mondayWorked,
					mondayFlexi, showFlexiTimes);
			events = fetchEventsForDay(tuesday);
			flexiBalance = showTimesForSingleDay(tuesday, events, flexiBalance, tuesdayIn, tuesdayOut, tuesdayWorked,
					tuesdayFlexi, showFlexiTimes);
			events = fetchEventsForDay(wednesday);
			flexiBalance = showTimesForSingleDay(wednesday, events, flexiBalance, wednesdayIn, wednesdayOut,
					wednesdayWorked, wednesdayFlexi, showFlexiTimes);
			events = fetchEventsForDay(thursday);
			flexiBalance = showTimesForSingleDay(thursday, events, flexiBalance, thursdayIn, thursdayOut,
					thursdayWorked, thursdayFlexi, showFlexiTimes);
			events = fetchEventsForDay(friday);
			flexiBalance = showTimesForSingleDay(friday, events, flexiBalance, fridayIn, fridayOut, fridayWorked,
					fridayFlexi, showFlexiTimes);
			events = fetchEventsForDay(saturday);
			flexiBalance = showTimesForSingleDay(saturday, events, flexiBalance, saturdayIn, saturdayOut,
					saturdayWorked, saturdayFlexi, showFlexiTimes);
			events = fetchEventsForDay(sunday);
			flexiBalance = showTimesForSingleDay(sunday, events, flexiBalance, sundayIn, sundayOut, sundayWorked,
					sundayFlexi, showFlexiTimes);

			TimeSum amountWorked = timerManager.calculateTimeSum(DateTimeUtil.getWeekStart(DateTimeUtil
					.stringToDateTime(currentlyShownWeek.getStart())), PeriodEnum.WEEK);
			showSummaryLine(amountWorked, flexiBalance, showFlexiTimes && DateTimeUtil.isInPast(monday
					.getStartOfDay()));
		}
	}

	private List<Event> fetchEventsForDay(DateTime day) {
		Logger.debug("fetchEventsForDay: {}", DateTimeUtil.dateTimeToDateString(day));
		List<Event> ret = dao.getEventsOnDay(day);
		DateTime now = DateTimeUtil.getCurrentDateTime();
		Event lastEventBeforeNow = dao.getLastEventBefore(now);
		if (day.isSameDayAs(now) && TimerManager.isClockInEvent(lastEventBeforeNow)) {
			// currently clocked in: add clock-out event "NOW"
			ret.add(timerManager.createClockOutNowEvent());
		}
		return ret;
	}

	private TimeSum showTimesForSingleDay(DateTime day, List<Event> events, TimeSum flexiBalanceAtDayStart,
			TextView in, TextView out, TextView worked, TextView flexi, boolean showFlexiTimes) {

		DayLine dayLine = timeCalculator.calulateOneDay(day, events);

		WeekDayEnum weekDay = WeekDayEnum.getByValue(day.getWeekDay());
		boolean isWorkDay = timerManager.isWorkDay(weekDay);
		boolean isTodayOrEarlier = DateTimeUtil.isInPast(day.getStartOfDay());
		boolean containsEventsForDay = containsEventsForDay(events, day);
		boolean weekEndWithoutEvents = !isWorkDay && !containsEventsForDay;
		// correct result by previous flexi time sum
		dayLine.getTimeFlexi().addOrSubstract(flexiBalanceAtDayStart);

		in.setText(formatTime(dayLine.getTimeIn()));
		if (isCurrentMinute(dayLine.getTimeOut()) && timerManager.isTracking()) {
			out.setText("NOW");
		} else {
			out.setText(formatTime(dayLine.getTimeOut()));
		}
		if (weekEndWithoutEvents) {
			worked.setText("");
		} else if (isWorkDay && isTodayOrEarlier) {
			worked.setText(formatSum(dayLine.getTimeWorked(), null));
		} else {
			worked.setText(formatSum(dayLine.getTimeWorked(), ""));
		}
		if (!showFlexiTimes || weekEndWithoutEvents || !preferences.getBoolean(Key.ENABLE_FLEXI_TIME.getName(),
				false)) {
			flexi.setText("");
		} else if (isWorkDay && isTodayOrEarlier) {
			flexi.setText(formatSum(dayLine.getTimeFlexi(), null));
		} else if (containsEventsForDay) {
			flexi.setText(formatSum(dayLine.getTimeFlexi(), ""));
		} else {
			flexi.setText("");
		}

		return dayLine.getTimeFlexi();
	}

	private void showSummaryLine(TimeSum amountWorked, TimeSum flexiBalance, boolean showFlexiTimes) {
		totalWorked.setText(amountWorked.toString());
		if (flexiBalance != null && showFlexiTimes) {
			totalFlexi.setText(flexiBalance.toString());
		} else {
			totalFlexi.setText("");
		}
	}

	private boolean containsEventsForDay(List<Event> events, DateTime day) {
		for (Event event : events) {
			if (DateTimeUtil.stringToDateTime(event.getTime()).isSameDayAs(day)) {
				return true;
			}
		}
		return false;
	}

	private boolean isCurrentMinute(DateTime dateTime) {
		if (dateTime == null) {
			return false;
		}
		DateTime now = DateTimeUtil.getCurrentDateTime();
		return now.getYear().equals(dateTime.getYear())
				&& now.getMonth().equals(dateTime.getMonth())
				&& now.getDay().equals(dateTime.getDay())
				&& now.getHour().equals(dateTime.getHour())
				&& now.getMinute().equals(dateTime.getMinute());
	}

	private String formatTime(DateTime time) {
		return time == null ? "" : DateTimeUtil.dateTimeToHourMinuteString(time);
	}

	private String formatSum(TimeSum sum, String valueForZero) {
		if (sum != null && sum.getAsMinutes() == 0 && valueForZero != null) {
			return valueForZero;
		}
		return sum == null ? "" : sum.toString();
	}

	@Override
	public void onResume() {
		super.onResume();
		refreshView();
		weekRefreshAttacher.addObserver(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		dao.close();
		weekRefreshAttacher.removeObserver(this);
	}

}
