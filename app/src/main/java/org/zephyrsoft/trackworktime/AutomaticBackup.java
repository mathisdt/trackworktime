package org.zephyrsoft.trackworktime;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import com.google.common.util.concurrent.ListenableFuture;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.util.BackupUtil;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.io.File;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

public class AutomaticBackup extends Worker {

    private static final String AUTOMATIC_BACKUP_FILE = "automatic-backup.csv";

    private Context context;

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
        final File backupDir = new File(externalStorageDirectory, Constants.DATA_DIR);
        final File backupFile = new File(backupDir, AUTOMATIC_BACKUP_FILE);

        Logger.info("starting automatic backup");
        BackupUtil.doBackup(context, backupFile);
        return Result.success();
    }
}
