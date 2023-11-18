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
import org.zephyrsoft.trackworktime.model.PeriodEnum;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

import java.time.LocalDate;

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
        if (ACTION_UPDATE.equals(intent.getAction())) {
            onUpdate(context);
        }
    }

    private void onUpdate(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context.getPackageName(), getClass().getName());
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
        Basics basics = Basics.get(context);
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
        for (int id : widgetIds) {
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
            Logger.warn(e, "could not update widget");
        }
    }

    private void updateWorkTime() {
        int workedTime = (int) timerManager.calculateTimeSum(LocalDate.now(), PeriodEnum.DAY);
        String timeSoFar = DateTimeUtil.formatDuration(workedTime);
        String workedText = context.getString(R.string.workedWithDuration, timeSoFar);
        int viewId = R.id.workTime;
        views.setTextViewText(viewId, workedText);
        Intent intent = new Intent(context, WorkTimeTrackerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE
                : 0));
        views.setOnClickPendingIntent(viewId, pendingIntent);
    }

    private void updateClockInBtn() {
        int textRes = isClockedIn() ? R.string.clockInChangeShort : R.string.clockIn;
        String text = context.getString(textRes);
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
        return PendingIntent.getBroadcast(context, 0, intent,
            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE
                : 0));
    }

    private void dispatchUpdate() {
        manager.updateAppWidget(currentWidgetId, views);
    }

}

