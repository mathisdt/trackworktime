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
package org.zephyrsoft.trackworktime.backup;

import android.annotation.TargetApi;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;

/**
 * @author Peter Rosenberg
 */
public class WorkTimeTrackerBackupManager {
	private static final String TIMESTAMP_BACKUP_KEY = "timestamp_backup";
	private final BackupManager backupManager;
	private final SharedPreferences timestampPrefs;
	private final SharedPreferences defaultPrefs;
	private boolean enabled;

	@TargetApi(Build.VERSION_CODES.FROYO)
	public WorkTimeTrackerBackupManager(final Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			backupManager = new BackupManager(context);
		} else {
			backupManager = null;
		}
		timestampPrefs = context.getSharedPreferences("timestampPrefs", Context.MODE_PRIVATE);
		defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		enabled = defaultPrefs.getBoolean("backup_enabled", true);
	}

	public void checkIfBackupEnabledChanged() {
		final boolean newValue = defaultPrefs.getBoolean("backup_enabled", true);
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
