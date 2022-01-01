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
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;

import org.pmw.tinylog.Logger;
import org.threeten.bp.LocalDate;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.DefaultTimesBinding;
import org.zephyrsoft.trackworktime.model.Target;
import org.zephyrsoft.trackworktime.model.TargetEnum;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.ui.DateTextViewController;
import org.zephyrsoft.trackworktime.ui.TargetTimeValidityCheck;

import java.util.List;

/**
 * Activity for managing the events of a week.
 */
public class InsertDefaultTimesActivity extends AppCompatActivity {

    private DAO dao = null;
    private TimerManager timerManager = null;

    private DefaultTimesBinding binding;
    private DateTextViewController dateToTextViewController;
    private DateTextViewController dateFromTextViewController;

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

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // bind lists to spinners
        List<Task> tasks = dao.getActiveTasks();
        ArrayAdapter<Task> tasksAdapter = new ArrayAdapter<>(this, R.layout.list_item_spinner, tasks);
        tasksAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.task.setAdapter(tasksAdapter);

        Consumer<LocalDate> fromDateListener = newDate -> {
            // correct to date if from date would be after to date
            if (dateToTextViewController.getDate() != null
                && newDate.isAfter(dateToTextViewController.getDate())) {
                updateToDate(newDate);
                Toast.makeText(InsertDefaultTimesActivity.this,
                    "adjusted \"to\" date to match \"from\" date (\"to\" cannot be before \"from\")",
                    Toast.LENGTH_LONG).show();
            }

            Logger.debug("toDate changed to {}", newDate);
        };
        dateFromTextViewController = new DateTextViewController(binding.dateFrom, fromDateListener);

        Consumer<LocalDate> toDateListener = newDate -> {
            // correct from date if to date would be before from date
            if (dateFromTextViewController.getDate() != null
                && newDate.isBefore(dateFromTextViewController.getDate())) {
                updateFromDate(newDate);
                Toast.makeText(InsertDefaultTimesActivity.this,
                    "adjusted \"from\" date to match \"to\" date (\"from\" cannot be after \"to\")",
                    Toast.LENGTH_LONG).show();
            }

            Logger.debug("fromDate changed to {}", newDate);
        };
        dateToTextViewController = new DateTextViewController(binding.dateTo, toDateListener);

        TargetTimeValidityCheck targetTimeValidityCheck =
            new TargetTimeValidityCheck(binding.targetTime, isValid -> binding.save.setEnabled(isValid));
        binding.targetTime.addTextChangedListener(targetTimeValidityCheck);

        binding.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isTask = checkedId == R.id.radioTask;
            boolean isChangeTargetTime = checkedId == R.id.radioChangeTargetTime;

