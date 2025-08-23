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

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.Constants;
import org.zephyrsoft.trackworktime.EventListActivity;
import org.zephyrsoft.trackworktime.R;
import org.zephyrsoft.trackworktime.WorkTimeTrackerActivity;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.EventBinding;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.model.Week;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.ui.DateTextViewController;
import org.zephyrsoft.trackworktime.util.BroadcastUtil;
import org.zephyrsoft.trackworktime.util.ThemeUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Activity for managing the events of a week.
 */
public class EventEditActivity extends AppCompatActivity {

	private DAO dao = null;
	private TimerManager timerManager = null;

	private Spinner task = null;
	private EditText text = null;
	private EventBinding binding;
	private boolean pickersAreInitialized = false;
	private List<Task> tasks;
	private ArrayAdapter<Task> tasksAdapter;

	/** saved here so the resume can access the original value given via intent */
	private long epochDay = -1;
	/** saved here so the resume can access the original value given via intent */
	private int eventId = -1;

	private Week week = null;
	/** only filled if an existing event is edited! blank for new events! */
	private Event editedEvent = null;
	private boolean newEvent = false;
	private boolean period = false;

	private TimeTextViewController timeTextViewController;
	private TimeTextViewController endTextViewController;
	private DateTextViewController dateTextViewController;

	@Override
	protected void onPause() {
		dao.close();
		super.onPause();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		dao = Basics.get(this).getDao();
		timerManager = Basics.get(this).getTimerManager();

		binding = EventBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ThemeUtil.styleActionBar(this, getSupportActionBar());

		timeTextViewController = new TimeTextViewController(binding.time);
		endTextViewController = new TimeTextViewController(binding.end);
		dateTextViewController = new DateTextViewController(binding.date);

		task = binding.task;
		text = binding.text;

		binding.radioClockIn.setOnCheckedChangeListener((buttonView, isChecked) -> setTaskAndTextVisible(isChecked));
		tasks = dao.getActiveTasks();
		tasksAdapter = new ArrayAdapter<>(this, R.layout.list_item_spinner, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		task.setAdapter(tasksAdapter);

		timeTextViewController.setListener(lt -> {
			if (period && lt.isAfter(LocalTime.of(23, 58))) {
				timeTextViewController.setTime(LocalTime.of(23, 58));
			} else if (period && endTextViewController.getTime() != null
				&& !endTextViewController.getTime().isAfter(lt)) {
				endTextViewController.setTime(lt.plusMinutes(1));
			}
		});
		endTextViewController.setListener(lt -> {
			if (period && lt.isBefore(LocalTime.of(0, 1))) {
				endTextViewController.setTime(LocalTime.of(0, 1));
			} else if (period && timeTextViewController.getTime() != null
				&& !timeTextViewController.getTime().isBefore(lt)) {
				timeTextViewController.setTime(lt.minusMinutes(1));
			}
		});

		binding.save.setOnClickListener(v -> {
            // commit all edit fields
            text.clearFocus();

			if (period) {
				OffsetDateTime startDateTime = getCurrentlySetDateTime();
				OffsetDateTime endDateTime = getCurrentlySetEndDateTime();
				if (startDateTime == null || endDateTime == null) {
					showMsgDateTimeNotSelected();
					return;
				}

				Task selectedTask = (Task) task.getSelectedItem();
				if (selectedTask == null) {
					showMsgTaskNotSelected();
					return;
				}
				Integer taskId = selectedTask.getId();
				String textString = text.getText().toString();

				Logger.debug("saving new period: {} - {}", startDateTime, endDateTime);
				timerManager.createEvent(startDateTime, taskId, TypeEnum.CLOCK_IN, textString, TimerManager.EventOrigin.EVENT_LIST);
				timerManager.createEvent(endDateTime, null, TypeEnum.CLOCK_OUT, null, TimerManager.EventOrigin.EVENT_LIST);
			} else {
				// save the event
				TypeEnum typeEnum = binding.radioClockIn.isChecked() ? TypeEnum.CLOCK_IN : TypeEnum.CLOCK_OUT;

				OffsetDateTime dateTime = getCurrentlySetDateTime();
				if (dateTime == null) {
					showMsgDateTimeNotSelected();
					return;
				}

				Task selectedTask = (Task) task.getSelectedItem();
				if (typeEnum == TypeEnum.CLOCK_IN && selectedTask == null) {
					showMsgTaskNotSelected();
					return;
				}
				Integer taskId = (typeEnum != TypeEnum.CLOCK_IN ? null : selectedTask.getId());
				String textString = (typeEnum != TypeEnum.CLOCK_IN ? null : text.getText().toString());

				if (newEvent) {
					Logger.debug("saving new event: {} @ {}", typeEnum.name(), dateTime);
					timerManager.createEvent(dateTime, taskId, typeEnum, textString, TimerManager.EventOrigin.EVENT_LIST);
				} else {
					Logger.debug("saving changed event with ID {}: {} @ {}",
						editedEvent.getId(), typeEnum.name(), dateTime);
					editedEvent.setType(typeEnum.getValue());
					editedEvent.setDateTime(dateTime);
					editedEvent.setTask(taskId);
					editedEvent.setText(textString);
					dao.updateEvent(editedEvent);

					// we have to call this manually when using the DAO directly
					timerManager.invalidateCacheFrom(dateTime);

					BroadcastUtil.sendEventBroadcast(editedEvent, this,
						BroadcastUtil.Action.UPDATED, TimerManager.EventOrigin.EVENT_LIST);
				}
			}

			Basics.get(this).safeCheckExternalControls();

			// refresh parents and close the event editor
			if (EventListActivity.getInstance() != null) {
				EventListActivity.getInstance().refreshView();
			}
			finish();
		});

		binding.cancel.setOnClickListener(v -> {
			Logger.debug("canceling EventEditActivity");
			finish();
		});
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			default:
				throw new IllegalArgumentException("options menu: unknown item selected");
		}
	}

