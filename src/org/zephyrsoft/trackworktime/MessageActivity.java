/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

/**
 * Activity for showing a message to the user.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class MessageActivity extends Activity implements OnClickListener {
	
	/** used to transport the message via an intent's extended data */
	public static final String MESSAGE_EXTRA_KEY = "message";
	/** used to transport the notification' ID via an intent's extended data */
	public static final String ID_EXTRA_KEY = "notificationId";
	
	private TextView textView = null;
	private int id = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.message);
		textView = (TextView) findViewById(R.id.dialogMessage);
		textView.setOnClickListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		String message = getIntent().getStringExtra(MESSAGE_EXTRA_KEY);
		id = getIntent().getIntExtra(ID_EXTRA_KEY, -1);
		textView.setText(message);
	}
	
	@Override
	public void onClick(View view) {
		closeNotificationAndDialog();
	}
	
	@Override
	public void onBackPressed() {
		closeNotificationAndDialog();
	}
	
	private void closeNotificationAndDialog() {
		if (id != -1) {
			NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			service.cancel(id);
		}
		finish();
	}
	
}
