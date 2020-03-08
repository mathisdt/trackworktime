package org.zephyrsoft.trackworktime.util;

import android.content.Context;

import org.zephyrsoft.trackworktime.Basics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class BackupUtil {

    private BackupUtil() {
        // only static usage
    }

    public static Boolean doBackup(Context context, File backupFile) {
        try {
            backupFile.getParentFile().mkdirs();
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(backupFile)));

            Basics.getOrCreateInstance(context).getDao().backupToWriter(writer);
            writer.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
