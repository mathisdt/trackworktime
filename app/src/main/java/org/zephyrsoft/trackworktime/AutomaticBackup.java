package org.zephyrsoft.trackworktime;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.backup.BackupFileInfo;
import org.zephyrsoft.trackworktime.util.BackupUtil;

import java.io.File;

public class AutomaticBackup extends Worker {

    private static final String AUTOMATIC_BACKUP_FILE = "automatic-";

    private final Context context;

    public AutomaticBackup(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure(new Data.Builder().putString("error", "the app doesn't have the permission to write to external storage - please trigger a manual backup to grant this permission").build());
        }
        final File externalStorageDirectory = Environment.getExternalStorageDirectory();
        if (externalStorageDirectory == null) {
            Logger.warn("automatic backup failed because getExternalStorageDirectory() returned null");
            return Result.failure(new Data.Builder().putString("error", "external storage directory could not be found").build());
        }
        final BackupFileInfo info = BackupFileInfo.getBackupFiles(false, AUTOMATIC_BACKUP_FILE);

        Logger.info("starting automatic backup");
        BackupUtil.doBackup(context, info);
        return Result.success();
    }
}
