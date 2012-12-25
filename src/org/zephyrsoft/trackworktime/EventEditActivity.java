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
import hirondelle.date4j.DateTime.DayOverflow;
import java.util.List;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.FlexibleArrayAdapter;
import org.zephyrsoft.trackworktime.util.Logger;
import org.zephyrsoft.trackworktime.util.StringExtractionMethod;
import org.zephyrsoft.trackworktime.util.WeekUtil;

/**
 * Activity for managing the events of a week.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class EventEditActivity extends Activity implements OnDateChangedListener, OnTimeChangedListener {
	
	/** key for the intent extra "week id" */
	public static final String WEEK_ID_EXTRA_KEY = "WEEK_ID_EXTRA_KEY";
	/** key for the intent extra "event id" */
	public static final String EVENT_ID_EXTRA_KEY = "EVENT_ID_EXTRA_KEY";
	
	private DAO dao = null;
	private TimerManager timerManager = null;
	
	private Button save = null;
	private Button cancel = null;
	private List<TypeEnum> types;
	private ArrayAdapter<TypeEnum> typesAdapter;
	private Spinner type = null;
	private TextView weekday = null;
	private DatePicker date = null;
	private int selectedYear = -1;
	private int selectedMonth = -1;
	private int selectedDay = -1;
	private int selectedHour = -1;
	private int selectedMinute = -1;
	private boolean pickersAreInitialized = false;
	private TimePicker time = null;
	private List<Task> tasks;
	private ArrayAdapter<Task> tasksAdapter;
	private Spinner task = null;
	private EditText text = null;
	
	private Week week = null;
	private DateTime weekStart;
	private DateTime weekEnd;
	/** only filled if an existing event is edited! blank for new events! */
	private Event editedEvent = null;
	private boolean newEvent = false;
	
	@Override
	protected void onPause() {
		dao.close();
		super.onPause();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		dao = Basics.getInstance().getDao();
		timerManager = Basics.getInstance().getTimerManager();
		
		setContentView(R.layout.event);
		
		save = (Button) findViewById(R.id.save);
		cancel = (Button) findViewById(R.id.cancel);
		type = (Spinner) findViewById(R.id.type);
		weekday = (TextView) findViewById(R.id.weekday);
		date = (DatePicker) findViewById(R.id.date);
		time = (TimePicker) findViewById(R.id.time);
		task = (Spinner) findViewById(R.id.task);
		text = (EditText) findViewById(R.id.text);
		
		// TODO combine this with the locale setting!
		time.setIs24HourView(Boolean.TRUE);
		
		// bind lists to spinners
		types = TypeEnum.getDefaultTypes();
		typesAdapter =
			new FlexibleArrayAdapter<TypeEnum>(this, android.R.layout.simple_list_item_1, types,
				new StringExtractionMethod<TypeEnum>() {
					@Override
					public String extractText(TypeEnum object) {
						switch (object) {
							case CLOCK_IN:
								return getString(R.string.clockIn);
							case CLOCK_OUT:
								return getString(R.string.clockOut);
							default:
								throw new IllegalArgumentException("unrecognized TypeEnum value");
						}
					}
					
				});
		typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		type.setAdapter(typesAdapter);
		tasks = dao.getActiveTasks();
		tasksAdapter = new ArrayAdapter<Task>(this, android.R.layout.simple_list_item_1, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		task.setAdapter(tasksAdapter);
		
		save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// save the event
				TypeEnum typeEnum = ((TypeEnum) type.getSelectedItem());
				DateTime dateTime = getCurrentlySetDateAndTime();
				String timeString = DateTimeUtil.dateTimeToString(dateTime);
				Integer taskId = ((Task) task.getSelectedItem()).getId();
				String textString = text.getText().toString();
				if (newEvent) {
					timerManager.createEvent(dateTime, taskId, typeEnum, textString);
				} else {
					editedEvent.setType(typeEnum.getValue());
					editedEvent.setTime(timeString);
					editedEvent.setTask(taskId);
					editedEvent.setText(textString);
					dao.updateEvent(editedEvent);
				}
				
				// refresh parents and close the event editor
				EventListActivity.getInstance().refreshView();
				finish();
			}
		});
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	@Override
	public void onBackPressed() {
		finish();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		int eventId = getIntent().getIntExtra(EVENT_ID_EXTRA_KEY, -1);
		int weekId = getIntent().getIntExtra(WEEK_ID_EXTRA_KEY, -1);
		if (eventId != -1) {
			// load event
			editedEvent = dao.getEvent(eventId);
		}
		if (weekId == -1) {
			if (eventId == -1) {
				throw new IllegalArgumentException("event ID must be given when week ID is missing");
			}
			// get week through event
			weekId = editedEvent.getWeek();
		}
		week = dao.getWeek(weekId);
		weekStart = DateTimeUtil.stringToDateTime(week.getStart());
		weekEnd = weekStart.plus(0, 0, 6, 23, 59, 0, DayOverflow.Spillover);
		if (eventId == -1) {
			newEvent = true;
			// prepare for entering a new event: make sure the date is inside the currently selected week
			DateTime now = DateTimeUtil.getCurrentDateTime();
			if (WeekUtil.isDateInWeek(now, week)) {
				updateDateAndTimePickers(now);
			} else {
				updateDateAndTimePickers(weekStart);
			}
		} else {
			newEvent = false;
			// editing an existing event
			for (int i = 0; i < type.getCount(); i++) {
				TypeEnum typeEnum = (TypeEnum) type.getItemAtPosition(i);
				if (typeEnum.getValue() != null && typeEnum.getValue().equals(editedEvent.getType())) {
					type.setSelection(i);
					break;
				}
			}
			DateTime dateTime = DateTimeUtil.stringToDateTime(editedEvent.getTime());
			updateDateAndTimePickers(dateTime);
			for (int i = 0; i < task.getCount(); i++) {
				Task taskItem = (Task) task.getItemAtPosition(i);
				if (taskItem.getId() != null && taskItem.getId().equals(editedEvent.getType())) {
					task.setSelection(i);
					break;
				}
			}
			text.setText(editedEvent.getText());
		}
	}
	
	private void updateDateAndTimePickers(DateTime dateTime) {
		time.setCurrentHour(dateTime.getHour());
		time.setCurrentMinute(dateTime.getMinute());
		if (pickersAreInitialized) {
			date.updateDate(dateTime.getYear(), dateTime.getMonth() - 1, dateTime.getDay());
		} else {
			time.setOnTimeChangedListener(this);
			date.init(dateTime.getYear(), dateTime.getMonth() - 1, dateTime.getDay(), this);
			pickersAreInitialized = true;
			// manually set the variables once:
			selectedHour = dateTime.getHour();
			selectedMinute = dateTime.getMinute();
			selectedYear = dateTime.getYear();
			selectedMonth = dateTime.getMonth() - 1;
			selectedDay = dateTime.getDay();
			setWeekday();
		}
	}
	
	private void setWeekday() {
		DateTime currentlySelected = getCurrentlySetDateAndTime();
		switch (currentlySelected.getWeekDay()) {
			case 1:
				weekday.setText(R.string.sunday);
				break;
			case 2:
				weekday.setText(R.string.monday);
				break;
			case 3:
				weekday.setText(R.string.tuesday);
				break;
			case 4:
				weekday.setText(R.string.wednesday);
				break;
			case 5:
				weekday.setText(R.string.thursday);
				break;
			case 6:
				weekday.setText(R.string.friday);
				break;
			case 7:
				weekday.setText(R.string.saturday);
				break;
			default:
				throw new IllegalStateException("unknown weekday");
		}
	}
	
	@Override
	public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		selectedYear = year;
		selectedMonth = monthOfYear;
		selectedDay = dayOfMonth;
		
		// restrict date range to the week we are editing right now
		DateTime newDate = getCurrentlySetDateAndTime();
		if (newDate.lt(weekStart)) {
			date.updateDate(weekStart.getYear(), weekStart.getMonth() - 1, weekStart.getDay());
		} else if (newDate.gt(weekEnd)) {
			date.updateDate(weekEnd.getYear(), weekEnd.getMonth() - 1, weekEnd.getDay());
		}
		setWeekday();
		Logger.debug("date changed to {0}-{1}-{2}", year, monthOfYear, dayOfMonth);
	}
	
	@Override
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
		selectedHour = hourOfDay;
		selectedMinute = minute;
		Logger.debug("time changed to {0}:{1}", hourOfDay, minute);
	}
	
	private DateTime getCurrentlySetDateAndTime() {
		// DON'T get the numbers directly from the date and time controls, but from the private variables!
		String datePartString =
			String.valueOf(selectedYear) + "-" + DateTimeUtil.padToTwoDigits(selectedMonth + 1) + "-"
				+ DateTimeUtil.padToTwoDigits(selectedDay);
		String timePartString =
			DateTimeUtil.padToTwoDigits(selectedHour) + ":" + DateTimeUtil.padToTwoDigits(selectedMinute) + ":00";
		DateTime dateTime = new DateTime(datePartString + " " + timePartString);
		return dateTime;
	}
	
}
