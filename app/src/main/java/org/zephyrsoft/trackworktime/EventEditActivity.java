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

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import android.widget.Toast;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.model.WeekDayEnum;
import org.zephyrsoft.trackworktime.model.WeekPlaceholder;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.WeekUtil;

import java.util.List;

import hirondelle.date4j.DateTime;
import hirondelle.date4j.DateTime.DayOverflow;

/**
 * Activity for managing the events of a week.
 *
 * @author Mathis Dirksen-Thedens
 */
public class EventEditActivity extends AppCompatActivity implements OnDateChangedListener, OnTimeChangedListener {

	private DAO dao = null;
	private TimerManager timerManager = null;

	private Button save = null;
	private Button cancel = null;
	private ArrayAdapter<TypeEnum> typesAdapter;
	private RadioGroup type;
	private RadioButton clockIn;
	private RadioButton clockOut;
	private RadioButton flexTime;
	private ImageButton infoButton;
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

	private boolean noDateChangedReaction = false;

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

		save = findViewById(R.id.save);
		cancel = findViewById(R.id.cancel);
		type = findViewById(R.id.radioType);
		clockIn = findViewById(R.id.radioClockIn);
		clockOut = findViewById(R.id.radioClockOut);
		flexTime = findViewById(R.id.radioFlexTime);
		infoButton = findViewById(R.id.typeInfoButton);
		date = findViewById(R.id.date);
		time = findViewById(R.id.time);
		task = findViewById(R.id.task);
		text = findViewById(R.id.text);

		infoButton.setOnClickListener(v -> new AlertDialog.Builder(v.getContext())
			.setMessage(R.string.eventTypeInfoMessage)
			.show());

		// TODO combine this with the locale setting!
		time.setIs24HourView(Boolean.TRUE);

		type.setOnCheckedChangeListener((buttonView, isChecked) -> {
			task.setEnabled(isChecked == R.id.radioClockIn);
			text.setEnabled(isChecked == R.id.radioClockIn || isChecked == R.id.radioFlexTime);
		});
		tasks = dao.getActiveTasks();
		tasksAdapter = new ArrayAdapter<>(this, R.layout.list_item_spinner, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		task.setAdapter(tasksAdapter);

		save.setOnClickListener(v -> {
            // commit all edit fields
            date.clearFocus();
            time.clearFocus();
            text.clearFocus();

            // call listener methods manually to make sure that even on buggy Android 5.0 the data is correct
            // => https://code.google.com/p/android/issues/detail?id=78861
            onDateChanged(date, date.getYear(), date.getMonth(), date.getDayOfMonth());
            onTimeChanged(time, time.getCurrentHour(), time.getCurrentMinute());

            // save the event
            TypeEnum typeEnum = clockIn.isChecked() ? TypeEnum.CLOCK_IN : clockOut.isChecked() ? TypeEnum.CLOCK_OUT: TypeEnum.FLEX;
            DateTime dateTime = getCurrentlySetDateAndTime();
            String timeString = DateTimeUtil.dateTimeToString(dateTime);
            Task selectedTask = (Task) task.getSelectedItem();
            Integer taskId = ((typeEnum == TypeEnum.CLOCK_OUT || selectedTask == null) ? null :
                selectedTask.getId());
            String textString = (typeEnum == TypeEnum.CLOCK_OUT ? null : text.getText().toString());
            if (newEvent) {
                Logger.debug("saving new event: {} @ {}", typeEnum.name(), timeString);
                timerManager.createEvent(dateTime, taskId, typeEnum, textString);
            } else {
                Logger.debug("saving changed event with ID {}: {} @ {}", editedEvent.getId(), typeEnum.name(),
                    timeString);
                editedEvent.setType(typeEnum.getValue());
                editedEvent.setTime(timeString);
                editedEvent.setTask(taskId);
                editedEvent.setText(textString);
                dao.updateEvent(editedEvent);
                // we have to call this manually when using the DAO directly:
                timerManager.updateWeekSum(week);
                Basics.getInstance().safeCheckPersistentNotification();
            }

            // refresh parents and close the event editor
            EventListActivity.getInstance().refreshView();
            finish();
        });
		cancel.setOnClickListener(v -> {
            Logger.debug("canceling EventEditActivity");
            finish();
        });
	}

