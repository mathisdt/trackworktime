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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility for handling permissions.
 */
public class PermissionsUtil {

    /**
     * @return the permissions which are not granted currently in order to enable tracking by location and/or Wi-Fi
     */
    public static Set<String> missingPermissionsForTracking(Context context) {
        Set<String> permissionsToRequest = new HashSet<>();

        List<String> locationPermissions = new ArrayList<>();
        locationPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        locationPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            locationPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }
        for (String permission : locationPermissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.addAll(locationPermissions);
                break;
            }
        }

        // beginning with API version 30 (Android R), also asking for ACCESS_BACKGROUND_LOCATION
        // in parallel results in no permission request shown at all!
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            addPermissionIfNotGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION, permissionsToRequest, context);
        }

        addPermissionIfNotGranted(Manifest.permission.CHANGE_WIFI_STATE, permissionsToRequest, context);
        addPermissionIfNotGranted(Manifest.permission.ACCESS_WIFI_STATE, permissionsToRequest, context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            addPermissionIfNotGranted(Manifest.permission.FOREGROUND_SERVICE, permissionsToRequest, context);
        }
        return permissionsToRequest;
    }

    public static boolean isBackgroundPermissionMissing(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isNotificationPermissionMissing(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
    }

    private static void addPermissionIfNotGranted(String permission, Set<String> permissionsToRequest, Context context) {
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(permission);
        }
    }

    public static List<String> notGrantedPermissions(Map<String, Boolean> permissionsWithGrantResults) {
        List<String> result = new ArrayList<>();
        if (permissionsWithGrantResults == null) {
            return result;
        }
        for (Map.Entry<String, Boolean> entry : permissionsWithGrantResults.entrySet()) {
            if (entry.getValue()==null || !entry.getValue()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public static void askForLocationPermission(Activity activity, Runnable positiveConsequence, Runnable negativeConsequence) {
        new AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.locationPermissionsRequestTitle))
            .setMessage(activity.getString(R.string.locationPermissionsRequestText)
                + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? activity.getString(R.string.locationPermissionsRequestTextSupplementForAPI29)
                : "")
                + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? String.format(activity.getString(R.string.locationPermissionsRequestTextSupplementForAPI30),
                    activity.getPackageManager().getBackgroundPermissionOptionLabel())
                : ""))
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                Basics.get(activity).disableLocationBasedTracking();
                Basics.get(activity).disableWifiBasedTracking();
                negativeConsequence.run();
            })
            .setPositiveButton(android.R.string.ok, (dialog, which) -> positiveConsequence.run())
            .create()
            .show();
    }

    public static void askForDocumentTreePermission(Context context,
                                                    Runnable positiveConsequence,
                                                    Runnable negativeConsequence,
                                                    int textResourceId,
                                                    String... textParameters) {
        new AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.documentTreePermissionsRequestTitle))
            .setMessage(context.getString(textResourceId, (Object[]) textParameters))
            .setPositiveButton(android.R.string.ok, (dialog, which) -> positiveConsequence.run())
            .setNegativeButton(R.string.notNow, (dialog, which) -> negativeConsequence.run())
            .create()
            .show();
    }

    public void openSystemSettingsForApp(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }

}
