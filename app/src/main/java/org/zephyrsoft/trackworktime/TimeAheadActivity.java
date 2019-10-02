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
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.pmw.tinylog.Logger;

/**
 * Activity for querying the amount of time which the new event should be pre-dated.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class TimeAheadActivity extends AppCompatActivity {

	private TextView type = null;
	private EditText minutes = null;
	private Button cancel = null;
	private Button ok = null;

	/** see {@link Constants#TYPE_EXTRA_KEY} for possible values **/
	private int typeIndicator = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.time_ahead);
		type = findViewById(R.id.type);
		minutes = findViewById(R.id.minutes);
		cancel = findViewById(R.id.cancel);
		ok = findViewById(R.id.ok);
		cancel.setOnClickListener(v -> finish());
		ok.setOnClickListener(v -> {
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
	protected void onResume() {
		super.onResume();
		typeIndicator = getIntent().getIntExtra(Constants.TYPE_EXTRA_KEY, 0);
		String typeString = getIntent().getStringExtra(Constants.TYPE_STRING_EXTRA_KEY);
		type.setText(typeString);
	}

	@Override
	public void onBackPressed() {
		finish();
	}

}
