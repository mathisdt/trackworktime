/*
 * This file is part of TrackWorkTime (TWT).
 *
 * TWT is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package org.zephyrsoft.trackworktime.util;

import java.text.MessageFormat;
import android.util.Log;

/**
 * Encapsulation of Android logging facility.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class Logger {
	
	private static final String TAG = "trackworktime";
	
	/**
	 * Log a debug message.
	 */
	public static void debug(String format, Object... arguments) {
		if (Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, composeMessage(format, arguments));
		}
	}
	
	/**
	 * Log an info message.
	 */
	public static void info(String format, Object... arguments) {
		if (Log.isLoggable(TAG, Log.INFO)) {
			Log.i(TAG, composeMessage(format, arguments));
		}
	}
	
	/**
	 * Log a warn message.
	 */
	public static void warn(String format, Object... arguments) {
		if (Log.isLoggable(TAG, Log.WARN)) {
			Log.w(TAG, composeMessage(format, arguments));
		}
	}
	
	/**
	 * Log an error message.
	 */
	public static void error(String format, Object... arguments) {
		if (Log.isLoggable(TAG, Log.ERROR)) {
			Log.e(TAG, composeMessage(format, arguments));
		}
	}
	
	private static String composeMessage(String format, Object[] arguments) {
		return MessageFormat.format(format, arguments);
	}
	
}