	private void setTaskAndTextVisible(boolean visible) {
		int visibility = visible ? View.VISIBLE : View.GONE;
		binding.taskLayout.setVisibility(visibility);
		binding.textLayout.setVisibility(visibility);
	}

	@Override
	public void onBackPressed() {
		Logger.debug("canceling EventEditActivity (back button pressed)");
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();

		period = getIntent().getBooleanExtra(Constants.PERIOD_EXTRA_KEY, false);
		if (period) {
			setTitle(R.string.editPeriod);
			binding.timeLabel.setText(R.string.start);
			binding.typeLabel.setVisibility(View.GONE);
			binding.radioType.setVisibility(View.GONE);
			binding.endLabel.setVisibility(View.VISIBLE);
			binding.end.setVisibility(View.VISIBLE);
		} else {
			binding.typeLabel.setVisibility(View.VISIBLE);
			binding.radioType.setVisibility(View.VISIBLE);
			binding.endLabel.setVisibility(View.GONE);
			binding.end.setVisibility(View.GONE);
		}

		// one of the following options
		if (getIntent().hasExtra(Constants.EVENT_ID_EXTRA_KEY)
			&& getIntent().hasExtra(Constants.WEEK_START_EXTRA_KEY)) {
			Logger.warn("both an event ID and a week start were given - event ID will win");
		}
		if (getIntent().hasExtra(Constants.EVENT_ID_EXTRA_KEY)) {
			eventId = getIntent().getIntExtra(Constants.EVENT_ID_EXTRA_KEY, -1);
			epochDay = -1;
		} else if (getIntent().hasExtra(Constants.WEEK_START_EXTRA_KEY)) {
			eventId = -1;
			epochDay = getIntent().getLongExtra(Constants.WEEK_START_EXTRA_KEY, -1);
		}

		if (eventId == -1 && epochDay == -1) {
			Logger.debug("we don't know which event or even which week is meant, return to main screen");
			Intent i = new Intent(this, WorkTimeTrackerActivity.class);
			startActivity(i);
			finish();
			// we don't want to run the rest of this method because it wouldn't do anything sensible
			return;
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
			if (week.isInWeek(LocalDate.now())) {
				updateDateAndTimePickers(ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES));
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
		LocalTime time = dateTime.toLocalTime();
		updateTimePicker(time);
		updateDatePicker(dateTime.toLocalDate());

		if (!pickersAreInitialized) {
			initDatePicker();
			if (period) {
				updateEndPicker(time.plusMinutes(1));
			}
			pickersAreInitialized = true;
		}
	}

	private void updateTimePicker(LocalTime localTime) {
		timeTextViewController.setTime(localTime);
	}

	private void updateEndPicker(LocalTime localTime) {
		endTextViewController.setTime(localTime);
	}

	private void updateDatePicker(LocalDate date) {
		dateTextViewController.setDate(date);
	}

	private void initDatePicker() {
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

	@Nullable
	private OffsetDateTime getCurrentlySetDateTime() {
		LocalTime time = timeTextViewController.getTime();
		if (time == null) {
			return null;
		}

		LocalDate date = dateTextViewController.getDate();
		if (date == null) {
			return null;
		}

		return date.atTime(time)
				.atZone(getSelectedZone())
				.toOffsetDateTime();
	}

	@Nullable
	private OffsetDateTime getCurrentlySetEndDateTime() {
		LocalTime time = endTextViewController.getTime();
		if (time == null) {
			return null;
		}

		LocalDate date = dateTextViewController.getDate();
		if (date == null) {
			return null;
		}

		return date.atTime(time)
				.atZone(getSelectedZone())
				.toOffsetDateTime();
	}

	private ZoneId getSelectedZone() {
		return binding.timeZonePicker.getZoneId();
	}

	private void showMsgDateTimeNotSelected() {
		Toast.makeText(this, R.string.errorDateOrTimeNotSelected, Toast.LENGTH_LONG).show();
	}

	private void showMsgTaskNotSelected() {
		Toast.makeText(this, R.string.errorTaskNotSelected, Toast.LENGTH_LONG).show();
	}

}
