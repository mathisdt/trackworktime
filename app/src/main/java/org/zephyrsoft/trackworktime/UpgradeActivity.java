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

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.database.MigrationCallback;
import org.zephyrsoft.trackworktime.databinding.ActivityUpgradeBinding;

public class UpgradeActivity extends AppCompatActivity implements MigrationCallback {

	private ActivityUpgradeBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivityUpgradeBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		binding.migrationProgress.setVisibility(View.GONE);

		TextView upgradeText = binding.textUpgrade;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			upgradeText.setText(Html.fromHtml(getString(R.string.upgradeText), Html.FROM_HTML_MODE_COMPACT));
		} else {
			upgradeText.setText(Html.fromHtml(getString(R.string.upgradeText)));
		}

		binding.startMigration.setOnClickListener(v -> {
			// show progress bar
			binding.startMigration.setVisibility(View.GONE);
			binding.migrationProgress.setVisibility(View.VISIBLE);

			// save home time zone
			Basics basics = Basics.get(this);
			basics.setHomeTimeZone(binding.timeZonePicker.getZoneId());

			// start migration
			DAO dao = basics.getDao();
			dao.migrateEventsToV2(binding.timeZonePicker.getZoneId(), this);
		});
	}

	@Override
	public void onProgressUpdate(int value) {
		binding.migrationProgress.setProgress(value);
	}

	@Override
	public void migrationDone() {
		Toast.makeText(this, getString(R.string.databaseMigrationSuccess), Toast.LENGTH_LONG).show();
		finish();
	}
}
