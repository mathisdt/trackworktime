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
package org.zephyrsoft.trackworktime;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.backup.BackupFileInfo;
import org.zephyrsoft.trackworktime.util.BackupUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutomaticBackup {

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
        .setDaemon(true)
        .build());

    public AutomaticBackup(@NonNull Context context) {
        this.context = context;
    }

    public void doAsynchronously() {
        executor.submit(this::doSynchronously);
    }

    private void doSynchronously() {
        try {
            if (!DocumentTreeStorage.hasValidDirectoryGrant(context)) {
                Logger.warn("automatic backup failed because no document tree access has been granted");
                return;
            }
            final BackupFileInfo info = BackupFileInfo.getBackupFiles(context, false, true);

            Logger.info("starting automatic backup");
            BackupUtil.doBackup(context, info);
            Logger.info("automatic backup done");
        } catch (Exception e) {
            Logger.warn(e, "error while doing automatic backup");
        }
    }
}
