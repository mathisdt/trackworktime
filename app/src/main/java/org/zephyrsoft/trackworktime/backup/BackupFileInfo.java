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

import org.apache.commons.lang3.StringUtils;
import org.zephyrsoft.trackworktime.DocumentTreeStorage;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

public class BackupFileInfo {

    private static final String BACKUP_FILE_OLD = "backup";
    private static final String BACKUP_FILE_EVENTS = "backup.events";
    private static final String BACKUP_FILE_TARGETS = "backup.targets";
    private static final String BACKUP_FILE_PREFERENCES = "backup.preferences";
    private static final String BACKUP_FILE_EXTENSION = ".csv";

    private String eventsBackupFile;
    private String targetsBackupFile;
    private String preferencesBackupFile;
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
        return getBackupFiles(context, existing, isAutomaticBackup, null);
    }

    private static BackupFileInfo getBackupFiles(Context context, boolean existing, boolean isAutomaticBackup, String suffix) {
        BackupFileInfo info = new BackupFileInfo();
        info.eventsBackupFile = BACKUP_FILE_EVENTS +
            (StringUtils.isNotBlank(suffix) ? "." + suffix : "")
            + BACKUP_FILE_EXTENSION;
        info.targetsBackupFile = BACKUP_FILE_TARGETS +
            (StringUtils.isNotBlank(suffix) ? "." + suffix : "")
            + BACKUP_FILE_EXTENSION;
        info.preferencesBackupFile = BACKUP_FILE_PREFERENCES +
            (StringUtils.isNotBlank(suffix) ? "." + suffix : "")
            + BACKUP_FILE_EXTENSION;
        info.type = isAutomaticBackup
                ? DocumentTreeStorage.Type.AUTOMATIC_BACKUP
                : DocumentTreeStorage.Type.MANUAL_BACKUP;

        if (existing && !exists(context, info.type, info.eventsBackupFile)) {
            if (exists(context, info.type, BACKUP_FILE_OLD + BACKUP_FILE_EXTENSION)) {
                info.eventsBackupFile = BACKUP_FILE_OLD + BACKUP_FILE_EXTENSION;
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

    /**
     * Get backup files (which don't have to exist) with current date and time as suffix
     */
    public static BackupFileInfo getBackupFilesWithTimestamp(Context context) {
        return getBackupFiles(context, false, false, DateTimeUtil.timestampNow());
    }
}