            binding.task.setEnabled(isTask);
            binding.targetTime.setEnabled(isChangeTargetTime);
            binding.checkAlsoOnNonWorkingDays.setEnabled(!isTask);
            if (isTask) {
                binding.checkAlsoOnNonWorkingDays.setChecked(false);
            }
            if (isChangeTargetTime) {
                targetTimeValidityCheck.check();
            } else {
                binding.targetTime.setText(null);
            }
        });

        binding.save.setOnClickListener(v -> {
            // commit all edit fields
            binding.text.clearFocus();
            binding.targetTime.clearFocus();

            // fetch the data
            Task selectedTask = (Task) binding.task.getSelectedItem();
            Integer taskId = selectedTask == null ? null : selectedTask.getId();
            String textString = binding.text.getText().toString();
            LocalDate fromDate = dateFromTextViewController.getDate();
            LocalDate toDate = dateToTextViewController.getDate();
            String targetTime = binding.targetTime.getText() == null
                ? null
                : binding.targetTime.getText().toString();
            boolean alsoOnNonWorkingDays = binding.checkAlsoOnNonWorkingDays.isChecked();

            boolean success = false;
            final int selectedInsertType = binding.radioGroup.getCheckedRadioButtonId();
            if (selectedInsertType == R.id.radioTask) {
                Logger.info("inserting default times from {} to {} with task=\"{}\" and text=\"{}\"", fromDate, toDate,
                    taskId, textString);
                success = timerManager.insertDefaultWorkTimes(fromDate, toDate, taskId, textString);
            } else if (selectedInsertType == R.id.radioHolidayVacationNonWorkingDay) {
                Logger.info("setting holiday/vacation/non-working-day from {} to {} with text=\"{}\" and alsoOnNonWorkingDays={}",
                    fromDate, toDate, textString, alsoOnNonWorkingDays);
                success = setTarget(fromDate, toDate, TargetEnum.DAY_SET, textString, alsoOnNonWorkingDays, "0:00");
            } else if (selectedInsertType == R.id.radioWorkingTimeToTargetTime) {
                Logger.info("setting working time to target time from {} to {} with text=\"{}\" and alsoOnNonWorkingDays={}",
                    fromDate, toDate, textString, alsoOnNonWorkingDays);
                success = setTarget(fromDate, toDate, TargetEnum.DAY_GRANT, textString, alsoOnNonWorkingDays, "0:00");
            } else if (selectedInsertType == R.id.radioChangeTargetTime) {
                Logger.info("setting change target time from {} to {} with text=\"{}\" and alsoOnNonWorkingDays={} and targetTime={}",
                    fromDate, toDate, textString, alsoOnNonWorkingDays, targetTime);
                success = setTarget(fromDate, toDate, TargetEnum.DAY_SET, textString, alsoOnNonWorkingDays, targetTime);
            } else {
                throw new IllegalArgumentException("unknown Multi-Insert type selected");
            }

            if (success) {
                finish();
            }
        });
        binding.cancel.setOnClickListener(v -> {
            Logger.debug("canceling InsertDefaultTimesActivity");
            finish();
        });
    }

    private boolean setTarget(LocalDate fromInclusive, LocalDate toInclusive, TargetEnum type, String hint,
                           boolean alsoOnNonWorkingDays, @Nullable String targetTime) {
        try {
            for (LocalDate day = fromInclusive; !day.isAfter(toInclusive); day = next(day, alsoOnNonWorkingDays)) {
                Target target = dao.getDayTarget(day);
                boolean wasPresentBefore = target != null;
                if (!wasPresentBefore) {
                    target = new Target();
                }

                target.setDate(day);
                target.setType(type.getValue());
                target.setComment(hint);
                int targetTimeValue = TimerManager.parseHoursMinutesString(targetTime);
                target.setValue(targetTimeValue);

                if (wasPresentBefore) {
                    dao.updateTarget(target);
                } else {
                    dao.insertTarget(target);
                }
            }
            return true;
        } catch (Exception e) {
            Logger.warn(e, "problem while setting target {} from {} until {} / alsoOnNonWorkingDays={} / targetTime={}",
                type, fromInclusive, toInclusive, alsoOnNonWorkingDays, targetTime);
            Toast.makeText(this, "could not set target: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private LocalDate next(LocalDate day, boolean alsoNonWorking) {
        if (alsoNonWorking) {
            return day.plusDays(1);
        } else {
            if (timerManager.countWorkDays() == 0) {
                throw new IllegalStateException("no working days defined");
            }
            LocalDate potentialResult = day.plusDays(1);
            while (!timerManager.isWorkDay(potentialResult.getDayOfWeek())) {
                potentialResult = potentialResult.plusDays(1);
            }
            return potentialResult;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        throw new IllegalArgumentException("options menu: unknown item selected");
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
        updateFromDate(now);
        updateToDate(now);

        boolean flexiTimeEnabled = Basics.getInstance().getPreferences().getBoolean(Key.ENABLE_FLEXI_TIME.getName(), false);
        if (!flexiTimeEnabled) {
            Toast.makeText(this, R.string.enableFlexiTimeOrItWontWork, Toast.LENGTH_LONG).show();
        }
    }

    private void updateFromDate(LocalDate date) {
        dateFromTextViewController.setDate(date);
    }

    private void updateToDate(LocalDate date) {
        dateToTextViewController.setDate(date);
    }

}
