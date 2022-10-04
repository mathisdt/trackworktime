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

import android.content.Context;
import android.content.Intent;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.Basics;
import org.zephyrsoft.trackworktime.Constants;
import org.zephyrsoft.trackworktime.model.Event;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.timer.TimerManager;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sends broadcast intents so other apps can react to events.
 */
public class BroadcastUtil {

    public enum Action {
        CREATED(Constants.EVENT_CREATED_ACTION),
        UPDATED(Constants.EVENT_UPDATED_ACTION),
        DELETED(Constants.EVENT_DELETED_ACTION);

        private final String name;

        Action(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static void sendEventBroadcast(Event event, Context context, Action action, TimerManager.EventOrigin source) {
        Intent intent = new Intent();
        intent.setAction(action.getName());
        fillIntent(event, context, intent, source);
        context.sendBroadcast(intent);
        Logger.debug("sent broadcast intent with action {} for event {} with source {}: {}",
            action.name(), event.getId(), source, event.toString());
    }

    private static void fillIntent(Event event, Context context, Intent intent, TimerManager.EventOrigin source) {
        intent.putExtra("id", event.getId());
        OffsetDateTime dateTime = event.getDateTime().withNano(0);
        intent.putExtra("date", DateTimeFormatter.ISO_LOCAL_DATE.format(dateTime));
        intent.putExtra("time", DateTimeFormatter.ISO_LOCAL_TIME.format(dateTime));
        intent.putExtra("timezone_offset", dateTime.getOffset().toString());
        intent.putExtra("timezone_offset_minutes", dateTime.getOffset().getTotalSeconds() / 60);
        intent.putExtra("type_id", event.getType());
        intent.putExtra("type", event.getTypeEnum().name());
        if (event.getTask() != null) {
            intent.putExtra("task_id", event.getTask());
            Task task = Basics.get(context).getDao().getTask(event.getTask());
            if (task != null) {
                intent.putExtra("task", task.getName());
            }
        }
        intent.putExtra("comment", event.getText());
        intent.putExtra("source", source.name());
    }
}
