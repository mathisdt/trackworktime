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

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import androidx.core.app.ActivityCompat;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.backup.WorkTimeTrackerBackupManager;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.options.AppCompatPreferenceActivity;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.util.PreferencesUtil;

import java.text.DateFormat;
import java.util.Date;

/**
 * Activity to set the preferences of the application.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class OptionsActivity extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener {

	private WorkTimeTrackerBackupManager backupManager;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.options);
		backupManager = new WorkTimeTrackerBackupManager(this);
		setTimestamps();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onStop() {
		backupManager.checkIfBackupEnabledChanged();
		super.onStop();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

		// make sure that location-based tracking gets enabled/disabled
		Basics.getOrCreateInstance(getApplicationContext()).safeCheckLocationBasedTracking();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyName) {
		Key sectionToDisable = PreferencesUtil.check(sharedPreferences, keyName);
		if (sectionToDisable != null && PreferencesUtil.getBooleanPreference(sharedPreferences, sectionToDisable)) {
			Logger.warn("option {} is invalid => disabling option {}", keyName, sectionToDisable.getName());

			// show message to user
			Intent messageIntent = Basics
				.getInstance()
				.createMessageIntent(
					"The option \""
						+ getString(sectionToDisable.getReadableNameResourceId())
						+ "\" was disabled due to invalid settings.\n\nYou can re-enable it after you have checked the values you entered in that section.",
					null);
			startActivity(messageIntent);

			// deactivate the section
			PreferencesUtil.disablePreference(sharedPreferences, sectionToDisable);
			// reload data in options view
			setPreferenceScreen(null);
			addPreferencesFromResource(R.xml.options);
		} else {
			if (Key.LOCATION_BASED_TRACKING_ENABLED.getName().equals(keyName)
					&& sharedPreferences.getBoolean(keyName, false)
					||
					Key.WIFI_BASED_TRACKING_ENABLED.getName().equals(keyName)
							&& sharedPreferences.getBoolean(keyName, false)
					) {
				if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
				}

			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		for (int i = 0; i < permissions.length; i++) {
			if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i])) {
				if (grantResults != null && grantResults.length > i && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
					final SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
					editor.putBoolean(Key.LOCATION_BASED_TRACKING_ENABLED.getName(), false);
					editor.putBoolean(Key.WIFI_BASED_TRACKING_ENABLED.getName(), false);
					editor.apply();

					Intent messageIntent = Basics.getInstance()
							.createMessageIntent("This option needs location permission.", null);
					startActivity(messageIntent);

					// reload data in options view
					setPreferenceScreen(null);
					addPreferencesFromResource(R.xml.options);
				}
			}
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@SuppressWarnings("deprecation")
	private void setTimestamps() {
		final Preference lastModifiedPref = findPreference(getString(R.string.keyBackupLastModifiedTimestamp));
		final Preference lastBackupPref = findPreference(getString(R.string.keyBackupLastBackupTimestamp));
		if (lastModifiedPref == null || lastBackupPref == null) {
			Logger.warn("backup timestamps preference not found!");
			return;
		}
		final DAO dao = new DAO(this);
		final long lastDbModification = dao.getLastDbModification();

		final DateFormat dateFormatUser = DateFormat.getDateInstance();
		final DateFormat timeFormatUser = DateFormat.getTimeInstance();

		final Date dateLocal = new Date(lastDbModification);
		final String dateLocalStr = dateFormatUser.format(dateLocal) + " "
			+ timeFormatUser.format(dateLocal);
		lastModifiedPref.setSummary(dateLocalStr);

		final long dateBackupLong = backupManager.getLastBackupTimestamp();
		final String dateBackupStr;
		if (dateBackupLong == 0) {
			dateBackupStr = "-";
		} else {
			final Date dateBackup = new Date(dateBackupLong);
			dateBackupStr = dateFormatUser.format(dateBackup) + " "
				+ timeFormatUser.format(dateBackup);
		}

		lastBackupPref.setSummary(dateBackupStr);
		showTimestampPrefIcon(lastBackupPref, dateLocalStr, dateBackupStr);
	}

	private void showTimestampPrefIcon(final Preference timestampPref, final String dateLocalStr, final String dateBackupStr) {
		if (dateLocalStr.equals(dateBackupStr)) {
			timestampPref.setIcon(R.drawable.backup_ok);
		} else {
			timestampPref.setIcon(R.drawable.backup_not_ok);
		}
	}
}
