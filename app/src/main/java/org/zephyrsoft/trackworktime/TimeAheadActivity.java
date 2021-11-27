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
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.databinding.TimeAheadBinding;

/**
 * Activity for querying the amount of time which the new event should be pre-dated.
 */
public class TimeAheadActivity extends AppCompatActivity {

	private TimeAheadBinding binding;

	/** see {@link Constants#TYPE_EXTRA_KEY} for possible values **/
	private int typeIndicator = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);

		binding = TimeAheadBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		EditText minutes = binding.minutes;

		binding.cancel.setOnClickListener(v -> finish());
		binding.ok.setOnClickListener(v -> {
            int minutesValue = 0;
            try {
                minutesValue = Integer.parseInt(minutes.getText().toString());
            } catch (NumberFormatException nfe) {
                Logger.warn("could not convert \"{}\" to int", minutes.getText().toString());
            }
            switch (typeIndicator) {
                case 0:
                    WorkTimeTrackerActivity.getInstance().clockInAction(minutesValue);
                    break;
                case 1:
                    WorkTimeTrackerActivity.getInstance().clockOutAction(minutesValue);
                    break;
                default:
                    Logger.error("type {} is unknown, doing nothing", typeIndicator);
                    break;
            }
            finish();
        });
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
	protected void onResume() {
		super.onResume();
		typeIndicator = getIntent().getIntExtra(Constants.TYPE_EXTRA_KEY, 0);
		String typeString = getIntent().getStringExtra(Constants.TYPE_STRING_EXTRA_KEY);
		binding.type.setText(typeString);
	}

	@Override
	public void onBackPressed() {
		finish();
	}

}
