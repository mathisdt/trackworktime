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

import static org.zephyrsoft.trackworktime.DocumentTreeStorage.exists;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import org.zephyrsoft.trackworktime.DocumentTreeStorage;

public class BackupFileInfo {

    private static final String BACKUP_FILE_OLD = "backup.csv";
    private static final String BACKUP_FILE_EVENTS = "backup.events.csv";
    private static final String BACKUP_FILE_TARGETS = "backup.targets.csv";
    private static final String BACKUP_FILE_PREFERENCES = "backup.preferences.csv";

    private String eventsBackupFile;
    private String targetsBackupFile;
    private final String preferencesBackupFile = BACKUP_FILE_PREFERENCES;
    private DocumentTreeStorage.Type type;

    private BackupFileInfo() {}

    public String getEventsBackupFile() {
        return eventsBackupFile;
    }
    public String getTargetsBackupFile() {
        return targetsBackupFile;
    }
    public String getPreferencesBackupFile() {
        return preferencesBackupFile;
    }

    public DocumentTreeStorage.Type getType() {
        return type;
    }

    @NonNull @Override
    public String toString() {
        return eventsBackupFile
            + "\n" + targetsBackupFile
            + "\n" + preferencesBackupFile;
    }

    public String listAvailable(Activity activity) {
        String separator = "";
        StringBuilder sb = new StringBuilder();

        if (exists(activity, type, eventsBackupFile)) {
            sb.append(eventsBackupFile);
            separator = "\n";
        }
        if (exists(activity, type, targetsBackupFile)) {
            sb.append(separator).append(targetsBackupFile);
            separator = "\n";
        }
        if (exists(activity, type, preferencesBackupFile)) {
            sb.append(separator).append(preferencesBackupFile);
        }

        return sb.toString();
    }

    /**
     * Get backup files
     * @param existing search for existing files
     */
    public static BackupFileInfo getBackupFiles(Context context, boolean existing, boolean isAutomaticBackup) {
        BackupFileInfo info = new BackupFileInfo();
        info.eventsBackupFile = BACKUP_FILE_EVENTS;
        info.targetsBackupFile = BACKUP_FILE_TARGETS;
        info.type = isAutomaticBackup
                ? DocumentTreeStorage.Type.AUTOMATIC_BACKUP
                : DocumentTreeStorage.Type.MANUAL_BACKUP;

        if (existing && !exists(context, info.type, info.eventsBackupFile)) {
            if (exists(context, info.type, BACKUP_FILE_OLD)) {
                info.eventsBackupFile = BACKUP_FILE_OLD;
            }
        }

        if (!existing
                || exists(context, info.type, info.eventsBackupFile)
                || exists(context, info.type, info.targetsBackupFile)
                || exists(context, info.type, info.preferencesBackupFile)) {
            return info;
        } else {
            // no backup file exists
            return null;
        }
    }

    /**
     * Get backup files
     * @param existing search for existing files
     */
    public static BackupFileInfo getBackupFiles(Context context, boolean existing) {
        return getBackupFiles(context, existing, false);
    }
}
