package org.zephyrsoft.trackworktime.backup;

import android.os.Environment;

import androidx.annotation.NonNull;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.Constants;

import java.io.File;

public class BackupFileInfo {

    private static final String BACKUP_FILE_OLD = "backup.csv";
    private static final String BACKUP_FILE_EVENTS = "backup.events.csv";
    private static final String BACKUP_FILE_TARGETS = "backup.targets.csv";

    public File eventsBackupFile;
    public File targetsBackupFile;

    private BackupFileInfo() {}

    @NonNull @Override
    public String toString() {
        return eventsBackupFile.toString() +
            "\n" + targetsBackupFile.toString();
    }

    public String listAvailable() {
        // TODO use StringJoiner
        String separator = "";
        StringBuilder sb = new StringBuilder();

        if (eventsBackupFile.exists()) {
            sb.append(eventsBackupFile);
            separator = "\n";
        }
        if (targetsBackupFile.exists()) {
            sb.append(separator).append(targetsBackupFile);
        }

        return sb.toString();
    }

    /**
     * Get backup files
     * @param existing search for existing files
     * @param prefix prefix for file names
     */
    public static BackupFileInfo getBackupFiles(boolean existing, String prefix) {
        final File backupDir = new File(Environment.getExternalStorageDirectory(), Constants.DATA_DIR);

        if (existing && !backupDir.exists()) {
            Logger.warn("Backup folder does not exist.");
            return null;
        }

        BackupFileInfo info = new BackupFileInfo();
        info.eventsBackupFile = new File(backupDir, prefix + BACKUP_FILE_EVENTS);
        info.targetsBackupFile = new File(backupDir, prefix + BACKUP_FILE_TARGETS);

        if (existing && !info.eventsBackupFile.exists()) {
            final File eventsBackupFile = new File(backupDir, BACKUP_FILE_OLD);

            if (eventsBackupFile.exists()) {
                info.eventsBackupFile = eventsBackupFile;
            }
        }

        if (!existing || info.eventsBackupFile.exists() || info.targetsBackupFile.exists()) {
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
    public static BackupFileInfo getBackupFiles(boolean existing) {
        return getBackupFiles(existing, "");
    }
}
