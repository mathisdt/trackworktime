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

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.zephyrsoft.trackworktime.databinding.MessageBinding;
import org.zephyrsoft.trackworktime.util.ThemeUtil;

/**
 * Activity for showing a message to the user.
 */
public class MessageActivity extends AppCompatActivity implements OnClickListener {

	private MessageBinding binding;
	private int id = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		binding = MessageBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		ThemeUtil.styleActionBar(this, getSupportActionBar());

		binding.message.setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		String message = getIntent().getStringExtra(Constants.MESSAGE_EXTRA_KEY);
		id = getIntent().getIntExtra(Constants.ID_EXTRA_KEY, -1);

		binding.message.setText(message);
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

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			closeNotificationAndDialog();
			return true;
		}
		throw new IllegalArgumentException("options menu: unknown item selected");
	}

}
