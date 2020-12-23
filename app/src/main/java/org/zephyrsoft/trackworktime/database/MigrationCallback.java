package org.zephyrsoft.trackworktime.database;

public interface MigrationCallback {
    void onProgressUpdate(int value);

    void migrationDone();
}
