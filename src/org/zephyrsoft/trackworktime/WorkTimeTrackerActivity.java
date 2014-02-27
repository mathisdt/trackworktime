/*
 * This file is part of TrackWorkTime (TWT).
 * 
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime;

import hirondelle.date4j.DateTime;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.DayLine;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekDayEnum;
import org.zephyrsoft.trackworktime.model.WeekPlaceholder;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.timer.TimeCalculator;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.Logger;
import org.zephyrsoft.trackworktime.util.PreferencesUtil;

/**
 * Main activity of the application.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class WorkTimeTrackerActivity extends Activity {

	private static enum MenuAction {
		EDIT_EVENTS,
		EDIT_TASKS,
		INSERT_DEFAULT_TIMES,
		OPTIONS,
		USE_CURRENT_LOCATION,
		REPORTS,
		ABOUT;

		public static MenuAction byOrdinal(int ordinal) {
			return values()[ordinal];
		}
	}

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
	private Spinner task = null;
	private EditText text = null;
	private Button clockInButton = null;
	private Button clockOutButton = null;
	private Button previousWeekButton = null;
	private Button nextWeekButton = null;
	private Button todayButton = null;
	
	private static WorkTimeTrackerActivity instance = null;

	private boolean visible = false;

	private boolean tabsAreChanging = false;

	private SharedPreferences preferences;
	private DAO dao = null;
	private TimerManager timerManager = null;
	private TimeCalculator timeCalculator = null;
	private ArrayAdapter<Task> tasksAdapter;
	private boolean reloadTasksOnResume = false;
	private List<Task> tasks;
	private Week currentlyShownWeek;

	private void checkAllOptions() {
		int disabledSections = PreferencesUtil.checkAllPreferenceSections();

		if (disabledSections > 0) {
			// show message to user
			Intent messageIntent = Basics
				.getInstance()
				.createMessageIntent(
					disabledSections == 1 ? "One option was disabled due to invalid values or value combinations.\n\nYou can re-enable it after you checked the values you entered."
						: String.valueOf(disabledSections)
							+ " options were disabled due to invalid values or value combinations.\n\nYou can re-enable them after you checked the values you entered.",
					null);
			startActivity(messageIntent);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		instance = this;
		Basics basics = Basics.getOrCreateInstance(getApplicationContext());
		// fill basic data from central structures
		preferences = basics.getPreferences();
		dao = basics.getDao();
		timerManager = basics.getTimerManager();
		timeCalculator = basics.getTimeCalculator();

		setContentView(R.layout.main);

		findAllViewsById();

		weekTable.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showEventList();
			}
		});

		previousWeekButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				changeDisplayedWeek(-1);
			}
		});

		nextWeekButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				changeDisplayedWeek(1);
			}
		});

		clockInButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clockInAction(0);
			}
		});
		clockInButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Intent i = new Intent(getApplicationContext(), TimeAheadActivity.class);
				String typeString = null;
				if (timerManager.isTracking()) {
					typeString = getString(R.string.clockInChange);
				} else {
					typeString = getString(R.string.clockIn);
				}
				i.putExtra(Constants.TYPE_EXTRA_KEY, 0);
				i.putExtra(Constants.TYPE_STRING_EXTRA_KEY, typeString);
				startActivity(i);
				return true;
			}
		});

		clockOutButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clockOutAction(0);
			}
		});
		clockOutButton.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Intent i = new Intent(getApplicationContext(), TimeAheadActivity.class);
				String typeString = null;
				if (timerManager.isTracking()) {
					typeString = getString(R.string.clockOut);
					i.putExtra(Constants.TYPE_EXTRA_KEY, 1);
					i.putExtra(Constants.TYPE_STRING_EXTRA_KEY, typeString);
					startActivity(i);
				}
				return true;
			}
		});
		
		todayButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				final String todaysWeekStart = DateTimeUtil.getWeekStartAsString(DateTimeUtil.getCurrentDateTime());
				Week todaysWeek = dao.getWeek(todaysWeekStart);
				if (todaysWeek == null){
					todaysWeek = new WeekPlaceholder(todaysWeekStart);
				}
				currentlyShownWeek = todaysWeek;
				refreshView();
			}
		});

		// delegate the rest of the work to onResume()
		reloadTasksOnResume = true;

		// check options for logical errors
		checkAllOptions();
	}

	private static void setVisibility(View view, boolean visible) {
		if (visible) {
			view.setVisibility(View.VISIBLE);
		} else {
			view.setVisibility(View.GONE);
		}
	}

	/**
	 * @param minutesToPredate
	 *            if greater than 0, predate the event this many minutes
	 */
	public void clockInAction(int minutesToPredate) {
		if (minutesToPredate < 0) {
			throw new IllegalArgumentException("no negative argument allowed");
		}
		// commit text field
		text.clearFocus();

		Task selectedTask = (Task) task.getSelectedItem();
		String description = text.getText().toString();
		timerManager.startTracking(minutesToPredate, selectedTask, description);
		refreshView();
	}

	/**
	 * @param minutesToPredate
	 *            if greater than 0, predate the event this many minutes
	 */
	public void clockOutAction(int minutesToPredate) {
		if (minutesToPredate < 0) {
			throw new IllegalArgumentException("no negative argument allowed");
		}

		timerManager.stopTracking(minutesToPredate);
		refreshView();
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
			Toast.makeText(this, backwardOrForward + " " + Math.abs(interval) + " " + getText(R.string.weeks),
				Toast.LENGTH_SHORT).show();
		}

		currentlyShownWeek = targetWeek;
		refreshView();
	}

	/**
	 * Reloads the view's data if the view is currently shown.
	 */
	public static void refreshViewIfShown() {
		if (instance != null && instance.visible) {
			Logger.debug("refreshing main view (it is visible at the moment)");
			instance.refreshView();
		}
	}

	protected void refreshView() {
		clockOutButton.setEnabled(timerManager.isTracking());
		Task taskToSelect = null;
		if (timerManager.isTracking()) {
			clockInButton.setText(R.string.clockInChange);
			taskToSelect = timerManager.getCurrentTask();
		} else {
			clockInButton.setText(R.string.clockIn);
			taskToSelect = dao.getDefaultTask();
		}
		if (taskToSelect != null) {
			int i = 0;
			for (Task oneTask : tasks) {
				if (oneTask.getId().equals(taskToSelect.getId())) {
					task.setSelection(i);
					break;
				}
				i++;
			}
		}

		if (currentlyShownWeek != null) {
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

	private void showTimes(DateTime monday, DateTime tuesday, DateTime wednesday, DateTime thursday, DateTime friday,
		DateTime saturday, DateTime sunday) {
		if (currentlyShownWeek != null) {
			TimeSum flexiBalance = null;
			if (!(currentlyShownWeek instanceof WeekPlaceholder)
				&& preferences.getBoolean(Key.ENABLE_FLEXI_TIME.getName(), false)) {
				flexiBalance = timerManager.getFlexiBalanceAtWeekStart(DateTimeUtil.stringToDateTime(currentlyShownWeek
					.getStart()));
			}
			List<Event> events = fetchEventsForDay(monday);
			flexiBalance = showTimesForSingleDay(monday, events, flexiBalance, mondayIn, mondayOut, mondayWorked,
				mondayFlexi);
			events = fetchEventsForDay(tuesday);
			flexiBalance = showTimesForSingleDay(tuesday, events, flexiBalance, tuesdayIn, tuesdayOut, tuesdayWorked,
				tuesdayFlexi);
			events = fetchEventsForDay(wednesday);
			flexiBalance = showTimesForSingleDay(wednesday, events, flexiBalance, wednesdayIn, wednesdayOut,
				wednesdayWorked, wednesdayFlexi);
			events = fetchEventsForDay(thursday);
			flexiBalance = showTimesForSingleDay(thursday, events, flexiBalance, thursdayIn, thursdayOut,
				thursdayWorked, thursdayFlexi);
			events = fetchEventsForDay(friday);
			flexiBalance = showTimesForSingleDay(friday, events, flexiBalance, fridayIn, fridayOut, fridayWorked,
				fridayFlexi);
			events = fetchEventsForDay(saturday);
			flexiBalance = showTimesForSingleDay(saturday, events, flexiBalance, saturdayIn, saturdayOut,
				saturdayWorked, saturdayFlexi);
			events = fetchEventsForDay(sunday);
			flexiBalance = showTimesForSingleDay(sunday, events, flexiBalance, sundayIn, sundayOut, sundayWorked,
				sundayFlexi);

			TimeSum amountWorked = timerManager.calculateTimeSum(DateTimeUtil.getWeekStart(DateTimeUtil
				.stringToDateTime(currentlyShownWeek.getStart())), PeriodEnum.WEEK);
			showSummaryLine(amountWorked, flexiBalance);
		}
	}

	private List<Event> fetchEventsForDay(DateTime day) {
		Logger.debug("fetchEventsForDay: {0}", DateTimeUtil.dateTimeToDateString(day));
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
		TextView in, TextView out, TextView worked, TextView flexi) {

		DayLine dayLine = timeCalculator.calulateOneDay(events);

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
		if (weekEndWithoutEvents || !preferences.getBoolean(Key.ENABLE_FLEXI_TIME.getName(), false)) {
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

	private void showSummaryLine(TimeSum amountWorked, TimeSum flexiBalance) {
		totalWorked.setText(amountWorked.toString());
		if (flexiBalance != null) {
			totalFlexi.setText(flexiBalance.toString());
		}
	}

	private void setupTasksAdapter() {
		tasks = dao.getActiveTasks();
		tasksAdapter = new ArrayAdapter<Task>(this, android.R.layout.simple_list_item_1, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		task.setAdapter(tasksAdapter);
	}

	private void findAllViewsById() {
		weekTable = (TableLayout) findViewById(R.id.week_table);
		titleRow = (TableRow) findViewById(R.id.titleRow);
		topLeftCorner = (TextView) findViewById(R.id.topLeftCorner);
		inLabel = (TextView) findViewById(R.id.inLabel);
		outLabel = (TextView) findViewById(R.id.outLabel);
		workedLabel = (TextView) findViewById(R.id.workedLabel);
		flexiLabel = (TextView) findViewById(R.id.flexiLabel);
		mondayRow = (TableRow) findViewById(R.id.mondayRow);
		mondayLabel = (TextView) findViewById(R.id.mondayLabel);
		mondayIn = (TextView) findViewById(R.id.mondayIn);
		mondayOut = (TextView) findViewById(R.id.mondayOut);
		mondayWorked = (TextView) findViewById(R.id.mondayWorked);
		mondayFlexi = (TextView) findViewById(R.id.mondayFlexi);
		tuesdayRow = (TableRow) findViewById(R.id.tuesdayRow);
		tuesdayLabel = (TextView) findViewById(R.id.tuesdayLabel);
		tuesdayIn = (TextView) findViewById(R.id.tuesdayIn);
		tuesdayOut = (TextView) findViewById(R.id.tuesdayOut);
		tuesdayWorked = (TextView) findViewById(R.id.tuesdayWorked);
		tuesdayFlexi = (TextView) findViewById(R.id.tuesdayFlexi);
		wednesdayRow = (TableRow) findViewById(R.id.wednesdayRow);
		wednesdayLabel = (TextView) findViewById(R.id.wednesdayLabel);
		wednesdayIn = (TextView) findViewById(R.id.wednesdayIn);
		wednesdayOut = (TextView) findViewById(R.id.wednesdayOut);
		wednesdayWorked = (TextView) findViewById(R.id.wednesdayWorked);
		wednesdayFlexi = (TextView) findViewById(R.id.wednesdayFlexi);
		thursdayRow = (TableRow) findViewById(R.id.thursdayRow);
		thursdayLabel = (TextView) findViewById(R.id.thursdayLabel);
		thursdayIn = (TextView) findViewById(R.id.thursdayIn);
		thursdayOut = (TextView) findViewById(R.id.thursdayOut);
		thursdayWorked = (TextView) findViewById(R.id.thursdayWorked);
		thursdayFlexi = (TextView) findViewById(R.id.thursdayFlexi);
		fridayRow = (TableRow) findViewById(R.id.fridayRow);
		fridayLabel = (TextView) findViewById(R.id.fridayLabel);
		fridayIn = (TextView) findViewById(R.id.fridayIn);
		fridayOut = (TextView) findViewById(R.id.fridayOut);
		fridayWorked = (TextView) findViewById(R.id.fridayWorked);
		fridayFlexi = (TextView) findViewById(R.id.fridayFlexi);
		saturdayRow = (TableRow) findViewById(R.id.saturdayRow);
		saturdayLabel = (TextView) findViewById(R.id.saturdayLabel);
		saturdayIn = (TextView) findViewById(R.id.saturdayIn);
		saturdayOut = (TextView) findViewById(R.id.saturdayOut);
		saturdayWorked = (TextView) findViewById(R.id.saturdayWorked);
		saturdayFlexi = (TextView) findViewById(R.id.saturdayFlexi);
		sundayRow = (TableRow) findViewById(R.id.sundayRow);
		sundayLabel = (TextView) findViewById(R.id.sundayLabel);
		sundayIn = (TextView) findViewById(R.id.sundayIn);
		sundayOut = (TextView) findViewById(R.id.sundayOut);
		sundayWorked = (TextView) findViewById(R.id.sundayWorked);
		sundayFlexi = (TextView) findViewById(R.id.sundayFlexi);
		totalRow = (TableRow) findViewById(R.id.totalRow);
		totalLabel = (TextView) findViewById(R.id.totalLabel);
		totalIn = (TextView) findViewById(R.id.totalIn);
		totalOut = (TextView) findViewById(R.id.totalOut);
		totalWorked = (TextView) findViewById(R.id.totalWorked);
		totalFlexi = (TextView) findViewById(R.id.totalFlexi);
		previousWeekButton = (Button) findViewById(R.id.previous);
		nextWeekButton = (Button) findViewById(R.id.next);
		task = (Spinner) findViewById(R.id.task);
		text = (EditText) findViewById(R.id.text);
		clockInButton = (Button) findViewById(R.id.clockInButton);
		clockOutButton = (Button) findViewById(R.id.clockOutButton);
		todayButton = (Button) findViewById(R.id.todayButton);
	}

	/**
	 * Mark task list as changed so it will be re-read from the database the next time the GUI is refreshed.
	 */
	public void refreshTasks() {
		reloadTasksOnResume = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MenuAction.EDIT_EVENTS.ordinal(), MenuAction.EDIT_EVENTS.ordinal(), R.string.edit_events)
			.setIcon(R.drawable.ic_menu_edit);
		menu.add(Menu.NONE, MenuAction.EDIT_TASKS.ordinal(), MenuAction.EDIT_TASKS.ordinal(), R.string.edit_tasks)
			.setIcon(R.drawable.ic_menu_sort_by_size);
		menu.add(Menu.NONE, MenuAction.INSERT_DEFAULT_TIMES.ordinal(), MenuAction.INSERT_DEFAULT_TIMES.ordinal(),
			R.string.insert_default_times).setIcon(R.drawable.ic_menu_mark);
		menu.add(Menu.NONE, MenuAction.OPTIONS.ordinal(), MenuAction.OPTIONS.ordinal(), R.string.options).setIcon(
			R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, MenuAction.USE_CURRENT_LOCATION.ordinal(), MenuAction.USE_CURRENT_LOCATION.ordinal(),
			R.string.use_current_location).setIcon(R.drawable.ic_menu_compass);
		menu.add(Menu.NONE, MenuAction.REPORTS.ordinal(), MenuAction.REPORTS.ordinal(), R.string.reports).setIcon(
			R.drawable.ic_menu_agenda);
		menu.add(Menu.NONE, MenuAction.ABOUT.ordinal(), MenuAction.ABOUT.ordinal(), R.string.about).setIcon(
			R.drawable.ic_menu_star);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (MenuAction.byOrdinal(item.getItemId())) {
			case EDIT_EVENTS:
				showEventList();
				return true;
			case EDIT_TASKS:
				showTaskList();
				return true;
			case INSERT_DEFAULT_TIMES:
				showInsertDefaultTimes();
				return true;
			case OPTIONS:
				showOptions();
				return true;
			case USE_CURRENT_LOCATION:
				useCurrentLocationAsWorkplace();
				return true;
			case REPORTS:
				showReports();
				return true;
			case ABOUT:
				showAbout();
				return true;
			default:
				throw new IllegalArgumentException("options menu: unknown item selected");
		}
	}

	private void showEventList() {
		Logger.debug("showing EventList");
		Intent i = new Intent(this, EventListActivity.class);
		i.putExtra(Constants.WEEK_START_EXTRA_KEY, currentlyShownWeek.getStart());
		startActivity(i);
	}

	private void showTaskList() {
		Logger.debug("showing TaskList");
		Intent i = new Intent(this, TaskListActivity.class);
		startActivity(i);
	}

	private void showInsertDefaultTimes() {
		Logger.debug("showing InsertDefaultTimes");
		Intent i = new Intent(this, InsertDefaultTimesActivity.class);
		startActivity(i);
	}

	private void showOptions() {
		Logger.debug("showing Options");
		Intent i = new Intent(this, OptionsActivity.class);
		startActivity(i);
	}

	private void useCurrentLocationAsWorkplace() {
		Logger.debug("use current location as work place");
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.use_current_location));
		alert.setMessage(getString(R.string.really_use_current_location));
		alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				Basics.getInstance().useCurrentLocationAsWorkplace(WorkTimeTrackerActivity.this);
			}
		});
		alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				// do nothing
			}
		});
		alert.show();
	}

	private void showReports() {
		Logger.debug("showing Reports");
		Intent i = new Intent(this, ReportsActivity.class);
		startActivity(i);
	}

	private void showAbout() {
		Logger.debug("showing About");
		Intent i = new Intent(this, AboutActivity.class);
		startActivity(i);
	}

	@Override
	protected void onResume() {
		Logger.debug("onResume called");
		visible = true;
		if (reloadTasksOnResume) {
			reloadTasksOnResume = false;
			setupTasksAdapter();
		}

		String weekStart = DateTimeUtil.getWeekStartAsString(DateTimeUtil.getCurrentDateTime());
		currentlyShownWeek = dao.getWeek(weekStart);
		if (currentlyShownWeek == null) {
			// don't insert a new week into the DB but only use a placeholder
			currentlyShownWeek = new WeekPlaceholder(weekStart);
		}
		Basics.getInstance().safeCheckPersistentNotification();

		refreshView();
		super.onResume();
	}

	@Override
	protected void onPause() {
		Logger.debug("onPause called");
		dao.close();
		visible = false;
		super.onPause();
	}

	/**
	 * Get the instance of this activity. If it was garbage-collected in the meantime, throw an exception.
	 */
	public static WorkTimeTrackerActivity getInstance() {
		if (instance == null) {
			throw new IllegalStateException("the main activity is not created yet or was dumped in the meantime");
		}
		return instance;
	}

	/**
	 * Get the instance of this activity. If it was garbage-collected in the meantime, return {@code null}.
	 */
	public static WorkTimeTrackerActivity getInstanceOrNull() {
		return instance;
	}

	private Event getLastEventIfClockIn() {
		Event event = dao.getLastEventBefore(DateTimeUtil.getCurrentDateTime());
		if (event != null && event.getType() != null && event.getType().equals(TypeEnum.CLOCK_IN.getValue())) {
			return event;
		} else {
			return null;
		}
	}

	private static boolean equalsWithNullEqualsEmptyString(String one, String two) {
		return (one == null && two == null) || (one != null && one.length() == 0 && two == null)
			|| (one != null && one.length() == 0 && two == null) || (one == null && two != null && two.length() == 0)
			|| (one != null && one.equals(two));
	}

}
