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
package org.zephyrsoft.trackworktime;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import org.pmw.tinylog.Logger;
import org.threeten.bp.LocalDate;
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

public class Widget extends AppWidgetProvider {

	private static final String ACTION_UPDATE = BuildConfig.APPLICATION_ID + ".WIDGET_UPDATE";

	private Context context;
	private AppWidgetManager manager;
	private RemoteViews views;
	private TimerManager timerManager;
	private int[] widgetIds;
	private Integer currentWidgetId;

	public static void dispatchUpdateIntent(Context context) {
		Intent intent = new Intent(context, Widget.class);
		intent.setAction(ACTION_UPDATE);
		context.sendBroadcast(intent);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		if(ACTION_UPDATE.equals(intent.getAction())) {
			onUpdate(context);
		}
	}

	private void onUpdate(Context context) {
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		ComponentName component = new ComponentName(context.getPackageName(),getClass().getName());
		int[] ids = manager.getAppWidgetIds(component);
		onUpdate(context, manager, ids);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager widgetManager, int[] widgetIds) {
		try {
			init(context, widgetManager, widgetIds);
			updateWidgets();
		} finally {
			clean();
		}
	}

	private void init(Context context, AppWidgetManager manager, int[] widgetIds) {
		this.context = context;
		this.manager = manager;
		this.widgetIds = widgetIds;
		this.views = new RemoteViews(context.getPackageName(), R.layout.widget);
		Basics basics = Basics.getOrCreateInstance(context.getApplicationContext());
		this.timerManager = basics.getTimerManager();
	}

	private void clean() {
		context = null;
		manager = null;
		views = null;
		widgetIds = null;
		currentWidgetId = null;
		timerManager = null;
	}

	private void updateWidgets() {
		for(int id : widgetIds) {
			currentWidgetId = id;
			updateWidget();
		}
	}

	private void updateWidget() {
		try {
			updateWorkTime();
			updateClockInBtn();
			updateClockOutBtn();
			dispatchUpdate();
		} catch (Exception e) {
			Logger.debug("Exception: {}", e.getMessage());
		}
	}

	private void updateWorkTime() {
		int workedTime = (int)timerManager.calculateTimeSum(LocalDate.now(), PeriodEnum.DAY);
		String timeSoFar = DateTimeUtil.formatDuration(workedTime);
		String workedText = getString(R.string.worked) + ": " + timeSoFar;
		views.setTextViewText(R.id.workTime, workedText);
	}

	private void updateClockInBtn() {
		int textRes = isClockedIn() ? R.string.clockInChangeShort : R.string.clockIn;
		String text = getString(textRes);
		int viewId = R.id.clockIn;
		views.setTextViewText(viewId, text);
		PendingIntent intent = createIntentForAction(Constants.CLOCK_IN_ACTION);
		views.setOnClickPendingIntent(viewId, intent);
	}

	private void updateClockOutBtn() {
		PendingIntent intent = createIntentForAction(Constants.CLOCK_OUT_ACTION);
		int viewId = R.id.clockOut;
		views.setOnClickPendingIntent(viewId, intent);

		boolean isClockedIn = isClockedIn();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			views.setBoolean(viewId, "setEnabled", isClockedIn);
		} else {
			// setBoolean() is not supported. Make text appear like disabled.
			@ColorRes int textColorRes = isClockedIn ? R.color.accent : R.color.text_disabled;
			@ColorInt int textColor = ContextCompat.getColor(context, textColorRes);
			views.setTextColor(viewId, textColor);
		}
	}

	private boolean isClockedIn() {
		return timerManager.isTracking();
	}

	private PendingIntent createIntentForAction(String action) {
		Intent intent = new Intent(context, ThirdPartyReceiver.class);
		intent.setAction(action);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}

	private void dispatchUpdate() {
		manager.updateAppWidget(currentWidgetId, views);
	}

	private String getString(@StringRes int id) {
		return context.getString(id);
	}

}

