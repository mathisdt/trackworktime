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

import java.text.DateFormat;
import java.util.Date;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;

import org.zephyrsoft.trackworktime.backup.WorkTimeTrackerBackupManager;
import org.zephyrsoft.trackworktime.database.DAO;
import org.zephyrsoft.trackworktime.options.AppCompatPreferenceActivity;
import org.zephyrsoft.trackworktime.options.Key;
import org.zephyrsoft.trackworktime.util.Logger;
import org.zephyrsoft.trackworktime.util.PreferencesUtil;

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
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			// Backup to Google servers is not supported before Froyo
			Preference backupEnabledPreference = findPreference(getString(R.string.keyBackupEnabled));
			if (backupEnabledPreference != null) {
				backupEnabledPreference.setEnabled(false);
			} else {
				Logger.warn("preference 'backup enabled' not found!");
			}
		}
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
			Logger.warn("option {0} is invalid => disabling option {1}", keyName, sectionToDisable.getName());

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
		}
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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showTimestampPrefIcon(final Preference timestampPref, final String dateLocalStr,
		final String dateBackupStr) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (dateLocalStr.equals(dateBackupStr)) {
				timestampPref.setIcon(R.drawable.backup_ok);
			} else {
				timestampPref.setIcon(R.drawable.backup_not_ok);
			}
		}
	}
}
