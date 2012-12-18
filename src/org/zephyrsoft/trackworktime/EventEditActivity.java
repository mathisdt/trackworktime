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
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.FlexibleArrayAdapter;
import org.zephyrsoft.trackworktime.util.StringExtractionMethod;
import org.zephyrsoft.trackworktime.util.WeekUtil;

/**
 * Activity for managing the events of a week.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class EventEditActivity extends Activity {
	
	/** key for the intent extra "week id" */
	public static final String WEEK_ID_EXTRA_KEY = "WEEK_ID_EXTRA_KEY";
	/** key for the intent extra "event id" */
	public static final String EVENT_ID_EXTRA_KEY = "EVENT_ID_EXTRA_KEY";
	
	private DAO dao = null;
	
	private Button save = null;
	private Button cancel = null;
	private List<TypeEnum> types;
	private ArrayAdapter<TypeEnum> typesAdapter;
	private Spinner type = null;
	private DatePicker date = null;
	private TimePicker time = null;
	private List<Task> tasks;
	private ArrayAdapter<Task> tasksAdapter;
	private Spinner task = null;
	private EditText text = null;
	
	private Week week = null;
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
		
		setContentView(R.layout.event);
		
		save = (Button) findViewById(R.id.save);
		cancel = (Button) findViewById(R.id.cancel);
		type = (Spinner) findViewById(R.id.type);
		date = (DatePicker) findViewById(R.id.date);
		time = (TimePicker) findViewById(R.id.time);
		task = (Spinner) findViewById(R.id.task);
		text = (EditText) findViewById(R.id.text);
		
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
				Integer typeId = ((TypeEnum) type.getSelectedItem()).getValue();
				String datePartString =
					String.valueOf(date.getYear()) + "-" + padToTwoDigits(date.getMonth() + 1) + "-"
						+ padToTwoDigits(date.getDayOfMonth());
				String timePartString =
					padToTwoDigits(time.getCurrentHour()) + ":" + padToTwoDigits(time.getCurrentMinute()) + ":00";
				DateTime dateTime = new DateTime(datePartString + " " + timePartString);
				String timeString = DateTimeUtil.dateTimeToString(dateTime);
				Integer taskId = ((Task) task.getSelectedItem()).getId();
				String textString = text.getText().toString();
				if (newEvent) {
					Event eventToCreate = new Event(null, week.getId(), taskId, typeId, timeString, textString);
					dao.insertEvent(eventToCreate);
				} else {
					editedEvent.setType(typeId);
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
	
	private static String padToTwoDigits(int number) {
		if (number < 0) {
			throw new IllegalArgumentException("");
		} else if (number < 10) {
			return "0" + String.valueOf(number);
		} else {
			return String.valueOf(number);
		}
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
		if (eventId == -1) {
			newEvent = true;
			// prepare for entering a new event: make sure the date is inside the currently selected week
			DateTime now = DateTimeUtil.getCurrentDateTime();
			if (WeekUtil.isDateInWeek(now, week)) {
				updateDateAndTimePickers(now);
			} else {
				DateTime weekStart = DateTimeUtil.stringToDateTime(week.getStart());
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
		// TODO restrict date range
//		DateTime weekFirstDay = DateTimeUtil.stringToDateTime(week.getStart());
//		DateTime weekLastDay = weekFirstDay.plusDays(6);
	}
	
	private void updateDateAndTimePickers(DateTime dateTime) {
		date.updateDate(dateTime.getYear(), dateTime.getMonth() - 1, dateTime.getDay());
		time.setCurrentHour(dateTime.getHour());
		time.setCurrentMinute(dateTime.getMinute());
	}
	
}