	@Override
	public void onBackPressed() {
		Logger.debug("canceling EventEditActivity (back button pressed)");
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// one of the following options
		int eventId = getIntent().getIntExtra(Constants.EVENT_ID_EXTRA_KEY, -1);
		String weekStartString = getIntent().getStringExtra(Constants.WEEK_START_EXTRA_KEY);

		if (eventId == -1 && weekStartString == null) {
			throw new IllegalArgumentException("either event ID or week start must be given");
		} else if (eventId != -1) {
			editedEvent = dao.getEvent(eventId);
			weekStartString = DateTimeUtil.getWeekStartAsString(DateTimeUtil.stringToDateTime(editedEvent.getTime()));
		}
		weekStart = DateTimeUtil.stringToDateTime(weekStartString);
		week = dao.getWeek(DateTimeUtil.dateTimeToString(weekStart));
		if (week == null) {
			week = new WeekPlaceholder(weekStartString);
		}
		weekEnd = weekStart.plus(0, 0, 6, 23, 59, 0, 0, DayOverflow.Spillover);
		if (eventId == -1) {
			newEvent = true;
			// prepare for entering a new event: make sure the date is inside the currently selected week
			DateTime now = DateTimeUtil.getCurrentDateTime();
			if (WeekUtil.isDateInWeek(now, week)) {
				updateDateAndTimePickers(now);
			} else {
				updateDateAndTimePickers(weekStart);
			}
			Task defaultTask = dao.getDefaultTask();
			if (defaultTask != null) {
				for (int i = 0; i < task.getCount(); i++) {
					Task taskItem = (Task) task.getItemAtPosition(i);
					if (taskItem != null && taskItem.getId() != null && taskItem.getId().equals(defaultTask.getId())) {
						task.setSelection(i);
						break;
					}
				}
			}
		} else {
			newEvent = false;
			clockIn.setChecked(TypeEnum.CLOCK_IN.getValue().equals(editedEvent.getType()));
			clockOut.setChecked(TypeEnum.CLOCK_OUT.getValue().equals(editedEvent.getType()));
			flexTime.setChecked(TypeEnum.FLEX.getValue().equals(editedEvent.getType()));
			DateTime dateTime = DateTimeUtil.stringToDateTime(editedEvent.getTime());
			updateDateAndTimePickers(dateTime);
			for (int i = 0; i < task.getCount(); i++) {
				Task taskItem = (Task) task.getItemAtPosition(i);
				if (taskItem != null && taskItem.getId() != null && taskItem.getId().equals(editedEvent.getTask())) {
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
		}
	}

	@Override
	public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		if (noDateChangedReaction) {
			Logger.debug("date not changed - infinite loop protection");
		} else {
			selectedYear = year;
			selectedMonth = monthOfYear;
			selectedDay = dayOfMonth;

			// restrict date range to the week we are editing right now
			DateTime newDate = getCurrentlySetDateAndTime();
			try {
				noDateChangedReaction = true;
				if (newDate.lt(weekStart)) {
					date.updateDate(weekStart.getYear(), weekStart.getMonth() - 1, weekStart.getDay());
					selectedYear = weekStart.getYear();
					selectedMonth = weekStart.getMonth() - 1;
					selectedDay = weekStart.getDay();
					Toast.makeText(this,
						"adjusted date to match first day of week - the event has to stay in the current week",
						Toast.LENGTH_LONG).show();
				} else if (newDate.gt(weekEnd)) {
					date.updateDate(weekEnd.getYear(), weekEnd.getMonth() - 1, weekEnd.getDay());
					selectedYear = weekEnd.getYear();
					selectedMonth = weekEnd.getMonth() - 1;
					selectedDay = weekEnd.getDay();
					Toast.makeText(this,
						"adjusted date to match last day of week - the event has to stay in the current week",
						Toast.LENGTH_LONG).show();
				}
			} finally {
				noDateChangedReaction = false;
			}

			Logger.debug("date changed to {}-{}-{}", year, monthOfYear, dayOfMonth);
		}
	}

	@Override
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
		selectedHour = hourOfDay;
		selectedMinute = minute;
		Logger.debug("time changed to {}:{}", hourOfDay, minute);
	}

	private DateTime getCurrentlySetDateAndTime() {
		// DON'T get the numbers directly from the date and time controls, but from the private variables!
		String datePartString = String.valueOf(selectedYear) + "-" + DateTimeUtil.padToTwoDigits(selectedMonth + 1)
			+ "-" + DateTimeUtil.padToTwoDigits(selectedDay);
		String timePartString = DateTimeUtil.padToTwoDigits(selectedHour) + ":"
			+ DateTimeUtil.padToTwoDigits(selectedMinute) + ":00";
		DateTime dateTime = new DateTime(datePartString + " " + timePartString);
		return dateTime;
	}

}
