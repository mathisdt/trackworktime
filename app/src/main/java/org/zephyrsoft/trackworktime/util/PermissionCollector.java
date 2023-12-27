package org.zephyrsoft.trackworktime.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.google.auto.service.AutoService;

import org.acra.builder.ReportBuilder;
import org.acra.collector.Collector;
import org.acra.collector.CollectorException;
import org.acra.config.CoreConfiguration;
import org.acra.data.CrashReportData;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

@AutoService(Collector.class)
public class PermissionCollector implements Collector {

    @Override
    public void collect(@NonNull Context context, @NonNull CoreConfiguration coreConfiguration, @NonNull ReportBuilder reportBuilder, @NonNull CrashReportData crashReportData) throws CollectorException {
        String granted = String.join(", ", getGrantedPermissions(context));
        crashReportData.put("GRANTED_PERMISSIONS", granted);
    }

    private List<String> getGrantedPermissions(Context context) {
        List<String> granted = new ArrayList<>();
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            for (int i = 0; i < pi.requestedPermissions.length; i++) {
                if ((pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                    granted.add(pi.requestedPermissions[i]);
                }
            }
        } catch (Exception e) {
            Logger.warn(e, "could not determine the granted permissions");
        }
        return granted;
    }

    @Override
    public boolean enabled(@NonNull CoreConfiguration config) {
        return true;
    }
}
