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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import androidx.appcompat.app.AppCompatActivity;

import org.pmw.tinylog.Logger;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.databinding.TargetBinding;
import org.zephyrsoft.trackworktime.model.Target;
import org.zephyrsoft.trackworktime.model.TargetEnum;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.util.Locale;

/**
 * Activity for managing the events of a week.
 *
 * @author Mathis Dirksen-Thedens
 */
public class TargetEditActivity extends AppCompatActivity
		implements OnDateChangedListener, OnTimeChangedListener, OnItemSelectedListener {

	private static final int DELETE_TARGET = 0;

	private DateTimeFormatter SHORT_DAY;

	private DAO dao = null;
	private TimerManager timerManager = null;

	private TargetBinding binding;
	private Spinner type = null;

	private boolean targetIsRelative = false;

	private int selectedYear = -1;
	private int selectedMonth = -1;
	private int selectedDay = -1;
	private boolean pickersAreInitialized = false;

	private LocalDate targetDay = null;
	/**
	 * only filled if an existing target is edited! blank for new targets!
	 */
	private Target editedTarget = null;
	private boolean newTarget = false;
	private boolean dayMode;

	@Override
	protected void onPause() {
		dao.close();
		super.onPause();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SHORT_DAY = DateTimeFormatter.ofPattern("E, ", Locale.getDefault());

		dao = Basics.getInstance().getDao();
		timerManager = Basics.getInstance().getTimerManager();

		binding = TargetBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		type = binding.targetType;

		EditText dateEdit = binding.dateEdit;
		dateEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override public void afterTextChanged(Editable s) {
				if (dateEdit.getVisibility() == View.GONE) {
					return;
				}

				if (isTargetValid(dateEdit.getText().toString())) {
					dateEdit.setError(null);
					binding.save.setEnabled(true);
				} else {
					dateEdit.setError("Target is invalid");
					binding.save.setEnabled(false);
				}
			}
		});

		binding.save.setOnClickListener(v -> {
			// commit all edit fields
			binding.date.clearFocus();
			binding.dateEdit.clearFocus();
			binding.text.clearFocus();


			// get values
			TargetEnum targetEnum = TargetEnum.DAY_IGNORE;
			int targetValue = 0;
			boolean useDateEdit = false;

			if (dayMode) {
				switch (type.getSelectedItemPosition()) {
					case 0:
						// holiday or vacation
						targetEnum = TargetEnum.DAY_SET;
						targetValue = 0;
						break;

					case 1:
						// TODO Grant static or dynamic?!

						// grant target time
						targetEnum = TargetEnum.DAY_GRANT;
						targetValue = 0;
						break;

					case 2:
						// set target time
						targetEnum = TargetEnum.DAY_SET;
						useDateEdit = true;
						break;
				}
			} else {
				// call listener methods manually to make sure that even on buggy Android 5.0 the data is correct
				// => https://code.google.com/p/android/issues/detail?id=78861
				DatePicker date = binding.date;
				onDateChanged(date, date.getYear(), date.getMonth(), date.getDayOfMonth());

				targetDay = getCurrentlySetDate();
				targetEnum = binding.radioFlexiSet.isChecked() ? TargetEnum.FLEXI_SET : TargetEnum.FLEXI_ADD;
				useDateEdit = true;
			}

			if (useDateEdit) {
				if (dateEdit.getVisibility() == View.GONE) {
					throw new java.lang.IllegalStateException("Internal error");
				}

				String targetValueString = DateTimeUtil.refineHourMinute(dateEdit.getText().toString());
				targetValue = TimerManager.parseHoursMinutesString(targetValueString);
			}

			String comment = binding.text.getText().toString();


			if (newTarget) {
				Target target = new Target(null, targetEnum.getValue(), targetValue, targetDay, comment);

				Logger.debug("saving new target: {}", target.toString());

				dao.insertTarget(target);
			} else {
				editedTarget.setType(targetEnum.getValue());
				editedTarget.setDate(targetDay);
				editedTarget.setValue(targetValue);
				editedTarget.setComment(comment);

				Logger.debug("saving changed target with ID {}: ", editedTarget.toString());

				dao.updateTarget(editedTarget);
			}

			// we have to call this manually when using the DAO directly
			timerManager.invalidateCacheFrom(targetDay);
			Basics.getInstance().safeCheckPersistentNotification();

			// close the editor
			finish();
		});

		Button cancel = binding.cancel;
		cancel.setOnClickListener(v -> {
			Logger.debug("Canceling TargetEditActivity");

			finish();
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!newTarget) {
			menu.add(Menu.NONE, DELETE_TARGET, DELETE_TARGET, getString(R.string.delete))
					.setIcon(android.R.drawable.ic_menu_delete)
					.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == DELETE_TARGET) {
			Logger.debug("Deleting target {}", editedTarget.toString());
			dao.deleteTarget(editedTarget);
			finish();

		} else {
			throw new IllegalArgumentException("options menu: unknown item selected");
		}

		return true;
	}

	@Override
	public void onBackPressed() {
		Logger.debug("Canceling TargetEditActivity (back button pressed)");
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();

		long epochDay = getIntent().getLongExtra(Constants.DATE_EXTRA_KEY, -1);

		if (epochDay == -1) {
			// general target
			dayMode = false;

			type.setVisibility(View.GONE);
			binding.dateText.setVisibility(View.GONE);

			targetDay = LocalDate.now();
			updateDatePicker(targetDay);
		} else {
			// target for single day
			dayMode = true;

			binding.radioType.setVisibility(View.GONE);
			binding.dateTimeLabel.setVisibility(View.GONE);
			binding.date.setVisibility(View.GONE);

			// check spinner & register listener
			if (type.getAdapter().getCount() != 3) {
				Logger.error("Canceling TargetEditActivity (wrong number of elements in targets");
				finish();
			}
			type.setOnItemSelectedListener(this);

			targetDay = LocalDate.ofEpochDay(epochDay);
			String dateText = targetDay.format(SHORT_DAY) + DateTimeUtil.formatLocalizedDate(targetDay);
			binding.dateText.setText(dateText);


			// check if day target already exists
			editedTarget = dao.getDayTarget(targetDay);
			if (editedTarget != null) {
				Logger.info("Editing existing day target...");

				newTarget = false;

				switch (TargetEnum.byValue(editedTarget.getType())) {
					case DAY_SET:
						if (editedTarget.getValue() == 0) {
							// holiday or vacation
							type.setSelection(0);
						} else {
							// set target time
							type.setSelection(2);
							binding.dateEdit.setText(DateTimeUtil.formatDuration(editedTarget.getValue()));
						}
						break;

					case DAY_GRANT:
						// grant target time
						type.setSelection(1);
						break;
				}

				binding.text.setText(editedTarget.getComment());
			} else {
				newTarget = true;
			}
		}
	}

	private void updateDatePicker(LocalDate day) {
		if (pickersAreInitialized) {
			binding.date.updateDate(day.getYear(), day.getMonthValue(), day.getDayOfMonth());
		} else {
			binding.date.setVisibility(View.VISIBLE);
			binding.date.init(day.getYear(), day.getMonthValue(), day.getDayOfMonth(), this);
			pickersAreInitialized = true;

			// manually set the variables once
			selectedYear = day.getYear();
			selectedMonth = day.getMonthValue();
			selectedDay = day.getDayOfMonth();
		}
	}

	// FIXME Copy & Paste
	private boolean isTargetValid(String target) {
		if (targetIsRelative) {
			try {
				int value = Integer.parseInt(target);
				return value >= 0 && value <= 100;
			} catch (NumberFormatException e) {
				// ignore and return false
			}
			return false;
		} else {
			return DateTimeUtil.isDurationValid(target);
		}
	}

	@Override
	public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		selectedYear = year;
		selectedMonth = monthOfYear;
		selectedDay = dayOfMonth;

		Logger.debug("date changed to {}-{}-{}", year, monthOfYear, dayOfMonth);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

		if (position <= 1) {
			// free day or grant target time
			binding.dateEditLabel.setVisibility(View.GONE);
			binding.dateEdit.setVisibility(View.GONE);
		} else {
			targetIsRelative = false;
			binding.dateEditLabel.setText(R.string.targetTimeAbs);
			binding.dateEditLabel.setVisibility(View.VISIBLE);
			binding.dateEdit.setVisibility(View.VISIBLE);
		}

		// FIXME
		//targetIsRelative = true;
		//dateEditLabel.setText(R.string.targetTimeRel);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

	@Override
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
	}

	private LocalDate getCurrentlySetDate() {
		return LocalDate.of(selectedYear, selectedMonth, selectedDay);
	}
}
