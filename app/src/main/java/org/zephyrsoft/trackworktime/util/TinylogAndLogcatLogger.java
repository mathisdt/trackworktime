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

import android.util.Log;

import org.acra.log.ACRALog;
import org.pmw.tinylog.Logger;

/**
 * ACRA logger for also collecting crash stack traces to the app's log file.
 *
 * @author Mathis Dirksen-Thedens
 */
public class TinylogAndLogcatLogger implements ACRALog {
    @Override
    public int v(String tag, String msg) {
        Logger.trace(msg);
        return Log.v(tag, msg);
    }
    @Override
    public int v(String tag, String msg, Throwable tr) {
        Logger.trace(tr, msg);
        return Log.v(tag, msg, tr);
    }
    @Override
    public int d(String tag, String msg) {
        Logger.debug(msg);
        return Log.d(tag, msg);
    }
    @Override
    public int d(String tag, String msg, Throwable tr) {
        Logger.debug(tr, msg);
        return Log.d(tag, msg, tr);
    }
    @Override
    public int i(String tag, String msg) {
        Logger.info(msg);
        return Log.i(tag, msg);
    }
    @Override
    public int i(String tag, String msg, Throwable tr) {
        Logger.info(tr, msg);
        return Log.i(tag, msg, tr);
    }
    @Override
    public int w(String tag, String msg) {
        Logger.warn(msg);
        return Log.w(tag, msg);
    }
    @Override
    public int w(String tag, String msg, Throwable tr) {
        Logger.warn(tr, msg);
        return Log.w(tag, msg, tr);
    }
    @Override
    public int w(String tag, Throwable tr) {
        Logger.warn(tr);
        return Log.w(tag, tr);
    }
    @Override
    public int e(String tag, String msg) {
        Logger.error(msg);
        return Log.e(tag, msg);
    }
    @Override
    public int e(String tag, String msg, Throwable tr) {
        Logger.error(tr, msg);
        return Log.e(tag, msg, tr);
    }
    @Override
    public String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }
}
