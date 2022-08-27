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

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.backup.BackupFileInfo;
import org.zephyrsoft.trackworktime.util.BackupUtil;

public class AutomaticBackup extends Worker {

    private final Activity activity;

    public AutomaticBackup(@NonNull Activity activity, @NonNull WorkerParameters workerParams) {
        super(activity, workerParams);
        this.activity = activity;
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!DocumentTreeStorage.hasValidDirectoryGrant(activity)) {
            Logger.warn("automatic backup failed because no document tree access has been granted");
            return Result.failure(new Data.Builder().putString("error", activity.getString(R.string.noDirectoryAccessGrantedError)).build());
        }
        final BackupFileInfo info = BackupFileInfo.getBackupFiles(activity, false, true);

        Logger.info("starting automatic backup");
        BackupUtil.doBackup(activity, info);
        return Result.success();
    }
}
