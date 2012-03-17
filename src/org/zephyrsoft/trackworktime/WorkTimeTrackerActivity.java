package org.zephyrsoft.trackworktime;

import hirondelle.date4j.DateTime;
import hirondelle.date4j.DateTime.DayOverflow;
import hirondelle.date4j.DateTime.Unit;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
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
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.SimpleGestureFilter;
import org.zephyrsoft.trackworktime.util.SimpleGestureListener;

/**
 * Main activity of the application.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class WorkTimeTrackerActivity extends Activity implements SimpleGestureListener {
	
	private static final int EDIT_TASKS = 0;
	private static final int OPTIONS = 1;
	
	private TableRow titleRow = null;
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
			
			if (timerManager.isTracking() && !taskOrTextChanged) {
				timerManager.stopTracking();
			} else {
				Task selectedTask = (Task) task.getSelectedItem();
				String description = text.getText().toString();
				if (timerManager.isTracking() && taskOrTextChanged) {
					timerManager.startTracking(selectedTask, description);
				} else if (!timerManager.isTracking()) {
					timerManager.startTracking(selectedTask, description);
				}
			}
			
			taskOrTextChanged = false;
			refreshView();
		}
	};
	private TaskAndTextListener taskAndTextListener = new TaskAndTextListener();
	
	private static WorkTimeTrackerActivity instance = null;
	
	private DAO dao = null;
	private TimerManager timerManager = null;
	private ArrayAdapter<Task> tasksAdapter;
	private boolean reloadTasksOnResume = false;
	private SharedPreferences preferences;
	private List<Task> tasks;
	private boolean taskOrTextChanged = false;
	private Week currentlyShownWeek;
	
	private SimpleGestureFilter detector;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		instance = this;
		
		readPreferences();
		
		setContentView(R.layout.main);
		
		findAllViewsById();
		
		detector = new SimpleGestureFilter(this, this);
		
		clockInOutButton.setOnClickListener(clockInOut);
		
		// make the clock-in/clock-out button change its title when changing
		// task and/or text
		task.setOnItemSelectedListener(taskAndTextListener);
		text.setOnKeyListener(taskAndTextListener);
		
		dao = new DAO(this);
		dao.open();
		timerManager = new TimerManager(dao);
		
		setupTasksAdapter();
		
		String weekStart = DateTimeUtil.getWeekStart(DateTimeUtil.getCurrentDateTime());
		currentlyShownWeek = dao.getWeek(weekStart);
		if (currentlyShownWeek == null) {
			currentlyShownWeek = dao.insertWeek(new Week(null, weekStart, null));
		}
		
		refreshView();
	}
	
	private void readPreferences() {
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		// TODO
	}
	
	/**
	 * @param interval position of the week relative to the currently displayed week, e.g. -2 for two weeks before
	 *            the currently displayed week
	 */
	private void changeDisplayedWeek(int interval) {
		if (interval == 0) {
			return;
		}
		
		DateTime targetWeekStart = DateTimeUtil.stringToDateTime(currentlyShownWeek.getStart()).plusDays(interval * 7);
		Week targetWeek = dao.getWeek(DateTimeUtil.dateTimeToString(targetWeekStart));
		if (targetWeek == null) {
			targetWeek = dao.insertWeek(new Week(null, DateTimeUtil.dateTimeToString(targetWeekStart), null));
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
		// update task and text from current tracking period (if currently
		// tracking)
		if (timerManager.isTracking()) {
			Event latestEvent = dao.getLatestEvent();
			Task latestTask = dao.getTask(latestEvent.getTask());
			Integer index = tasks.indexOf(latestTask);
			task.setSelection(index);
			text.setText(latestEvent.getText());
			taskOrTextChanged = false;
		}
		// button text
		if (timerManager.isTracking() && !taskOrTextChanged) {
			clockInOutButton.setText(R.string.clockOut);
		} else if (timerManager.isTracking() && taskOrTextChanged) {
			clockInOutButton.setText(R.string.clockInChange);
		} else if (!timerManager.isTracking()) {
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
			List<Event> events = fetchEventsForDay(monday);
			showTimesForSingleDay(monday, events, mondayIn, mondayOut, mondayWorked, mondayFlexi);
			events = fetchEventsForDay(tuesday);
			showTimesForSingleDay(tuesday, events, tuesdayIn, tuesdayOut, tuesdayWorked, tuesdayFlexi);
			events = fetchEventsForDay(wednesday);
			showTimesForSingleDay(wednesday, events, wednesdayIn, wednesdayOut, wednesdayWorked, wednesdayFlexi);
			events = fetchEventsForDay(thursday);
			showTimesForSingleDay(thursday, events, thursdayIn, thursdayOut, thursdayWorked, thursdayFlexi);
			events = fetchEventsForDay(friday);
			showTimesForSingleDay(friday, events, fridayIn, fridayOut, fridayWorked, fridayFlexi);
			events = fetchEventsForDay(saturday);
			showTimesForSingleDay(saturday, events, saturdayIn, saturdayOut, saturdayWorked, saturdayFlexi);
			events = fetchEventsForDay(sunday);
			showTimesForSingleDay(sunday, events, sundayIn, sundayOut, sundayWorked, sundayFlexi);
		}
	}
	
	private List<Event> fetchEventsForDay(DateTime day) {
		List<Event> ret = dao.getEventsOnDay(day);
		Log.d(getClass().getName(), "fetchEventsForDay: " + DateTimeUtil.dateTimeToDateString(day));
		DateTime now = DateTimeUtil.getCurrentDateTime();
		Event lastEventBeforeNow = dao.getLastEventBefore(now);
		Log.d(getClass().getName(), "lastEventBeforeNow: " + lastEventBeforeNow);
		if (day.isSameDayAs(now) && lastEventBeforeNow != null
			&& lastEventBeforeNow.getType().equals(TypeEnum.CLOCK_IN.getValue())) {
			// currently clocked in: add clock-out event "NOW"
			ret.add(new Event(null, currentlyShownWeek.getId(), null, TypeEnum.CLOCK_OUT_NOW.getValue(), DateTimeUtil
				.dateTimeToString(now), null));
		}
		return ret;
	}
	
	private void showTimesForSingleDay(DateTime day, List<Event> events, TextView in, TextView out, TextView worked,
		TextView flexi) {
		CharSequence timeIn = null;
		CharSequence timeOut = null;
		CharSequence timeWorked = null;
		CharSequence timeFlexi = null;
		
		if (!events.isEmpty()) {
			// take special care of the event type (CLOCK_IN vs. CLOCK_OUT/CLOCK_OUT_NOW)
			Event firstEvent = events.get(0);
			Event lastEvent = events.get(events.size() - 1);
			if (firstEvent.getType().equals(TypeEnum.CLOCK_IN.getValue())) {
				timeIn = DateTimeUtil.dateTimeToHourMinuteString(DateTimeUtil.stringToDateTime(firstEvent.getTime()));
			} else if (firstEvent.getType().equals(TypeEnum.CLOCK_OUT.getValue())
				|| firstEvent.getType().equals(TypeEnum.CLOCK_OUT_NOW.getValue())) {
				timeIn =
					DateTimeUtil.dateTimeToHourMinuteString(DateTimeUtil.stringToDateTime(firstEvent.getTime())
						.getStartOfDay());
			} else {
				throw new IllegalArgumentException("illegal event type");
			}
			if (lastEvent.getType().equals(TypeEnum.CLOCK_OUT.getValue())
				|| lastEvent.getType().equals(TypeEnum.CLOCK_OUT_NOW.getValue())) {
				timeOut = DateTimeUtil.dateTimeToHourMinuteString(DateTimeUtil.stringToDateTime(lastEvent.getTime()));
			} else if (lastEvent.getType().equals(TypeEnum.CLOCK_IN.getValue())) {
				timeOut =
					DateTimeUtil.dateTimeToHourMinuteString(DateTimeUtil.stringToDateTime(lastEvent.getTime())
						.getEndOfDay());
			} else {
				throw new IllegalArgumentException("illegal event type");
			}
			if (lastEvent.getType().equals(TypeEnum.CLOCK_OUT_NOW.getValue())) {
				timeOut = getText(R.string.now);
			}
			
			DateTime amountWorked = calculateWorkedTime(day, events);
			timeWorked = DateTimeUtil.dateTimeToHourMinuteString(amountWorked);
			
			// TODO handle flexi time - use amountWorked
			timeFlexi = "";
			
		} else {
			Event lastEventBeforeToday = dao.getLastEventBefore(day);
			DateTime now = DateTimeUtil.getCurrentDateTime();
			if (lastEventBeforeToday != null && lastEventBeforeToday.getType().equals(TypeEnum.CLOCK_IN.getValue())
				&& day.getStartOfDay().lt(now)) {
				// although there are no events on this day, the user is clocked in all day long - else there would be a
				// CLOCK_OUT_NOW event!
				timeIn = DateTimeUtil.dateTimeToHourMinuteString(day.getStartOfDay());
				timeOut = DateTimeUtil.dateTimeToHourMinuteString(day.getEndOfDay());
				timeWorked = DateTimeUtil.getCompleteDayAsHourMinuteString();
				// TODO handle flexi time
				timeFlexi = "";
			} else {
				// clear all fields
				timeIn = "";
				timeOut = "";
				timeWorked = "";
				timeFlexi = "";
			}
		}
		
		in.setText(timeIn);
		out.setText(timeOut);
		worked.setText(timeWorked);
		flexi.setText(timeFlexi);
	}
	
	/**
	 * Calculate the amount of work time for one day (doesn't work for multiple days).
	 * 
	 * @param events the events on one specific day
	 * @return a DateTime whose time part is set to the amount of work time (the date part has to be ignored)
	 */
	private DateTime calculateWorkedTime(DateTime day, List<Event> events) {
		DateTime ret = DateTimeUtil.getCurrentDateTime().getStartOfDay();
		boolean isFirst = true;
		DateTime clockedInSince = null;
		
		// copy list so the original is not changed externally
		List<Event> internalEvents = new ArrayList<Event>();
		internalEvents.addAll(events);
		if (!internalEvents.isEmpty()
			&& internalEvents.get(internalEvents.size() - 1).getType().equals(TypeEnum.CLOCK_IN.getValue())
			&& !DateTimeUtil.getCurrentDateTime().isSameDayAs(
				DateTimeUtil.stringToDateTime(internalEvents.get(internalEvents.size() - 1).getTime()))) {
			// add clock-out event at midnight to be sure that all time is counted
			Log.d(getClass().getName(), "adding clock-out event at midnight");
			internalEvents.add(new Event(null, null, null, TypeEnum.CLOCK_OUT.getValue(), DateTimeUtil
				.dateTimeToString(day.getEndOfDay()), null));
		}
		
		for (Event event : internalEvents) {
			DateTime eventTime = DateTimeUtil.stringToDateTime(event.getTime());
			Log.d(getClass().getName(), "handling event: " + event.toString());
			
			// clocked in over midnight? => add time since midnight to result
			if (isFirst
				&& (event.getType().equals(TypeEnum.CLOCK_OUT.getValue()) || event.getType().equals(
					TypeEnum.CLOCK_OUT_NOW.getValue()))) {
				Log.d(getClass().getName(), "clocked in over midnight");
				ret = ret.plus(0, 0, 0, eventTime.getHour(), eventTime.getMinute(), 0, DayOverflow.Abort);
			}
			// clock-in event while not clocked in? => remember time
			if (clockedInSince == null && event.getType().equals(TypeEnum.CLOCK_IN.getValue())) {
				Log.d(getClass().getName(), "remembering time");
				clockedInSince = eventTime;
			}
			// clock-out event while clocked in? => add time since last clock-in to result
			if (clockedInSince != null
				&& (event.getType().equals(TypeEnum.CLOCK_OUT.getValue()) || event.getType().equals(
					TypeEnum.CLOCK_OUT_NOW.getValue()))) {
				Log.d(getClass().getName(), "counting time");
				ret = ret.plus(0, 0, 0, eventTime.getHour(), eventTime.getMinute(), 0, DayOverflow.Abort);
				if (eventTime.truncate(Unit.SECOND).equals(day.getEndOfDay().truncate(Unit.SECOND))) {
					// still clocked in at midnight: add 1 minute for the last minute of the day (23:59)
					ret = ret.plus(0, 0, 0, 0, 1, 0, DayOverflow.Abort);
					Log.d(getClass().getName(), "adding one minute");
				}
				ret = ret.minus(0, 0, 0, clockedInSince.getHour(), clockedInSince.getMinute(), 0, DayOverflow.Abort);
				clockedInSince = null;
			}
			
			if (isFirst) {
				isFirst = false;
			}
		}
		return ret;
	}
	
	private void setupTasksAdapter() {
		tasks = dao.getActiveTasks();
		tasksAdapter = new ArrayAdapter<Task>(this, android.R.layout.simple_list_item_1, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		task.setAdapter(tasksAdapter);
	}
	
	private void findAllViewsById() {
		titleRow = (TableRow) findViewById(R.id.titleRow);
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
		menu.add(Menu.NONE, EDIT_TASKS, 0, getString(R.string.edit_tasks)).setIcon(android.R.drawable.ic_menu_edit);
		menu.add(Menu.NONE, OPTIONS, 1, getString(R.string.options)).setIcon(android.R.drawable.ic_menu_preferences);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case EDIT_TASKS:
				showTaskList();
				return true;
			case OPTIONS:
				showOptions();
				return true;
			default:
				Log.w(getClass().getName(), "options menu: unknown item selected");
		}
		return false;
	}
	
	private void showTaskList() {
		Log.d(getClass().getName(), "showing TaskList");
		Intent i = new Intent(this, TaskListActivity.class);
		startActivity(i);
	}
	
	private void showOptions() {
		Log.d(getClass().getName(), "showing Options");
		Intent i = new Intent(this, OptionsActivity.class);
		startActivity(i);
	}
	
	@Override
	protected void onResume() {
		Log.d(getClass().getName(), "onResume called");
		initDaoAndPrefs();
		refreshView();
		super.onResume();
	}
	
	private void initDaoAndPrefs() {
		dao.open();
		if (reloadTasksOnResume) {
			reloadTasksOnResume = false;
			setupTasksAdapter();
		}
		readPreferences();
	}
	
	@Override
	protected void onPause() {
		Log.d(getClass().getName(), "onPause called");
		dao.close();
		super.onPause();
	}
	
	/**
	 * Get the instance of this activity.
	 */
	public static WorkTimeTrackerActivity getInstance() {
		return instance;
	}
	
	/**
	 * Listener for task dropdown field and text field. Triggers a refresh of the main activity.
	 */
	private class TaskAndTextListener implements OnItemSelectedListener, OnKeyListener {
		
		private void valueChanged() {
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
			case SimpleGestureFilter.SWIPE_DOWN:
				// display 4 weeks before
				changeDisplayedWeek(-4);
				break;
			case SimpleGestureFilter.SWIPE_UP:
				// display 4 weeks after
				changeDisplayedWeek(4);
				break;
			default:
				// do nothing
		}
	}
	
	@Override
	public void onDoubleTap() {
		// do nothing
	}
	
}
