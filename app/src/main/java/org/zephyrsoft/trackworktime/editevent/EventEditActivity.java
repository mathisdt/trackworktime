/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License 3.0 as published by
 * the Free Software Foundation.
 *
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License 3.0 for more details.
 *
 * You should have received a copy of the GNU General Public License 3.0
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime.editevent;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import androidx.appcompat.app.AppCompatActivity;

import org.pmw.tinylog.Logger;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.Constants;
import org.zephyrsoft.trackworktime.EventListActivity;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.EventBinding;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.timer.TimerManager;

import java.util.List;

/**
 * Activity for managing the events of a week.
 */
public class EventEditActivity extends AppCompatActivity implements OnTimeChangedListener {

	private DAO dao = null;
	private TimerManager timerManager = null;

	private TimePicker time = null;
	private Spinner task = null;
	private EditText text = null;
	private EventBinding binding;
	private int selectedHour = -1;
	private int selectedMinute = -1;
	private boolean pickersAreInitialized = false;
	private List<Task> tasks;
	private ArrayAdapter<Task> tasksAdapter;

	private Week week = null;
	/** only filled if an existing event is edited! blank for new events! */
	private Event editedEvent = null;
	private boolean newEvent = false;

	private DateTextViewController dateTextViewController;

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

		binding = EventBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		dateTextViewController = new DateTextViewController(binding.date);

		time = binding.time;
		task = binding.task;
		text = binding.text;

		// TODO combine this with the locale setting!
		time.setIs24HourView(Boolean.TRUE);

		binding.radioClockIn.setOnCheckedChangeListener((buttonView, isChecked) -> setTaskAndTextEditable(isChecked));
		tasks = dao.getActiveTasks();
		tasksAdapter = new ArrayAdapter<>(this, R.layout.list_item_spinner, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		task.setAdapter(tasksAdapter);

		binding.save.setOnClickListener(v -> {
            // commit all edit fields
            time.clearFocus();
            text.clearFocus();

            // call listener methods manually to make sure that even on buggy Android 5.0 the data is correct
            // => https://code.google.com/p/android/issues/detail?id=78861
            onTimeChanged(time, time.getCurrentHour(), time.getCurrentMinute());

			// save the event
			TypeEnum typeEnum = binding.radioClockIn.isChecked() ? TypeEnum.CLOCK_IN : TypeEnum.CLOCK_OUT;

			OffsetDateTime dateTime = getCurrentlySetDateTime();
			Task selectedTask = (Task) task.getSelectedItem();
			Integer taskId = ((typeEnum == TypeEnum.CLOCK_OUT || selectedTask == null) ? null :
					selectedTask.getId());
			String textString = (typeEnum == TypeEnum.CLOCK_OUT ? null : text.getText().toString());

			if (newEvent) {
				Logger.debug("saving new event: {} @ {}", typeEnum.name(), dateTime);
				timerManager.createEvent(dateTime, taskId, typeEnum, textString);
			} else {
				Logger.debug("saving changed event with ID {}: {} @ {}", editedEvent.getId(), typeEnum.name(),
						dateTime);
				editedEvent.setType(typeEnum.getValue());
				editedEvent.setDateTime(dateTime);
				editedEvent.setTask(taskId);
				editedEvent.setText(textString);
				dao.updateEvent(editedEvent);

				// we have to call this manually when using the DAO directly
				timerManager.invalidateCacheFrom(dateTime);

				Basics.getInstance().safeCheckExternalControls();
			}

			// refresh parents and close the event editor
			EventListActivity.getInstance().refreshView();
			finish();
		});

		binding.cancel.setOnClickListener(v -> {
			Logger.debug("canceling EventEditActivity");
			finish();
		});
	}

	private void setTaskAndTextEditable(boolean shouldBeEditable) {
		task.setEnabled(shouldBeEditable);
		text.setEnabled(shouldBeEditable);
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
		long epochDay = getIntent().getLongExtra(Constants.WEEK_START_EXTRA_KEY, -1);

		if (eventId == -1 && epochDay == -1) {
			throw new IllegalArgumentException("Either event ID or week start must be given");
		} else if (eventId != -1) {
			editedEvent = dao.getEvent(eventId);

			// week in the zone of the edited event
			week = new Week(editedEvent.getDateTime().toLocalDate());
		} else {
			week = new Week(epochDay);
		}

		if (eventId == -1) {
			newEvent = true;
			// prepare for entering a new event: make sure the date is inside the currently selected week
			// TODO why this restriction?

			if (week.isInWeek(LocalDate.now())) {
				updateDateAndTimePickers(ZonedDateTime.now());
			} else {
				// assume home time zone
				updateDateAndTimePickers(week.getStart().atStartOfDay(timerManager.getHomeTimeZone()));
			}

			Task defaultTask = dao.getDefaultTask();
			if (defaultTask != null) {
				updateSelectedTask(defaultTask.getId());
			}

		} else {
			newEvent = false;
			binding.radioClockIn.setChecked(TypeEnum.CLOCK_IN.getValue().equals(editedEvent.getType()));
			binding.radioClockOut.setChecked(TypeEnum.CLOCK_OUT.getValue().equals(editedEvent.getType()));

			updateDateAndTimePickers(editedEvent.getDateTime());

			updateSelectedTask(editedEvent.getTask());

			text.setText(editedEvent.getText());
		}
	}

	private void updateSelectedTask(Integer taskId) {
		for (int i = 0; i < task.getCount(); i++) {
			Task taskItem = (Task) task.getItemAtPosition(i);
			if (taskItem != null && taskItem.getId() != null && taskItem.getId().equals(taskId)) {
				task.setSelection(i);
				break;
			}
		}
	}

	private void updateDateAndTimePickers(LocalDateTime dateTime) {
		time.setCurrentHour(dateTime.getHour());
		time.setCurrentMinute(dateTime.getMinute());
		if (!pickersAreInitialized) {
			time.setOnTimeChangedListener(this);
			pickersAreInitialized = true;
			// manually set the variables once:
			selectedHour = dateTime.getHour();
			selectedMinute = dateTime.getMinute();
		}
		updateDatePicker(dateTime.toLocalDate());
	}

	private void updateDatePicker(LocalDate date) {
		dateTextViewController.setDate(date);

		if (pickersAreInitialized) {
			return;
		}

		ZoneId zone = getSelectedZone();
		dateTextViewController.setDateLimits(
				week.getStart().atStartOfDay(zone),
				week.getEnd().atStartOfDay(zone)
		);
	}

	private void updateDateAndTimePickers(ZonedDateTime dateTime) {
		updateDateAndTimePickers(dateTime.toLocalDateTime());
		binding.timeZonePicker.setZoneId(dateTime.getZone());
	}

	private void updateDateAndTimePickers(OffsetDateTime dateTime) {
		updateDateAndTimePickers(dateTime.toLocalDateTime());
		binding.timeZonePicker.setZoneIdFromOffset(dateTime.getOffset());
	}

	@Override
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
		selectedHour = hourOfDay;
		selectedMinute = minute;
		Logger.debug("time changed to {}:{}", hourOfDay, minute);
	}

	private OffsetDateTime getCurrentlySetDateTime() {
		// DON'T get the numbers directly from the time picker, but from the variables!
		LocalDate date = dateTextViewController.getDate();
		return date.atTime(selectedHour, selectedMinute)
				.atZone(getSelectedZone())
				.toOffsetDateTime();
	}

	private ZoneId getSelectedZone() {
		return binding.timeZonePicker.getZoneId();
	}

}
