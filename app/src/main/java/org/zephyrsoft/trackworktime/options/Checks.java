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
package org.zephyrsoft.trackworktime.options;

import hirondelle.date4j.DateTime;

import java.util.HashSet;
import java.util.Set;

import android.content.SharedPreferences;

import org.zephyrsoft.trackworktime.model.TimeSum;
import org.zephyrsoft.trackworktime.timer.TimerManager;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;
import org.zephyrsoft.trackworktime.util.Logger;

/**
 * Container of all checks.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class Checks {

	private static final Set<Check> checks = new HashSet<Check>();

	static {
		checks.add(new Check("auto-pause begin has to be before auto-pause end (at least one minute)") {
			@Override
			public boolean usesPreference(Key key) {
				return key == Key.AUTO_PAUSE_BEGIN || key == Key.AUTO_PAUSE_END;
			}

			@Override
			public boolean check(SharedPreferences prefs) {
				String beginString = prefs.getString(Key.AUTO_PAUSE_BEGIN.getName(), null);
				if (beginString == null || beginString.trim().length() == 0) {
					return false;
				}
				beginString = DateTimeUtil.refineTime(beginString);
				DateTime begin = DateTimeUtil.parseTimeForToday(beginString);
				String endString = prefs.getString(Key.AUTO_PAUSE_END.getName(), null);
				if (endString == null || endString.trim().length() == 0) {
					return false;
				}
				endString = DateTimeUtil.refineTime(endString);
				DateTime end = DateTimeUtil.parseTimeForToday(endString);
				try {
					// the actual parsing of begin and end takes place here as this is the first access to fields
					return begin.lt(end);
				} catch (Exception e) {
					return false;
				}
			}
		});

		checks.add(new Check("weekly target working time has to be at least one minute (positive)") {
			@Override
			public boolean usesPreference(Key key) {
				return key == Key.FLEXI_TIME_TARGET;
			}

			@Override
			public boolean check(SharedPreferences prefs) {
				String targetString = prefs.getString(Key.FLEXI_TIME_TARGET.getName(), null);
				if (targetString == null || targetString.trim().length() == 0) {
					return false;
				}
				TimeSum target = TimerManager.parseHoursMinutesString(targetString);

				return target.getAsMinutes() > 0;
			}
		});

		checks.add(new Check("at least one working day has to be checked in the week") {
			@Override
			public boolean usesPreference(Key key) {
				return key == Key.FLEXI_TIME_DAY_MONDAY || key == Key.FLEXI_TIME_DAY_TUESDAY
					|| key == Key.FLEXI_TIME_DAY_WEDNESDAY || key == Key.FLEXI_TIME_DAY_THURSDAY
					|| key == Key.FLEXI_TIME_DAY_FRIDAY || key == Key.FLEXI_TIME_DAY_SATURDAY
					|| key == Key.FLEXI_TIME_DAY_SUNDAY;
			}

			@Override
			public boolean check(SharedPreferences prefs) {
				boolean monday = prefs.getBoolean(Key.FLEXI_TIME_DAY_MONDAY.getName(), false);
				boolean tuesday = prefs.getBoolean(Key.FLEXI_TIME_DAY_TUESDAY.getName(), false);
				boolean wednesday = prefs.getBoolean(Key.FLEXI_TIME_DAY_WEDNESDAY.getName(), false);
				boolean thursday = prefs.getBoolean(Key.FLEXI_TIME_DAY_THURSDAY.getName(), false);
				boolean friday = prefs.getBoolean(Key.FLEXI_TIME_DAY_FRIDAY.getName(), false);
				boolean saturday = prefs.getBoolean(Key.FLEXI_TIME_DAY_SATURDAY.getName(), false);
				boolean sunday = prefs.getBoolean(Key.FLEXI_TIME_DAY_SUNDAY.getName(), false);

				return monday || tuesday || wednesday || thursday || friday || saturday || sunday;
			}
		});

		checks.add(new Check("latitude and longitude have to be provided") {
			@Override
			public boolean usesPreference(Key key) {
				return key == Key.LOCATION_BASED_TRACKING_LATITUDE || key == Key.LOCATION_BASED_TRACKING_LONGITUDE;
			}

			@Override
			public boolean check(SharedPreferences prefs) {
				String latitudeString = prefs.getString(Key.LOCATION_BASED_TRACKING_LATITUDE.getName(), null);
				if (latitudeString == null || latitudeString.trim().length() == 0) {
					return false;
				}
				try {
					Double.parseDouble(latitudeString);
				} catch (NumberFormatException nfe) {
					return false;
				}

				String longitudeString = prefs.getString(Key.LOCATION_BASED_TRACKING_LONGITUDE.getName(), null);
				if (longitudeString == null || longitudeString.trim().length() == 0) {
					return false;
				}
				try {
					Double.parseDouble(longitudeString);
				} catch (NumberFormatException nfe) {
					return false;
				}

				return true;
			}
		});

		checks.add(new Check(
			"time to ignore location before/after events has to be 0 or more, if given at all (not necessary!)") {
			@Override
			public boolean usesPreference(Key key) {
				return key == Key.LOCATION_BASED_TRACKING_IGNORE_BEFORE_EVENTS
					|| key == Key.LOCATION_BASED_TRACKING_IGNORE_AFTER_EVENTS;
			}

			@Override
			public boolean check(SharedPreferences prefs) {
				String ignoreBeforeString = prefs.getString(Key.LOCATION_BASED_TRACKING_IGNORE_BEFORE_EVENTS.getName(),
					null);
				if (ignoreBeforeString == null || ignoreBeforeString.trim().length() == 0) {
					ignoreBeforeString = "0";
				}
				int ignoreBefore = -1;
				try {
					ignoreBefore = Integer.parseInt(ignoreBeforeString);
				} catch (NumberFormatException nfe) {
					ignoreBefore = -1;
				}

				String ignoreAfterString = prefs.getString(Key.LOCATION_BASED_TRACKING_IGNORE_AFTER_EVENTS.getName(),
					null);
				if (ignoreAfterString == null || ignoreAfterString.trim().length() == 0) {
					ignoreAfterString = "0";
				}
				int ignoreAfter = -1;
				try {
					ignoreAfter = Integer.parseInt(ignoreAfterString);
				} catch (NumberFormatException nfe) {
					ignoreAfter = -1;
				}

				return ignoreBefore >= 0 && ignoreAfter >= 0;
			}
		});

		checks.add(new Check("the smallest time unit for flattening has to be a divisor of 60") {
			@Override
			public boolean usesPreference(Key key) {
				return key == Key.SMALLEST_TIME_UNIT;
			}

			@Override
			public boolean check(SharedPreferences prefs) {
				String divisorString = prefs.getString(Key.SMALLEST_TIME_UNIT.getName(), null);
				if (divisorString == null || divisorString.trim().length() == 0) {
					return false;
				}
				int divisor = -1;
				try {
					divisor = Integer.parseInt(divisorString);
				} catch (NumberFormatException nfe) {
					return false;
				}

				return divisor > 0 && divisor <= 60 && (60 % divisor == 0);
			}
		});
	}

	/**
	 * Execute the checks that use the specified preference key.
	 * 
	 * @return {@code true} if all executed checks returned true
	 */
	public static boolean executeFor(Key key, SharedPreferences prefs) {
		for (Check check : checks) {
			if (check.usesPreference(key) && !check.check(prefs)) {
				Logger.info("check \"{0}\" failed for option \"{1}\"", check.getDescription(), key.getName());
				return false;
			}
		}
		return true;
	}

}
