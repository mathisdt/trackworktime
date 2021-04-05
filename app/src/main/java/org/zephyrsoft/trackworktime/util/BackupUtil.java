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
