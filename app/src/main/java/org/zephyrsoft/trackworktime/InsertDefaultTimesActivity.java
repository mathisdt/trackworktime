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
package org.zephyrsoft.trackworktime;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.pmw.tinylog.Logger;
import org.threeten.bp.LocalDate;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.DefaultTimesBinding;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.timer.TimerManager;

import java.util.List;

/**
 * Activity for managing the events of a week.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class InsertDefaultTimesActivity extends AppCompatActivity {

	private DAO dao = null;
	private TimerManager timerManager = null;

	private DefaultTimesBinding binding;
	private DatePicker fromDate;
	private DatePicker toDate;
	private OnDateChangedListener fromDateListener = null;
	private boolean noFromDateChangedReaction = false;
	private boolean fromPickerIsInitialized = false;
	private OnDateChangedListener toDateListener = null;
	private boolean noToDateChangedReaction = false;
	private boolean toPickerIsInitialized = false;

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

		binding = DefaultTimesBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		fromDate = binding.fromDate;
		toDate = binding.toDate;

		// bind lists to spinners
		List<Task> tasks = dao.getActiveTasks();
		ArrayAdapter<Task> tasksAdapter = new ArrayAdapter<>(this, R.layout.list_item_spinner, tasks);
		tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.task.setAdapter(tasksAdapter);

		fromDateListener = (view, year, monthOfYear, dayOfMonth) -> {
            if (noFromDateChangedReaction) {
                Logger.debug("from date not changed - infinite loop protection");
            } else {
                LocalDate newFromDate = getCurrentlySetFromDate();
                LocalDate newToDate = getCurrentlySetToDate();
                try {
                    noFromDateChangedReaction = true;

                    // correct to date if from date would be after to date
                    if (newFromDate.isAfter(newToDate)) {
                        updateToDatePicker(newFromDate);
                        Toast.makeText(InsertDefaultTimesActivity.this,
                            "adjusted \"to\" date to match \"from\" date (\"to\" cannot be before \"from\")",
                            Toast.LENGTH_LONG).show();
                    }
                } finally {
                    noFromDateChangedReaction = false;
                }

                Logger.debug("from date changed to {}-{}-{}", year, monthOfYear+1, dayOfMonth);
            }
        };
		toDateListener = (view, year, monthOfYear, dayOfMonth) -> {
            if (noToDateChangedReaction) {
                Logger.debug("to date not changed - infinite loop protection");
            } else {
                LocalDate newFromDate = getCurrentlySetFromDate();
                LocalDate newToDate = getCurrentlySetToDate();
                try {
                    noToDateChangedReaction = true;

                    // correct from date if to date would be before from date
                    if (newToDate.isBefore(newFromDate)) {
                        updateFromDatePicker(newToDate);
                        Toast.makeText(InsertDefaultTimesActivity.this,
                            "adjusted \"from\" date to match \"to\" date (\"from\" cannot be after \"to\")",
                            Toast.LENGTH_LONG).show();
                    }
                } finally {
                    noToDateChangedReaction = false;
                }

                Logger.debug("to date changed to {}-{}-{}", year, monthOfYear+1, dayOfMonth);
            }
        };

		binding.save.setOnClickListener(v -> {
            // commit all edit fields
            fromDate.clearFocus();
            toDate.clearFocus();
            binding.text.clearFocus();

            // call listener methods manually to make sure that even on buggy Android 5.0 the data is correct
            // => https://code.google.com/p/android/issues/detail?id=78861
            fromDateListener.onDateChanged(fromDate, fromDate.getYear(), fromDate.getMonth(), fromDate
                .getDayOfMonth());
            toDateListener.onDateChanged(toDate, toDate.getYear(), toDate.getMonth(), toDate.getDayOfMonth());

            // fetch the data
            LocalDate from = getCurrentlySetFromDate();
            LocalDate to = getCurrentlySetToDate();
            Task selectedTask = (Task) binding.task.getSelectedItem();
            Integer taskId = selectedTask == null ? null : selectedTask.getId();
            String textString = binding.text.getText().toString();

            Logger.info("inserting default times from {} to {} with task=\"{}\" and text=\"{}\"", from, to,
                taskId, textString);

            // save the resulting events
            timerManager.insertDefaultWorkTimes(from, to, taskId, textString);

            finish();
        });
		binding.cancel.setOnClickListener(v -> {
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
		LocalDate now = LocalDate.now();
		updateFromDatePicker(now);
		updateToDatePicker(now);
	}

	private void updateFromDatePicker(LocalDate date) {
		if (fromPickerIsInitialized) {
			fromDate.updateDate(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth());
		} else {
			fromDate.init(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth(), fromDateListener);
			fromPickerIsInitialized = true;
		}
	}

	private void updateToDatePicker(LocalDate dateTime) {
		if (toPickerIsInitialized) {
			toDate.updateDate(dateTime.getYear(), dateTime.getMonthValue()-1, dateTime.getDayOfMonth());
		} else {
			toDate.init(dateTime.getYear(), dateTime.getMonthValue()-1, dateTime.getDayOfMonth(), toDateListener);
			toPickerIsInitialized = true;
		}
	}

	private LocalDate getCurrentlySetFromDate() {
		return getCurrentlySelectedDate(fromDate.getYear(), fromDate.getMonth(), fromDate.getDayOfMonth());
	}

	private LocalDate getCurrentlySetToDate() {
		return getCurrentlySelectedDate(toDate.getYear(), toDate.getMonth(), toDate.getDayOfMonth());
	}

	private LocalDate getCurrentlySelectedDate(int year, int month, int day) {
		return LocalDate.of(year, month + 1, day);
	}

}
