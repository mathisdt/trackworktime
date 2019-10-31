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

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.model.WeekDayEnum;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.util.List;

import hirondelle.date4j.DateTime;

/**
 * Activity for managing the events of a week.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class InsertDefaultTimesActivity extends AppCompatActivity {

	private DAO dao = null;
	private TimerManager timerManager = null;

	private Button save = null;
	private Button cancel = null;
	private TextView fromWeekday = null;
	private DatePicker fromDate = null;
	private OnDateChangedListener fromDateListener = null;
	private boolean noFromDateChangedReaction = false;
	private boolean fromPickerIsInitialized = false;
	private TextView toWeekday = null;
	private DatePicker toDate = null;
	private OnDateChangedListener toDateListener = null;
	private boolean noToDateChangedReaction = false;
	private boolean toPickerIsInitialized = false;
	private List<Task> tasks;
	private ArrayAdapter<Task> tasksAdapter;
	private Spinner task = null;
	private EditText text = null;

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

		setContentView(R.layout.default_times);

		save = findViewById(R.id.save);
		cancel = findViewById(R.id.cancel);
		fromWeekday = findViewById(R.id.fromWeekday);
		fromDate = findViewById(R.id.fromDate);
		toWeekday = findViewById(R.id.toWeekday);
		toDate = findViewById(R.id.toDate);
		task = findViewById(R.id.task);
		text = findViewById(R.id.text);

		// bind lists to spinners
		tasks = dao.getActiveTasks();
		tasksAdapter = new ArrayAdapter<>(this, R.layout.list_item_spinner, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		task.setAdapter(tasksAdapter);

		fromDateListener = (view, year, monthOfYear, dayOfMonth) -> {
            if (noFromDateChangedReaction) {
                Logger.debug("from date not changed - infinite loop protection");
            } else {
                DateTime newFromDate = getCurrentlySetFromDate();
                DateTime newToDate = getCurrentlySetToDate();
                try {
                    noFromDateChangedReaction = true;

                    // correct to date if from date would be after to date
                    if (newFromDate.gt(newToDate)) {
                        updateToDatePicker(newFromDate);
                        Toast.makeText(InsertDefaultTimesActivity.this,
                            "adjusted \"to\" date to match \"from\" date (\"to\" cannot be before \"from\")",
                            Toast.LENGTH_LONG).show();
                    }
                } finally {
                    noFromDateChangedReaction = false;
                }

                setFromWeekday();
                Logger.debug("from date changed to {}-{}-{}", year, monthOfYear, dayOfMonth);
            }
        };
		toDateListener = (view, year, monthOfYear, dayOfMonth) -> {
            if (noToDateChangedReaction) {
                Logger.debug("to date not changed - infinite loop protection");
            } else {
                DateTime newFromDate = getCurrentlySetFromDate();
                DateTime newToDate = getCurrentlySetToDate();
                try {
                    noToDateChangedReaction = true;

                    // correct from date if to date would be before from date
                    if (newToDate.lt(newFromDate)) {
                        updateFromDatePicker(newToDate);
                        Toast.makeText(InsertDefaultTimesActivity.this,
                            "adjusted \"from\" date to match \"to\" date (\"from\" cannot be after \"to\")",
                            Toast.LENGTH_LONG).show();
                    }
                } finally {
                    noToDateChangedReaction = false;
                }

                setToWeekday();
                Logger.debug("to date changed to {}-{}-{}", year, monthOfYear, dayOfMonth);
            }
        };

		save.setOnClickListener(v -> {
            // commit all edit fields
            fromDate.clearFocus();
            toDate.clearFocus();
            text.clearFocus();

            // call listener methods manually to make sure that even on buggy Android 5.0 the data is correct
            // => https://code.google.com/p/android/issues/detail?id=78861
            fromDateListener.onDateChanged(fromDate, fromDate.getYear(), fromDate.getMonth(), fromDate
                .getDayOfMonth());
            toDateListener.onDateChanged(toDate, toDate.getYear(), toDate.getMonth(), toDate.getDayOfMonth());

            // fetch the data
            DateTime from = getCurrentlySetFromDate();
            DateTime to = getCurrentlySetToDate();
            Task selectedTask = (Task) task.getSelectedItem();
            Integer taskId = selectedTask == null ? null : selectedTask.getId();
            String textString = text.getText().toString();

            Logger.info("inserting default times from {} to {} with task=\"{}\" and text=\"{}\"", from, to,
                taskId, textString);

            // save the resulting events
            timerManager.insertDefaultWorkTimes(from, to, taskId, textString);

            finish();
        });
		cancel.setOnClickListener(v -> {
            Logger.debug("canceling InsertDefaultTimesActivity");
            finish();
        });
	}

	@Override
	public void onBackPressed() {
		Logger.debug("canceling InsertDefaultTimesActivity (back button pressed)");
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// prepare for entering dates
		DateTime now = DateTimeUtil.getCurrentDateTime();
		updateFromDatePicker(now);
		updateToDatePicker(now);
	}

	private void updateFromDatePicker(DateTime dateTime) {
		if (fromPickerIsInitialized) {
			fromDate.updateDate(dateTime.getYear(), dateTime.getMonth() - 1, dateTime.getDay());
		} else {
			fromDate.init(dateTime.getYear(), dateTime.getMonth() - 1, dateTime.getDay(), fromDateListener);
			fromPickerIsInitialized = true;
		}

		setFromWeekday();
	}

	private void updateToDatePicker(DateTime dateTime) {
		if (toPickerIsInitialized) {
			toDate.updateDate(dateTime.getYear(), dateTime.getMonth() - 1, dateTime.getDay());
		} else {
			toDate.init(dateTime.getYear(), dateTime.getMonth() - 1, dateTime.getDay(), toDateListener);
			toPickerIsInitialized = true;
		}

		setToWeekday();
	}

	private void setFromWeekday() {
		DateTime currentlySelectedFrom = getCurrentlySetFromDate();
		setWeekday(currentlySelectedFrom, fromWeekday);
	}

	private void setToWeekday() {
		DateTime currentlySelectedTo = getCurrentlySetToDate();
		setWeekday(currentlySelectedTo, toWeekday);
	}

	private void setWeekday(DateTime currentlySelected, TextView weekdayView) {
		WeekDayEnum weekDay = WeekDayEnum.getByValue(currentlySelected.getWeekDay());
		switch (weekDay) {
			case MONDAY:
				weekdayView.setText(R.string.monday);
				break;
			case TUESDAY:
				weekdayView.setText(R.string.tuesday);
				break;
			case WEDNESDAY:
				weekdayView.setText(R.string.wednesday);
				break;
			case THURSDAY:
				weekdayView.setText(R.string.thursday);
				break;
			case FRIDAY:
				weekdayView.setText(R.string.friday);
				break;
			case SATURDAY:
				weekdayView.setText(R.string.saturday);
				break;
			case SUNDAY:
				weekdayView.setText(R.string.sunday);
				break;
			default:
				throw new IllegalStateException("unknown weekday");
		}
	}

	private DateTime getCurrentlySetFromDate() {
		return getCurrectlySelectedDate(fromDate.getYear(), fromDate.getMonth(), fromDate.getDayOfMonth());
	}

	private DateTime getCurrentlySetToDate() {
		return getCurrectlySelectedDate(toDate.getYear(), toDate.getMonth(), toDate.getDayOfMonth());
	}

	private DateTime getCurrectlySelectedDate(int year, int month, int day) {
		String datePartString = String.valueOf(year) + "-" + DateTimeUtil.padToTwoDigits(month + 1) + "-"
			+ DateTimeUtil.padToTwoDigits(day);
		DateTime dateTime = new DateTime(datePartString);
		return dateTime;
	}

}
