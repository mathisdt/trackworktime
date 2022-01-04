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
package org.zephyrsoft.trackworktime.backup;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import org.zephyrsoft.trackworktime.R;

public class WorkTimeTrackerBackupManager {
	private final String prefKeyBackupEnabled;
	private static final String TIMESTAMP_BACKUP_KEY = "timestamp_backup";
	private final BackupManager backupManager;
	private final SharedPreferences timestampPrefs;
	private final SharedPreferences defaultPrefs;
	private boolean enabled;

	public WorkTimeTrackerBackupManager(final Context context) {
		prefKeyBackupEnabled = context.getText(R.string.keyBackupEnabled) + "";
		backupManager = new BackupManager(context);
		timestampPrefs = context.getSharedPreferences("timestampPrefs", Context.MODE_PRIVATE);
		defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		enabled = defaultPrefs.getBoolean(prefKeyBackupEnabled, false);
	}

	public void checkIfBackupEnabledChanged() {
		final boolean newValue = defaultPrefs.getBoolean(prefKeyBackupEnabled, false);
		if (enabled != newValue && backupManager != null) {
			// trigger if changed
			backupManager.dataChanged();
		}
		enabled = newValue;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void dataChanged() {
		if (enabled && backupManager != null) {
			backupManager.dataChanged();
		}
	}

	public void setLastBackupTimestamp(long lastBackupTimestamp) {
		final Editor editor = timestampPrefs.edit();
		// save value as string because there is no data type to hold long
		editor.putString(TIMESTAMP_BACKUP_KEY, lastBackupTimestamp + "");
		editor.commit();
	}

	public long getLastBackupTimestamp() {
		return Long.parseLong(timestampPrefs.getString(TIMESTAMP_BACKUP_KEY, "0"));
	}
}
