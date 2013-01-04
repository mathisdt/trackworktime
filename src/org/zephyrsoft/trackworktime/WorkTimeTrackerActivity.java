/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekDayEnum;
import org.zephyrsoft.trackworktime.model.WeekPlaceholder;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.Logger;
import org.zephyrsoft.trackworktime.util.SimpleGestureFilter;
import org.zephyrsoft.trackworktime.util.SimpleGestureListener;

/**
 * Main activity of the application.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class WorkTimeTrackerActivity extends Activity implements SimpleGestureListener {
	
	private static final int EDIT_EVENTS = 0;
	private static final int EDIT_TASKS = 1;
	private static final int OPTIONS = 2;
	
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
	private TextView taskLabel = null;
	private Spinner task = null;
	private TextView textLabel = null;
	private EditText text = null;
	private Button clockInOutButton = null;
	
	private OnClickListener clockInOut = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// commit text field
			text.clearFocus();
			
			if (timerManager.isTracking() && !taskOrTextChanged) {
				timerManager.stopTracking();
			} else {
				Task selectedTask = (Task) task.getSelectedItem();
				String description = text.getText().toString();
				if (timerManager.isTracking() && taskOrTextChanged) {
					timerManager.startTracking(selectedTask, description);
				} else {
					timerManager.startTracking(selectedTask, description);
				}
			}
			
			Logger.debug("setting taskOrTextChanged to false");
			taskOrTextChanged = false;
			refreshView();
		}
	};
	private TaskAndTextListener taskAndTextListener = new TaskAndTextListener();
	
	private static WorkTimeTrackerActivity instance = null;
	
	private SharedPreferences preferences;
	private DAO dao = null;
	private TimerManager timerManager = null;
	private ArrayAdapter<Task> tasksAdapter;
	private boolean reloadTasksOnResume = false;
	private List<Task> tasks;
	private boolean taskOrTextChanged = false;
	private Week currentlyShownWeek;
	
	private SimpleGestureFilter detector;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		instance = this;
		Basics basics = Basics.getOrCreateInstance(getApplicationContext());
		// fill basic data from central structures
		preferences = basics.getPreferences();
		dao = basics.getDao();
		timerManager = basics.getTimerManager();
		
		setContentView(R.layout.main);
		
		findAllViewsById();
		
		detector = new SimpleGestureFilter(this, this);
		weekTable.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showEventList();
			}
		});
		
		clockInOutButton.setOnClickListener(clockInOut);
		
		// make the clock-in/clock-out button change its title when changing
		// task and/or text
		task.setOnItemSelectedListener(taskAndTextListener);
		text.setOnKeyListener(taskAndTextListener);
		
		// delegate the rest of the work to onResume()
		reloadTasksOnResume = true;
	}
	
	/**
	 * @param interval position of the week relative to the currently displayed week, e.g. -2 for two weeks before the
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
	
	protected void refreshView() {
		// TODO update task and text from current tracking period (if tracking)?
		// ATTENTION: setting "taskOrTextChanged" interferes with the TaskAndTextListener!
//		if (timerManager.isTracking()) {
//			Event latestEvent = dao.getLastEventBefore(DateTimeUtil.getCurrentDateTime());
//			Task latestTask = dao.getTask(latestEvent.getTask());
//			Integer index = tasks.indexOf(latestTask);
//			task.setSelection(index);
//			text.setText(latestEvent.getText());
//			taskOrTextChanged = false;
//		}
		// button text
		if (timerManager.isTracking() && !taskOrTextChanged) {
			clockInOutButton.setText(R.string.clockOut);
		} else if (timerManager.isTracking() && taskOrTextChanged) {
			clockInOutButton.setText(R.string.clockInChange);
		} else {
			clockInOutButton.setText(R.string.clockIn);
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
				flexiBalance = timerManager.getFlexiBalanceAtWeekStart(currentlyShownWeek.getStart());
			}
			List<Event> events = fetchEventsForDay(monday);
			flexiBalance =
				showTimesForSingleDay(monday, events, flexiBalance, mondayIn, mondayOut, mondayWorked, mondayFlexi);
			events = fetchEventsForDay(tuesday);
			flexiBalance =
				showTimesForSingleDay(tuesday, events, flexiBalance, tuesdayIn, tuesdayOut, tuesdayWorked, tuesdayFlexi);
			events = fetchEventsForDay(wednesday);
			flexiBalance =
				showTimesForSingleDay(wednesday, events, flexiBalance, wednesdayIn, wednesdayOut, wednesdayWorked,
					wednesdayFlexi);
			events = fetchEventsForDay(thursday);
			flexiBalance =
				showTimesForSingleDay(thursday, events, flexiBalance, thursdayIn, thursdayOut, thursdayWorked,
					thursdayFlexi);
			events = fetchEventsForDay(friday);
			flexiBalance =
				showTimesForSingleDay(friday, events, flexiBalance, fridayIn, fridayOut, fridayWorked, fridayFlexi);
			events = fetchEventsForDay(saturday);
			flexiBalance =
				showTimesForSingleDay(saturday, events, flexiBalance, saturdayIn, saturdayOut, saturdayWorked,
					saturdayFlexi);
			events = fetchEventsForDay(sunday);
			flexiBalance =
				showTimesForSingleDay(sunday, events, flexiBalance, sundayIn, sundayOut, sundayWorked, sundayFlexi);
			
			TimeSum amountWorked =
				timerManager.calculateTimeSum(
					DateTimeUtil.getWeekStart(DateTimeUtil.stringToDateTime(currentlyShownWeek.getStart())),
					PeriodEnum.WEEK);
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
		CharSequence timeIn = null;
		CharSequence timeOut = null;
		CharSequence timeWorked = null;
		CharSequence timeFlexi = null;
		TimeSum flexiBalance = flexiBalanceAtDayStart;
		
		Event lastEventBeforeToday = dao.getLastEventBefore(day);
		if (!events.isEmpty()) {
			// take special care of the event type (CLOCK_IN vs. CLOCK_OUT/CLOCK_OUT_NOW)
			Event firstClockInEvent = null;
			for (Event event : events) {
				if (TimerManager.isClockInEvent(event)) {
					firstClockInEvent = event;
					break;
				}
			}
			Event effectiveClockOutEvent = null;
			for (int i = events.size() - 1; i >= 0; i--) {
				Event event = events.get(i);
				if (TimerManager.isClockOutEvent(event)) {
					effectiveClockOutEvent = event;
				}
				if (TimerManager.isClockInEvent(event)) {
					break;
				}
			}
			
			if (TimerManager.isClockInEvent(lastEventBeforeToday)) {
				// clocked in since begin of day
				timeIn = DateTimeUtil.dateTimeToHourMinuteString(day.getStartOfDay());
			} else if (firstClockInEvent != null) {
				timeIn =
					DateTimeUtil.dateTimeToHourMinuteString(DateTimeUtil.stringToDateTime(firstClockInEvent.getTime()));
			} else {
				// apparently not clocked in before begin of day and no clock-in event
				timeIn = "";
			}
			
			if (effectiveClockOutEvent != null) {
				timeOut =
					DateTimeUtil.dateTimeToHourMinuteString(DateTimeUtil.stringToDateTime(effectiveClockOutEvent
						.getTime()));
				// replace time with NOW if applicable
				if (effectiveClockOutEvent.getType().equals(TypeEnum.CLOCK_OUT_NOW.getValue())) {
					timeOut = getText(R.string.now);
				}
			} else {
				timeOut = DateTimeUtil.dateTimeToHourMinuteString(day.getEndOfDay());
			}
			
			TimeSum amountWorked = timerManager.calculateTimeSum(day, PeriodEnum.DAY);
			timeWorked = amountWorked.toString();
			
			if (flexiBalance != null) {
				flexiBalance.addOrSubstract(amountWorked);
				// substract the "normal" work time for one day
				WeekDayEnum weekDay = WeekDayEnum.getByValue(day.getWeekDay());
				int normalWorkTimeInMinutes = timerManager.getNormalWorkDurationFor(weekDay);
				flexiBalance.substract(0, normalWorkTimeInMinutes);
				timeFlexi = flexiBalance.toString();
			} else {
				timeFlexi = "";
			}
			
		} else {
			DateTime now = DateTimeUtil.getCurrentDateTime();
			if (TimerManager.isClockInEvent(lastEventBeforeToday) && day.getStartOfDay().lt(now)) {
				// although there are no events on this day, the user is clocked in all day long - else there would be a
				// CLOCK_OUT_NOW event!
				timeIn = DateTimeUtil.dateTimeToHourMinuteString(day.getStartOfDay());
				timeOut = DateTimeUtil.dateTimeToHourMinuteString(day.getEndOfDay());
				timeWorked = DateTimeUtil.getCompleteDayAsHourMinuteString();
				if (flexiBalance != null) {
					flexiBalance.add(24, 0);
					// substract the "normal" work time for one day
					WeekDayEnum weekDay = WeekDayEnum.getByValue(day.getWeekDay());
					int normalWorkTimeInMinutes = timerManager.getNormalWorkDurationFor(weekDay);
					flexiBalance.substract(0, normalWorkTimeInMinutes);
					timeFlexi = flexiBalance.toString();
				} else {
					timeFlexi = "";
				}
			} else {
				// clear all/most fields
				timeIn = "";
				timeOut = "";
				timeWorked = "";
				
				WeekDayEnum weekDay = WeekDayEnum.getByValue(day.getWeekDay());
				if (flexiBalance != null && timerManager.isWorkDay(weekDay)) {
					// substract the "normal" work time for one day
					int normalWorkTimeInMinutes = timerManager.getNormalWorkDurationFor(weekDay);
					flexiBalance.substract(0, normalWorkTimeInMinutes);
					if (DateTimeUtil.isInPast(day.getStartOfDay())) {
						// only show for days up to now, not for future days
						timeFlexi = flexiBalance.toString();
					} else {
						timeFlexi = "";
					}
				} else {
					timeFlexi = "";
				}
			}
		}
		
		in.setText(timeIn);
		out.setText(timeOut);
		worked.setText(timeWorked);
		flexi.setText(timeFlexi);
		
		return flexiBalance;
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
		taskLabel = (TextView) findViewById(R.id.taskLabel);
		task = (Spinner) findViewById(R.id.task);
		textLabel = (TextView) findViewById(R.id.textLabel);
		text = (EditText) findViewById(R.id.text);
		clockInOutButton = (Button) findViewById(R.id.clockInOutButton);
	}
	
	/**
	 * Mark task list as changed so it will be re-read from the database the next time the GUI is refreshed.
	 */
	public void refreshTasks() {
		reloadTasksOnResume = true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, EDIT_EVENTS, EDIT_EVENTS, R.string.edit_events).setIcon(R.drawable.ic_menu_edit);
		menu.add(Menu.NONE, EDIT_TASKS, EDIT_TASKS, R.string.edit_tasks).setIcon(R.drawable.ic_menu_sort_by_size);
		menu.add(Menu.NONE, OPTIONS, OPTIONS, R.string.options).setIcon(R.drawable.ic_menu_preferences);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case EDIT_EVENTS:
				showEventList();
				return true;
			case EDIT_TASKS:
				showTaskList();
				return true;
			case OPTIONS:
				showOptions();
				return true;
			default:
				throw new IllegalArgumentException("options menu: unknown item selected");
		}
	}
	
	private void showEventList() {
		Logger.debug("showing EventList");
		Intent i = new Intent(this, EventListActivity.class);
		i.putExtra(EventListActivity.WEEK_START_EXTRA_KEY, currentlyShownWeek.getStart());
		startActivity(i);
	}
	
	private void showTaskList() {
		Logger.debug("showing TaskList");
		Intent i = new Intent(this, TaskListActivity.class);
		startActivity(i);
	}
	
	private void showOptions() {
		Logger.debug("showing Options");
		Intent i = new Intent(this, OptionsActivity.class);
		startActivity(i);
	}
	
	@Override
	protected void onResume() {
		Logger.debug("onResume called");
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
		Basics.getInstance().checkPersistentNotification();
		
		refreshView();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		Logger.debug("onPause called");
		dao.close();
		super.onPause();
	}
	
	/**
	 * Get the instance of this activity.
	 */
	public static WorkTimeTrackerActivity getInstance() {
		if (instance == null) {
			throw new IllegalStateException("the WTT activity is not created yet");
		}
		return instance;
	}
	
	/**
	 * Listener for task dropdown field and text field. Triggers a refresh of the main activity.
	 */
	private class TaskAndTextListener implements OnItemSelectedListener, OnKeyListener {
		
		private void valueChanged() {
			Logger.debug("setting taskOrTextChanged to true");
			taskOrTextChanged = true;
			refreshView();
		}
		
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			valueChanged();
			return false;
		}
		
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			valueChanged();
		}
		
		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			valueChanged();
		}
		
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent me) {
		// first pass the events to the SimpleGestureFilter
		this.detector.onTouchEvent(me);
		// then process them normally
		return super.dispatchTouchEvent(me);
	}
	
	@Override
	public void onSwipe(int direction) {
		switch (direction) {
			case SimpleGestureFilter.SWIPE_RIGHT:
				changeDisplayedWeek(-1);
				break;
			case SimpleGestureFilter.SWIPE_LEFT:
				changeDisplayedWeek(1);
				break;
//			case SimpleGestureFilter.SWIPE_DOWN:
//				// display 4 weeks before
//				changeDisplayedWeek(-4);
//				break;
//			case SimpleGestureFilter.SWIPE_UP:
//				// display 4 weeks after
//				changeDisplayedWeek(4);
//				break;
			default:
				// do nothing
		}
	}
	
	@Override
	public void onDoubleTap() {
		// do nothing
	}
	
}
