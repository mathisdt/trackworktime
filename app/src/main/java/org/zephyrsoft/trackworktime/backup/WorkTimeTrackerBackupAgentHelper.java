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

import android.annotation.TargetApi;
import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataOutput;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.Build;
import android.os.ParcelFileDescriptor;

/**
 * @author Peter Rosenberg
 */
@TargetApi(Build.VERSION_CODES.FROYO)
public class WorkTimeTrackerBackupAgentHelper extends BackupAgentHelper {
	// A key to uniquely identify the set of backup data
	private static final String PREFS_BACKUP_KEY = "prefs";
	// A key to uniquely identify the set of backup data
	private static final String DB_BACKUP_KEY = "db";

	// Allocate a helper and add it to the backup agent
	@Override
	public void onCreate() {
		// The name of the SharedPreferences file
		final String prefs = getPackageName() + "_preferences"; // getPackageName() cannot be used in final
		SharedPreferencesBackupHelper prefsHelper = new SharedPreferencesBackupHelper(this, prefs) {
			@Override
			public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
				ParcelFileDescriptor newState) {
				if (new WorkTimeTrackerBackupManager(WorkTimeTrackerBackupAgentHelper.this).isEnabled()) {
					super.performBackup(oldState, data, newState);
				}
			}
		};
		addHelper(PREFS_BACKUP_KEY, prefsHelper);

		DbBackupHelper dbHelper = new DbBackupHelper(this);
		addHelper(DB_BACKUP_KEY, dbHelper);
	}
}
