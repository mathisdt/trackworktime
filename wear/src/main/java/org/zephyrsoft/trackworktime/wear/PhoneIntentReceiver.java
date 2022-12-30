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
package org.zephyrsoft.trackworktime.wear;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester;

public class PhoneIntentReceiver extends BroadcastReceiver {

    private static final String STATUS_CLOCKED_IN = "org.zephyrsoft.trackworktime.status.ClockedIn";
    private static final String STATUS_CLOCKED_OUT = "org.zephyrsoft.trackworktime.status.ClockedOut";

    private ComplicationDataSourceUpdateRequester updateRequester;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (updateRequester == null) {
            updateRequester = ComplicationDataSourceUpdateRequester.create(context,
                new ComponentName(context, IconDataSourceService.class));
        }

        if (STATUS_CLOCKED_IN.equals(intent.getAction())) {
            ((WearApplication)context.getApplicationContext()).setAppOnPhoneClockedIn(true);
            updateRequester.requestUpdateAll();
        } else if (STATUS_CLOCKED_OUT.equals(intent.getAction())) {
            ((WearApplication)context.getApplicationContext()).setAppOnPhoneClockedIn(false);
            updateRequester.requestUpdateAll();
        } else {
            Log.d(WearApplication.TAG, "ignoring intent with action " + intent.getAction());
        }
    }
}