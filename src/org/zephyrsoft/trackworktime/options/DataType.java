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
package org.zephyrsoft.trackworktime.options;

import hirondelle.date4j.DateTime;
import java.util.regex.Pattern;
import android.content.SharedPreferences;
import org.zephyrsoft.trackworktime.util.DateTimeUtil;

/**
 * Data types for the options.
 * 
 * @see Key
 * @author Mathis Dirksen-Thedens
 */
public enum DataType {
	
	/** boolean */
	BOOLEAN {
		@Override
		public boolean validate(String value) {
			// method will not be used as boolean options don't have input fields
			return true;
		}
		
		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			// method will not be used as boolean options don't have input fields
			return true;
		}
	},
	/** integer */
	INTEGER {
		@Override
		public boolean validate(String value) {
			try {
				Integer.parseInt(value);
				return true;
			} catch (Exception nfe) {
				return false;
			}
		}
		
		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			String value = sharedPreferences.getString(key, "0");
			return validate(value);
		}
	},
	/** double */
	DOUBLE {
		@Override
		public boolean validate(String value) {
			try {
				Double.parseDouble(value);
				return true;
			} catch (Exception nfe) {
				return false;
			}
		}
		
		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			String value = sharedPreferences.getString(key, "0.0");
			return validate(value);
		}
	},
	/** time in 24-hour format, e.g. "15.30" or "5.01" */
	TIME {
		@Override
		public boolean validate(String value) {
			String refinedValue = DateTimeUtil.refineTime(value);
			try {
				DateTime dateTime = new DateTime(refinedValue);
				dateTime.getHour();
				return true;
			} catch (Exception nfe) {
				return false;
			}
		}
		
		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			String value = sharedPreferences.getString(key, "00:00");
			return validate(value);
		}
	},
	/** hours and minutes separated by colon (not necessarily under 24 hours), e.g. "27:25" */
	HOUR_MINUTE {
		@Override
		public boolean validate(String value) {
			return value != null && Pattern.matches("\\-?\\d+:\\d\\d", value);
		}
		
		@Override
		public boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key) {
			String value = sharedPreferences.getString(key, "0:00");
			return validate(value);
		}
	};
	
	/**
	 * Validate that a value is correct for this data type.
	 */
	public abstract boolean validate(String value);
	
	/**
	 * Validate that the value found under "key" in "sharedPreferences" is correct for this data type.
	 */
	public abstract boolean validateFromSharedPreferences(SharedPreferences sharedPreferences, String key);
	
	/** Run some internal self-tests. */
	public static void test() {
		boolean valid = HOUR_MINUTE.validate("-1:55");
		valid &= HOUR_MINUTE.validate("1:00");
		valid &= HOUR_MINUTE.validate("1:29");
		valid &= HOUR_MINUTE.validate("37:30");
		if (!valid) {
			throw new AssertionError("HOUR_MINUTE");
		}
	}
}
