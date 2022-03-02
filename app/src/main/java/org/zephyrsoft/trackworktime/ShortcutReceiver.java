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

import android.app.Activity;
import android.os.Bundle;

import org.pmw.tinylog.Logger;
import org.threeten.bp.OffsetDateTime;
import org.zephyrsoft.trackworktime.model.TypeEnum;
import org.zephyrsoft.trackworktime.timer.TimerManager;

/**
 * This technically is an activity but it only receives intents from shortcuts
 * and the directly exits again. No UI is ever shown.
 */
public class ShortcutReceiver extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String action = getIntent().getAction();
        TimerManager timerManager = Basics.getOrCreateInstance(this).getTimerManager();
        if (Constants.CLOCK_IN_ACTION.equals(action)) {
            Logger.info("TRACKING: clock-in via shortcut");
            Integer taskId = ThirdPartyReceiver.getDefaultTaskId(this);
            timerManager.createEvent(OffsetDateTime.now(), taskId, TypeEnum.CLOCK_IN, null);
            WorkTimeTrackerActivity instanceOrNull = WorkTimeTrackerActivity.getInstanceOrNull();
            if (instanceOrNull != null) {
                instanceOrNull.refreshView();
            }
            Widget.dispatchUpdateIntent(this);
        } else if (Constants.CLOCK_OUT_ACTION.equals(action)) {
            Logger.info("TRACKING: clock-out via shortcut");
            timerManager.createEvent(OffsetDateTime.now(), null, TypeEnum.CLOCK_OUT, null);
            WorkTimeTrackerActivity instanceOrNull = WorkTimeTrackerActivity.getInstanceOrNull();
            if (instanceOrNull != null) {
                instanceOrNull.refreshView();
            }
            Widget.dispatchUpdateIntent(this);
        }
        finish();
        super.onCreate(savedInstanceState);
    }
}