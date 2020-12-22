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
			Basics basics = Basics.getOrCreateInstance(getApplicationContext());
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
		Toast.makeText(this, "Successfully migrated database.", Toast.LENGTH_LONG).show();
		finish();
	}
}
