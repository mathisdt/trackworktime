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

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.MonochromaticImage;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;

import org.jetbrains.annotations.Nullable;

public class IconDataSourceService extends ComplicationDataSourceService {

    private static final String ACTION_CLOCK_IN = "org.zephyrsoft.trackworktime.ClockIn";
    private static final String ACTION_CLOCK_OUT = "org.zephyrsoft.trackworktime.ClockOut";

    private boolean isClockedIn() {
        return ((WearApplication) getApplicationContext()).isAppOnPhoneClockedIn();
    }

    @Nullable
    @Override
    public ComplicationData getPreviewData(@NonNull ComplicationType complicationType) {
        return null;
    }

    @Override
    public void onComplicationRequest(@NonNull ComplicationRequest request, @NonNull ComplicationRequestListener listener) {
        if (request.getComplicationType() != ComplicationType.SHORT_TEXT) {
            return;
        }
        try {
            listener.onComplicationData(getData());
        } catch (RemoteException e) {
            Log.w(WearApplication.TAG, "could not update WearOS complication", e);
        }
    }

    private ComplicationData getData() {
        boolean clockedIn = isClockedIn();
        return new ShortTextComplicationData.Builder(
            new PlainComplicationText.Builder(getText(clockedIn
                ? R.string.clock_out
                : R.string.clock_in)).build(),
            new PlainComplicationText.Builder(getText(clockedIn
                ? R.string.clock_out_description
                : R.string.clock_in_description)).build())
            .setMonochromaticImage(
                new MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_notification)).build())
            .setTapAction(PendingIntent.getBroadcast(this, 0,
                new Intent(clockedIn
                    ? ACTION_CLOCK_OUT
                    : ACTION_CLOCK_IN)
                    .putExtra("text", "from Wear"),
                0))
            .build();
    }
}
