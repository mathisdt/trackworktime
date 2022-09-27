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

import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

import org.pmw.tinylog.Logger;
import org.zephyrsoft.trackworktime.model.Task;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.Updatable;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickSettingsService extends TileService implements Updatable {
    private TimerManager timerManager;

    @Override
    public void onCreate() {
        timerManager = Basics.get(getApplication()).getTimerManager();
        super.onCreate();
    }

    @Override
    public void update() {
        if (getQsTile() == null) {
            return;
        }
        if (timerManager.isTracking()) {
            getQsTile().setState(Tile.STATE_ACTIVE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getQsTile().setStateDescription(getString(R.string.clockedIn));
                getQsTile().setSubtitle(getString(R.string.clockedIn));
            }
        } else {
            getQsTile().setState(Tile.STATE_INACTIVE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getQsTile().setStateDescription(getString(R.string.clockedOut));
                getQsTile().setSubtitle(getString(R.string.clockedOut));
            }
        }
        getQsTile().updateTile();
    }

    @Override
    public void onTileAdded() {
        update();
    }

    @Override
    public void onStartListening() {
        update();
        timerManager.addListener(this);
    }

    @Override
    public void onStopListening() {
        timerManager.removeListener(this);
    }

    @Override
    public void onClick() {
        if (timerManager.isTracking()) {
            Logger.info("TRACKING: clock-out via quick settings");
            timerManager.stopTracking(0, TimerManager.EventOrigin.QUICK_SETTINGS);
        } else {
            Logger.info("TRACKING: clock-in via quick settings");
            Task defaultTask = timerManager.getDefaultTask();
            timerManager.startTracking(0, defaultTask, null, TimerManager.EventOrigin.QUICK_SETTINGS);
        }
        update();
        WorkTimeTrackerActivity instanceOrNull = WorkTimeTrackerActivity.getInstanceOrNull();
        if (instanceOrNull != null) {
            instanceOrNull.refreshView();
        }
        Widget.dispatchUpdateIntent(this);
    }
}