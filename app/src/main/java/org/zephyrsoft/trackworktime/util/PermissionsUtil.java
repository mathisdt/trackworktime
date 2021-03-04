/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TWT is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TWT. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.trackworktime.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.Constants;
import org.zephyrsoft.trackworktime.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

/**
 * Utility for handling permissions.
 */
public class PermissionsUtil {

    /**
     * @return {@code true} if files (backups, logs) can't be written to external storage
     */
    public static boolean missingPermissionForExternalStorage(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * @return the permissions which are not granted currently in order to enable tracking by location and/or Wi-Fi
     */
    public static List<String> missingPermissionsForTracking(Context context) {
        List<String> permissionsToRequest = new ArrayList<>();
        addPermissionIfNotGranted(Manifest.permission.ACCESS_COARSE_LOCATION, permissionsToRequest, context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            addPermissionIfNotGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION, permissionsToRequest, context);
        }
        addPermissionIfNotGranted(Manifest.permission.CHANGE_WIFI_STATE, permissionsToRequest, context);
        addPermissionIfNotGranted(Manifest.permission.ACCESS_WIFI_STATE, permissionsToRequest, context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            addPermissionIfNotGranted(Manifest.permission.FOREGROUND_SERVICE, permissionsToRequest, context);
        }
        return permissionsToRequest;
    }

    private static void addPermissionIfNotGranted(String permission, List<String> permissionsToRequest, Context context) {
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(permission);
        }
    }

    /**
     * @return the permissions which were not granted in the diven results
     * @see android.app.Activity#onRequestPermissionsResult(int, String[], int[])
     */
    public static List<String> notGrantedPermissions(String[] permissions, int[] grantResults) {
        List<String> result = new ArrayList<>();
        if (permissions==null || grantResults==null) {
            return result;
        }
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                result.add(permissions[i]);
            }
        }
        return result;
    }

    public static void askForLocationPermission(Context context, Runnable positiveConsequence, Runnable negativeConsequence) {
        new AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.locationPermissionsRequestTitle))
            .setMessage(context.getString(R.string.locationPermissionsRequestText)
                + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? context.getString(R.string.locationPermissionsRequestTextSupplement)
                : ""))
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                Basics.getOrCreateInstance(context).disableLocationBasedTracking();
                negativeConsequence.run();
            })
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                positiveConsequence.run();
            })
            .create()
            .show();
    }

    public static void askForStoragePermission(Context context, Runnable positiveConsequence) {
        new AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.storagePermissionsRequestTitle))
            .setMessage(context.getString(R.string.storagePermissionsRequestText))
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                positiveConsequence.run();
            })
            .create()
            .show();
    }

}
