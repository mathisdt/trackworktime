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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * Activity for querying the amount of time which the new event should be pre-dated.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class TimeAheadActivity extends Activity {
	
	private TextView type = null;
	private EditText minutes = null;
	private Button cancel = null;
	private Button ok = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.time_ahead);
		type = (TextView) findViewById(R.id.type);
		minutes = (EditText) findViewById(R.id.minutes);
		cancel = (Button) findViewById(R.id.cancel);
		ok = (Button) findViewById(R.id.ok);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int minutesValue = 0;
				try {
					minutesValue = Integer.parseInt(minutes.getText().toString());
				} catch (NumberFormatException nfe) {
					Logger.warn("could not convert \"{0}\" to int", minutes.getText().toString());
				}
				WorkTimeTrackerActivity.getInstance().clockInOutAction(minutesValue);
				finish();
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		String typeString = getIntent().getStringExtra(Constants.TYPE_EXTRA_KEY);
		type.setText(typeString);
	}
	
	@Override
	public void onBackPressed() {
		finish();
	}
	
}
