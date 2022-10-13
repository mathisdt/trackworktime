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
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.backup.BackupFileInfo;
import org.zephyrsoft.trackworktime.util.BackupUtil;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

public class AutomaticBackup extends Worker {

    private final Context context;

    public AutomaticBackup(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        return Futures.immediateFuture(new ForegroundInfo(Constants.AUTOMATIC_BACKUP_ID,
            Basics.get(context).createNotification(context.getString(R.string.backup_started_in_background),
                context.getString(R.string.backup_started_in_background),
                null,
                null,
                false,
                null, null, null,
                null, null, null)));
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            if (!DocumentTreeStorage.hasValidDirectoryGrant(context)) {
                Logger.warn("automatic backup failed because no document tree access has been granted");
                return Result.failure(new Data.Builder()
                    .putString("error", context.getString(R.string.noDirectoryAccessGrantedError))
                    .putString("lastError", DateTimeUtil.timestampNow())
                    .build());
            }
            final BackupFileInfo info = BackupFileInfo.getBackupFiles(context, false, true);

            Logger.info("starting automatic backup");
            BackupUtil.doBackup(context, info);
            Logger.info("automatic backup done");
            return Result.success(new Data.Builder()
                .putString("lastSuccess", DateTimeUtil.timestampNow())
                .build());
        } catch (Exception e) {
            Logger.warn(e, "error while doing automatic backup");
            return Result.failure(new Data.Builder()
                .putString("error", e.getMessage())
                .putString("lastError", DateTimeUtil.timestampNow())
                .build());
        }
    }
}
