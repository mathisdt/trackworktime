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

import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.APP_VERSION_CODE;
import static org.acra.ReportField.APP_VERSION_NAME;
import static org.acra.ReportField.BRAND;
import static org.acra.ReportField.CRASH_CONFIGURATION;
import static org.acra.ReportField.INSTALLATION_ID;
import static org.acra.ReportField.LOGCAT;
import static org.acra.ReportField.PACKAGE_NAME;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.PRODUCT;
import static org.acra.ReportField.REPORT_ID;
import static org.acra.ReportField.SHARED_PREFERENCES;
import static org.acra.ReportField.STACK_TRACE;
import static org.acra.ReportField.USER_APP_START_DATE;
import static org.acra.ReportField.USER_CRASH_DATE;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;
import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.util.TinylogAndLogcatLogger;

import java.util.concurrent.TimeUnit;

/**
 * Application entry point.
 */
public class WorkTimeTrackerApplication extends Application {

	private Basics basics;

	public WorkTimeTrackerApplication() {
		Logger.info("instantiating application");
	}

	@Override
	public void onCreate() {
		Logger.info("creating application");

		NotificationChannel notificationChannel = null;
		NotificationChannel serviceNotificationChannel = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			String name = getString(R.string.standardChannelName);
			String description = getString(R.string.standardChannelDescription);
			int importance = NotificationManager.IMPORTANCE_DEFAULT;
			notificationChannel = new NotificationChannel(name, name, importance);
			notificationChannel.setDescription(description);
			notificationManager.createNotificationChannel(notificationChannel);

			name = getString(R.string.serviceChannelName);
			description = getString(R.string.serviceChannelDescription);
			importance = NotificationManager.IMPORTANCE_LOW;
			serviceNotificationChannel = new NotificationChannel(name, name, importance);
			serviceNotificationChannel.setDescription(description);
			notificationManager.createNotificationChannel(serviceNotificationChannel);
		}

        CoreConfigurationBuilder builder = new CoreConfigurationBuilder()
            .withBuildConfigClass(BuildConfig.class)
            .withReportFormat(StringFormat.JSON)
            .withReportContent(ANDROID_VERSION, APP_VERSION_CODE, APP_VERSION_NAME,
                BRAND, CRASH_CONFIGURATION, INSTALLATION_ID, LOGCAT,
                PACKAGE_NAME, PHONE_MODEL, PRODUCT, REPORT_ID, SHARED_PREFERENCES,
                STACK_TRACE, USER_APP_START_DATE, USER_CRASH_DATE)
            .withPluginConfigurations(new DialogConfigurationBuilder()
                    .withTitle(getString(R.string.acraTitle))
                    .withText(getString(R.string.acraText))
                    .withCommentPrompt(getString(R.string.acraCommentPrompt))
                    .withEnabled(true)
                    .build(),
                new HttpSenderConfigurationBuilder()
                    .withHttpMethod(HttpSender.Method.POST)
                    .withUri("https://crashreport.zephyrsoft.org/")
                    .withEnabled(true)
                    .build());

		ACRA.init(this, builder);
		ACRA.log = new TinylogAndLogcatLogger();

		basics = new Basics(this);

		basics.setNotificationChannel(notificationChannel);
		basics.setServiceNotificationChannel(serviceNotificationChannel);

		try {
			PeriodicWorkRequest automaticBackup = new PeriodicWorkRequest.Builder(AutomaticBackup.class, 24, TimeUnit.HOURS, 6, TimeUnit.HOURS)
					.build();
			WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(Constants.WORK_AUTOBACKUP, ExistingPeriodicWorkPolicy.KEEP, automaticBackup);

			Logger.info("Successfully installed periodic work request.");
		} catch (IllegalStateException e) {
			Logger.error(e.getMessage());
		}

		Logger.info("handing off to super");
		super.onCreate();
	}

	@Override
	public void onTerminate() {
		Logger.info("terminating application");
		basics.getDao().close();
		basics.unregisterThirdPartyReceiver();
		super.onTerminate();
	}

	@Override
	public void onLowMemory() {
		Logger.info("low memory for application");
		super.onLowMemory();
	}

	public Basics getBasics() {
		return basics;
	}

}
