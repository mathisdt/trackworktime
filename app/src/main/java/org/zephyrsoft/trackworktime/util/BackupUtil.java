package org.zephyrsoft.trackworktime.util;

import android.content.Context;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.backup.BackupFileInfo;
import org.zephyrsoft.trackworktime.database.DAO;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class BackupUtil {

    private BackupUtil() {
        // only static usage
    }

    public static Boolean doBackup(Context context, BackupFileInfo info) {
        try {
            info.eventsBackupFile.getParentFile().mkdirs();

            DAO dao = Basics.getOrCreateInstance(context).getDao();

            // Events
            {
                final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(info.eventsBackupFile)));

                dao.backupEventsToWriter(writer);
                writer.close();
            }

            // Targets
            {
                final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(info.targetsBackupFile)));

                dao.backupTargetsToWriter(writer);
                writer.close();
            }

            return true;
        } catch (Exception e) {
            Logger.warn(e, "problem while writing backup");
            return false;
        }
    }

}
